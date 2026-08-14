package com.enterpriseai.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import com.enterpriseai.backend.workspace.entity.Workspace;

@Getter
@Setter
@Entity
@Table(name = "agent_tasks")
public class AgentTask extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false)
    private String agentId;

    @Column(nullable = false)
    private String createdBy; // email of the user who created it

    @Column(columnDefinition = "TEXT", nullable = false)
    private String prompt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentTaskStatus status = AgentTaskStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String result;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    public AgentTask() {
    }
}
