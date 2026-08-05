package com.enterpriseai.backend.ai.agent;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class AgentRegistry {

    private final Map<String, Agent> agents = new ConcurrentHashMap<>();

    public AgentRegistry() {
        registerBuiltInAgents();
    }

    private void registerBuiltInAgents() {
        agents.put("general-assistant", new Agent(
                "general-assistant",
                "General Assistant",
                "Default assistant that can answer general questions.",
                "Brain",
                "#3b82f6", // blue-500
                "You are an enterprise AI assistant. "
                        + "Never answer using unsupported facts. "
                        + "Retrieved knowledge has higher priority than conversation history. "
                        + "If retrieved knowledge conflicts with previous assistant replies, ignore the previous assistant replies. "
                        + "Answer from retrieved knowledge whenever possible. If the answer is missing, explicitly say: \"I couldn't find this information in the uploaded documents.\" "
                        + "Never fabricate document contents. Never mix retrieved facts with general knowledge unless explicitly asked.",
                0.7,
                0.9,
                null,
                true,
                true,
                java.util.List.of("workspace_info")
        ));

        agents.put("research-assistant", new Agent(
                "research-assistant",
                "Research Assistant",
                "Heavily relies on retrieved documents to provide structured, cited answers.",
                "BookOpen",
                "#8b5cf6", // violet-500
                "You are an expert research assistant. You MUST rely heavily on the retrieved knowledge provided. "
                        + "Analyze the provided documents deeply and extract facts, data points, and quotes. "
                        + "Your answers must be highly structured, using markdown headings, bullet points, and citations. "
                        + "If you make a claim, ensure it is backed by the retrieved context. Do NOT answer if the context is missing.",
                0.2, // lower temp for more factual answers
                0.8,
                null,
                true,
                true,
                java.util.List.of("search_documents", "retrieve_document")
        ));

        agents.put("coding-assistant", new Agent(
                "coding-assistant",
                "Coding Assistant",
                "Optimized for software development, code generation, and debugging.",
                "Code",
                "#10b981", // emerald-500
                "You are an expert software engineer and technical lead. "
                        + "You specialize in Java, Spring Boot, React, and SQL. "
                        + "When writing code, always use markdown code blocks with the correct language tag. "
                        + "Ensure code is production-ready, clean, and follows best practices. "
                        + "Explain your code briefly. If the user provides error messages or stack traces, analyze them step-by-step.",
                0.4,
                0.9,
                null,
                true,
                true,
                java.util.List.of("conversation_summary")
        ));

        agents.put("sql-assistant", new Agent(
                "sql-assistant",
                "SQL Assistant",
                "Specializes in writing complex SQL queries, database design, and optimization.",
                "Database",
                "#f59e0b", // amber-500
                "You are a Senior Database Administrator and Data Engineer. "
                        + "Your primary role is to write, optimize, and explain SQL queries. "
                        + "When provided with a schema, write the most efficient query to solve the problem. "
                        + "Always use standard SQL or PostgreSQL syntax unless specified otherwise. "
                        + "Use comments in your SQL code to explain complex joins or window functions.",
                0.1,
                0.7,
                null,
                false, // maybe doesn't need RAG as much, but let's say false or true based on context
                true,
                java.util.List.of()
        ));

        agents.put("project-planner", new Agent(
                "project-planner",
                "Project Planner",
                "Helps break down goals into roadmaps, milestones, and actionable tasks.",
                "ListTodo",
                "#ec4899", // pink-500
                "You are an Agile Project Manager and Product Owner. "
                        + "Your goal is to help users break down complex projects into actionable roadmaps. "
                        + "Generate clear milestones, sprint tasks, and timelines. "
                        + "Always structure your output as a hierarchical list or checklist. "
                        + "Focus on MVP (Minimum Viable Product) first, then iterate.",
                0.6,
                0.9,
                null,
                true,
                true,
                java.util.List.of("list_documents")
        ));
    }

    public Agent getAgent(String id) {
        return agents.getOrDefault(id, agents.get("general-assistant"));
    }

    public Collection<Agent> getAllAgents() {
        return agents.values();
    }
}
