package com.enterpriseai.backend.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CacheLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(CacheLoggingAspect.class);
    private final CacheManager cacheManager;

    public CacheLoggingAspect(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Around("execution(* com.enterpriseai.backend.ai.retrieval.service.ChatRetrievalService.retrieve(..))")
    public Object logCacheHit(ProceedingJoinPoint joinPoint) throws Throwable {
        Cache cache = cacheManager.getCache("retrievalResults");
        if (cache != null) {
            Object key = SimpleKeyGenerator.generateKey(joinPoint.getArgs());
            if (cache.get(key) != null) {
                log.info("CACHE HIT - Retrieved from cache");
            }
        }
        return joinPoint.proceed();
    }
}
