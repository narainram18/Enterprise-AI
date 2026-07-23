package com.enterpriseai.backend.config;

import org.mapstruct.factory.Mappers;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.enterpriseai.backend.mapper.ConversationMapper;
import com.enterpriseai.backend.mapper.UserMapper;

@Configuration
public class MapperConfiguration {

    @Bean
    @ConditionalOnMissingBean(ConversationMapper.class)
    ConversationMapper conversationMapper() {
        return Mappers.getMapper(ConversationMapper.class);
    }

    @Bean
    @ConditionalOnMissingBean(UserMapper.class)
    UserMapper userMapper() {
        return Mappers.getMapper(UserMapper.class);
    }
}
