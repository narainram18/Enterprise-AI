package com.enterpriseai.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@RestController
public class TempController {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/api/temp-update")
    public String update() {
        jdbcTemplate.update("UPDATE knowledge_documents SET processing_status = 'READY' WHERE id = 14");
        return "Updated!";
    }
}
