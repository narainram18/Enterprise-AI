package com.enterpriseai.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.enterpriseai.backend.controller.DocumentController;
import com.enterpriseai.backend.mapper.ConversationMapper;

@SpringBootTest
class BackendApplicationTests {

	@Autowired
	private ConversationMapper conversationMapper;

	@Autowired
	private DocumentController documentController;

	@Test
	void contextLoads() {
		org.junit.jupiter.api.Assertions.assertNotNull(conversationMapper);
		org.junit.jupiter.api.Assertions.assertNotNull(documentController);
	}

}
