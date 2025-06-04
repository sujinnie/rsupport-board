package com.rsupport.board.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


/**
 * RedisTemplate 설정
 * (직접 redis 키벨류 조작하고 싶을때(ex. INCR, GET, DELETE 등) 사용)
 *
 * 지금은 조회수 증가를 카운트하기위한 용도 (key: String, value: Long 타입을 다룰 수 있도록 설정)
 */
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Long> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Long> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer()); // key 는 스트링
        redisTemplate.setValueSerializer(new GenericToStringSerializer<>(Long.class)); // value 는 Long
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}

