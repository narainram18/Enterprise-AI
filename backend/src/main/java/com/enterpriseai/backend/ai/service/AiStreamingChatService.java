package com.enterpriseai.backend.ai.service;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enterpriseai.backend.ai.config.AiChatProperties;
import com.enterpriseai.backend.ai.exception.AiGenerationException;
import com.enterpriseai.backend.ai.exception.AiStreamCancelledException;
import com.enterpriseai.backend.ai.model.AiChatRequest;
import com.enterpriseai.backend.ai.provider.AiProvider;
import com.enterpriseai.backend.ai.provider.AiStreamHandler;
import com.enterpriseai.backend.ai.retrieval.model.ChatRetrievalResult;
import com.enterpriseai.backend.ai.retrieval.service.ChatRetrievalService;
import com.enterpriseai.backend.dto.AiStreamErrorResponse;
import com.enterpriseai.backend.dto.ChatMessageResponse;
import com.enterpriseai.backend.dto.CreateMessageRequest;
import com.enterpriseai.backend.entity.ChatMessage;
import com.enterpriseai.backend.mapper.ConversationMapper;
import com.enterpriseai.backend.service.ConversationService;
import com.enterpriseai.backend.workspace.context.WorkspaceContext;
import com.enterpriseai.backend.workspace.context.WorkspaceContextHolder;
import com.enterpriseai.backend.ai.agent.Agent;
import com.enterpriseai.backend.ai.agent.AgentRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.MDC;
import java.util.Map;

@Service
public class AiStreamingChatService {

    private static final Logger log = LoggerFactory.getLogger(AiStreamingChatService.class);

    private static final String USER_MESSAGE_EVENT = "user_message";
    private static final String TOKEN_EVENT = "token";
    private static final String TOOL_PROGRESS_EVENT = "tool_progress";
    private static final String COMPLETE_EVENT = "complete";
    private static final String ERROR_EVENT = "error";

    private final ConversationService conversationService;
    private final AiChatService aiChatService;
    private final ConversationMapper conversationMapper;
    private final AiProvider aiProvider;
    private final AiChatProperties properties;
    private final Executor executor;
    private final ChatRetrievalService chatRetrievalService;
    private final AgentRegistry agentRegistry;
    private final com.enterpriseai.backend.ai.tool.ToolExecutor toolExecutor;
    private final MeterRegistry meterRegistry;

    @Autowired
    public AiStreamingChatService(
            ConversationService conversationService,
            AiChatService aiChatService,
            ConversationMapper conversationMapper,
            AiProvider aiProvider,
            AiChatProperties properties,
            @Qualifier("aiStreamingExecutor") Executor executor,
            ChatRetrievalService chatRetrievalService,
            AgentRegistry agentRegistry,
            com.enterpriseai.backend.ai.tool.ToolExecutor toolExecutor,
            MeterRegistry meterRegistry) {
        this.conversationService = conversationService;
        this.aiChatService = aiChatService;
        this.conversationMapper = conversationMapper;
        this.aiProvider = aiProvider;
        this.properties = properties;
        this.executor = executor;
        this.chatRetrievalService = chatRetrievalService;
        this.agentRegistry = agentRegistry;
        this.toolExecutor = toolExecutor;
        this.meterRegistry = meterRegistry;
    }

    public AiStreamingChatService(
            ConversationService conversationService,
            AiChatService aiChatService,
            ConversationMapper conversationMapper,
            AiProvider aiProvider,
            AiChatProperties properties,
            @Qualifier("aiStreamingExecutor") Executor executor,
            AgentRegistry agentRegistry,
            com.enterpriseai.backend.ai.tool.ToolExecutor toolExecutor,
            MeterRegistry meterRegistry) {
        this(conversationService, aiChatService, conversationMapper, aiProvider,
                properties, executor, null, agentRegistry, toolExecutor, meterRegistry);
    }

    public SseEmitter stream(
            Long conversationId,
            String currentUserEmail,
            CreateMessageRequest request) {

        ChatMessage userMessage = conversationService.saveUserMessage(
                conversationId,
                currentUserEmail,
                request);
        log.info("SSE USER message saved conversationId={} messageId={}", conversationId, userMessage.getId());
        ChatMessageResponse userResponse = conversationMapper.toMessageResponse(userMessage);
        
        Agent agent = agentRegistry.getAgent(userMessage.getConversation().getAgentId());
        
        ChatRetrievalResult retrieval = agent.supportsRag() 
                ? retrieve(request.getContent(), currentUserEmail)
                : ChatRetrievalResult.empty(false);

        return startStream(
                conversationId,
                currentUserEmail,
                buildRequest(agent, conversationId, retrieval),
                userResponse,
                retrieval);
    }

