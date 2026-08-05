package com.enterpriseai.backend.controller;

import java.util.Collection;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.enterpriseai.backend.ai.agent.Agent;
import com.enterpriseai.backend.ai.agent.AgentRegistry;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentRegistry agentRegistry;

    public AgentController(AgentRegistry agentRegistry) {
        this.agentRegistry = agentRegistry;
    }

    @GetMapping
    public Collection<Agent> getAgents() {
        return agentRegistry.getAllAgents();
    }
}
