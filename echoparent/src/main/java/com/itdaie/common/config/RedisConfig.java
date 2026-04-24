package com.itdaie.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类
 * 配置RedisTemplate用于Redis操作
 *
 * @author itdaie
 */
@Configuration
public class RedisConfig {

    /**
     * 配置RedisTemplate
     * 使用String序列化器处理key，JSON序列化器处理value
     *
     * @param connectionFactory Redis连接工厂
     * @return 配置好的RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 配置ObjectMapper以支持类型信息保留
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // 使用StringRedisSerializer序列化key
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        // 使用GenericJackson2JsonRedisSerializer序列化value
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 设置key和hash key的序列化方式
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // 设置value和hash value的序列化方式
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
