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
                java.util.List.of("list_documents", "workspace_info"),
                "General Q&A, Writing, Brainstorming",
                "Ask me anything about your workspace, or let's brainstorm ideas together.",
                java.util.List.of("What documents are in my workspace?", "Help me brainstorm ideas")
        ));

        agents.put("research-assistant", new Agent(
                "research-assistant",
                "Research Assistant",
                "Uses web search, browser reading, and retrieved documents to provide structured answers.",
                "BookOpen",
                "#8b5cf6", // violet-500
                "You are an expert research assistant. "
                        + "Analyze the provided documents deeply and extract facts, data points, and quotes. "
                        + "If the documents do not contain the answer, you MUST use `web_search`. "
                        + "You can use `browser_tool` to open a specific URL from search results to read the full page content. "
                        + "Limit your tool usage to a maximum of 3 `web_search` calls and 3 `browser_tool` calls per task. "
                        + "Your answers must be highly structured, using markdown headings, bullet points, and citations (including URLs if from the web). "
                        + "If you make a claim, ensure it is backed by the retrieved context or web search results.",
                0.2, // lower temp for more factual answers
                0.8,
                null,
                true,
                true,
                java.util.List.of("search_documents", "retrieve_document", "summarize_document", "web_search", "browser_tool"),
                "Research, Web Search, Browser, PDFs, Knowledge Base",
                "Upload documents or ask me to search the web, and I'll answer using citations.",
                java.util.List.of("Search the web for latest AI news", "Summarize my uploaded documents", "Compare two documents")
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
                java.util.List.of("code_analysis", "explain_code", "generate_code"),
                "Programming, Debugging, Code Review",
                "Ask me to build APIs, debug code, review architecture, or explain algorithms.",
                java.util.List.of("Build Spring Boot API", "Review this code", "Optimize my React component")
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
                false, 
                true,
                java.util.List.of("generate_sql", "explain_sql"),
                "Database Queries, Schema Design, Optimization",
                "I can write complex SQL queries, design schemas, and optimize database performance.",
                java.util.List.of("Generate PostgreSQL query", "Optimize this SQL", "Design database schema")
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
                false,
                true,
                java.util.List.of("task_planner", "timeline_generator", "risk_analysis"),
                "Roadmaps, Milestones, Task Breakdown",
                "I'll help you create roadmaps, milestones, and implementation plans.",
                java.util.List.of("Plan my project", "Generate milestones", "Estimate timeline")
        ));
    }

    public Agent getAgent(String id) {
        return agents.getOrDefault(id, agents.get("general-assistant"));
    }

    public Collection<Agent> getAllAgents() {
        return agents.values();
    }
}