    public SseEmitter regenerate(
            Long conversationId,
            Long messageId,
            String currentUserEmail) {
        ChatMessage userMessage = conversationService.getUserMessage(conversationId, messageId, currentUserEmail);
        Agent agent = agentRegistry.getAgent(userMessage.getConversation().getAgentId());
        
        ChatRetrievalResult retrieval = agent.supportsRag()
                ? retrieve(userMessage.getContent(), currentUserEmail)
                : ChatRetrievalResult.empty(false);
                
        return startStream(
                conversationId,
                currentUserEmail,
                buildRequest(agent, conversationId, retrieval),
                null,
                retrieval);
    }

    SseEmitter streamWithContext(
            Long conversationId,
            String currentUserEmail,
            CreateMessageRequest request,
            String retrievalContext) {
        ChatMessage userMessage = conversationService.saveUserMessage(
                conversationId,
                currentUserEmail,
                request);
        ChatMessageResponse userResponse = conversationMapper.toMessageResponse(userMessage);
        Agent agent = agentRegistry.getAgent(userMessage.getConversation().getAgentId());
        
        ChatRetrievalResult retrieval = ChatRetrievalResult.empty(false);
        return startStream(
                conversationId,
                currentUserEmail,
                aiChatService.buildContextRequest(agent, conversationId, retrievalContext),
                userResponse,
                retrieval);
    }

    private SseEmitter startStream(
            Long conversationId,
            String currentUserEmail,
            AiChatRequest aiRequest,
            ChatMessageResponse userResponse,
            ChatRetrievalResult retrieval) {

        SseEmitter emitter = new SseEmitter(properties.streamTimeout());
        AtomicBoolean cancelled = new AtomicBoolean(false);
        AtomicReference<Thread> workerThread = new AtomicReference<>();
        registerLifecycleCallbacks(emitter, cancelled, workerThread);

        if (userResponse != null && !sendEvent(emitter, USER_MESSAGE_EVENT, userResponse, cancelled)) {
            log.warn("SSE user_message event could not be sent conversationId={}", conversationId);
            return emitter;
        }

        WorkspaceContext context = WorkspaceContextHolder.getContext();
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();

        executor.execute(() -> {
            if (mdcContext != null) {
                MDC.setContextMap(mdcContext);
            }
            if (context != null) {
                WorkspaceContextHolder.setContext(context);
            }
            try {
                generateAndStream(
                        conversationId,
                        currentUserEmail,
                        aiRequest,
                        emitter,
                        cancelled,
                        workerThread,
                        retrieval);
            } finally {
                if (context != null) {
                    WorkspaceContextHolder.clearContext();
                }
                MDC.clear();
            }
        });

        return emitter;
    }

    private void generateAndStream(
            Long conversationId,
            String currentUserEmail,
            AiChatRequest request,
            SseEmitter emitter,
            AtomicBoolean cancelled,
            AtomicReference<Thread> workerThread,
            ChatRetrievalResult retrieval) {

        workerThread.set(Thread.currentThread());
        log.info("SSE Ollama streaming started conversationId={} thread={}", conversationId, Thread.currentThread().getName());

        Timer.Sample streamSample = Timer.start(meterRegistry);
        StringBuilder assistantContent = new StringBuilder();

        try {
            boolean loop = true;
            int maxToolCalls = 5;
            int toolCalls = 0;
            AiChatRequest currentRequest = request;

            while (loop && toolCalls < maxToolCalls) {
                loop = false; // By default don't loop unless a tool is called

                ToolStreamInterceptor interceptor = new ToolStreamInterceptor(new AiStreamHandler() {
                    @Override
                    public void onToken(String token) {
                        if (cancelled.get()) {
                            throw new AiStreamCancelledException();
                        }

                        assistantContent.append(token);
                        if (!sendEvent(emitter, TOKEN_EVENT, token, cancelled)) {
                            throw new AiStreamCancelledException();
                        }
                    }

                    @Override
                    public boolean isCancelled() {
                        return cancelled.get();
                    }
                });

                Timer.Sample llmSample = Timer.start(meterRegistry);
                boolean completed = aiProvider.stream(currentRequest, interceptor);
                llmSample.stop(meterRegistry.timer("chat.llm.latency"));
                interceptor.flushRemaining();

                if (!completed || cancelled.get()) {
                    log.info("SSE generation ended without persistence conversationId={} completed={} cancelled={}", conversationId, completed, cancelled.get());
                    emitter.complete();
                    return;
                }

                if (interceptor.hasToolCall()) {
                    toolCalls++;
                    log.info("Tool call intercepted: {}", interceptor.getToolName());
                    sendEvent(emitter, TOOL_PROGRESS_EVENT, interceptor.getToolName(), cancelled);

                    // Execute tool
                    com.enterpriseai.backend.ai.tool.ToolContext toolContext = new com.enterpriseai.backend.ai.tool.ToolContext(
                            WorkspaceContextHolder.getContext().getWorkspaceId(), currentUserEmail, conversationId);
                    
                    Timer.Sample toolSample = Timer.start(meterRegistry);
                    com.enterpriseai.backend.ai.tool.ToolResult result = toolExecutor.execute(
                            interceptor.getToolName(), interceptor.getToolParameters(), toolContext);
                    toolSample.stop(meterRegistry.timer("chat.tool.latency", "tool", interceptor.getToolName()));

                    // Add assistant tool_call message and tool result to currentRequest
                    java.util.List<com.enterpriseai.backend.ai.model.AiMessage> newMessages = new java.util.ArrayList<>(currentRequest.messages());
                    newMessages.add(new com.enterpriseai.backend.ai.model.AiMessage(
                            com.enterpriseai.backend.ai.model.AiMessageRole.ASSISTANT, interceptor.getRawToolCall()));
                    
                    String toolResultMessage = "<tool_result>\n" +
                            "  <success>" + result.success() + "</success>\n" +
                            "  <result>" + result.content() + "</result>\n" +
                            "</tool_result>\n";
                    newMessages.add(new com.enterpriseai.backend.ai.model.AiMessage(
                            com.enterpriseai.backend.ai.model.AiMessageRole.USER, toolResultMessage));

                    currentRequest = new AiChatRequest(newMessages, currentRequest.temperature(), currentRequest.topP(), currentRequest.model());
                    loop = true;
                }
            }

            if (assistantContent.isEmpty()) {
                throw new AiGenerationException(
                        "AI provider returned an empty response");
            }

            if (cancelled.get()) {
                emitter.complete();
                return;
            }

            ChatMessage assistantMessage = conversationService.saveAssistantMessage(
                    conversationId,
                    currentUserEmail,
                    assistantContent.toString());
            log.info("SSE ASSISTANT message saved conversationId={} messageId={} length={}", conversationId, assistantMessage.getId(), assistantContent.length());
            ChatMessageResponse assistantResponse = conversationMapper.toMessageResponse(assistantMessage);
            if (retrieval.statistics().retrievalAttempted()) {
                assistantResponse.setCitations(retrieval.citations());
                assistantResponse.setRetrievalStatistics(retrieval.statistics());
            }

            if (sendEvent(emitter, COMPLETE_EVENT, assistantResponse, cancelled)) {
                log.info("SSE complete event emitted conversationId={}", conversationId);
                streamSample.stop(meterRegistry.timer("chat.sse.duration"));
                emitter.complete();
                log.info("SSE emitter completed conversationId={}", conversationId);
            }
        } catch (AiStreamCancelledException ex) {
            log.info("SSE generation cancelled conversationId={}", conversationId);
            streamSample.stop(meterRegistry.timer("chat.sse.duration", "status", "cancelled"));
            emitter.complete();
        } catch (AiGenerationException ex) {
            log.error("SSE generation failed conversationId={} cause={}", conversationId, ex.getMessage(), ex);
            streamSample.stop(meterRegistry.timer("chat.sse.duration", "status", "failed"));
            sendErrorAndComplete(emitter, cancelled, ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("SSE generation failed unexpectedly conversationId={}", conversationId, ex);
            streamSample.stop(meterRegistry.timer("chat.sse.duration", "status", "error"));
            sendErrorAndComplete(emitter, cancelled, "AI streaming failed");
        }
    }

    private ChatRetrievalResult retrieve(String query, String currentUserEmail) {
        return chatRetrievalService == null
                ? ChatRetrievalResult.empty(false)
                : chatRetrievalService.retrieve(query, currentUserEmail);
    }

    private AiChatRequest buildRequest(Agent agent, Long conversationId, ChatRetrievalResult retrieval) {
        return chatRetrievalService == null
                ? aiChatService.buildContextRequest(conversationId)
                : aiChatService.buildContextRequest(agent, conversationId, retrieval.context());
    }

    private void registerLifecycleCallbacks(
            SseEmitter emitter,
            AtomicBoolean cancelled,
            AtomicReference<Thread> workerThread) {
        Runnable cancel = () -> {
            cancelled.set(true);
            Thread worker = workerThread.get();
            if (worker != null && worker != Thread.currentThread()) {
                worker.interrupt();
            }
        };
        emitter.onCompletion(cancel);
        emitter.onTimeout(cancel);
        emitter.onError(error -> cancel.run());
    }

    private boolean sendEvent(
            SseEmitter emitter,
            String eventName,
            Object data,
            AtomicBoolean cancelled) {
        if (cancelled.get()) {
            return false;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
            return true;
        } catch (IOException | IllegalStateException ex) {
            log.error("SSE event send failed event={}", eventName, ex);
            cancelled.set(true);
            emitter.complete();
            return false;
        }
    }

    private void sendErrorAndComplete(
            SseEmitter emitter,
            AtomicBoolean cancelled,
            String message) {
        if (!cancelled.get()) {
            sendEvent(
                    emitter,
                    ERROR_EVENT,
                    new AiStreamErrorResponse(message),
                    cancelled);
        }
        emitter.complete();
    }
}
