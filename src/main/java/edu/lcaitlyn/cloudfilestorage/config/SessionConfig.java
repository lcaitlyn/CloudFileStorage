package edu.lcaitlyn.cloudfilestorage.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import edu.lcaitlyn.cloudfilestorage.models.AuthUserDetails;
import edu.lcaitlyn.cloudfilestorage.models.AuthUserDetailsMixin;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@EnableRedisHttpSession
public class SessionConfig implements BeanClassLoaderAware {
    private ClassLoader classLoader;

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        objectMapper.registerModules(SecurityJackson2Modules.getModules(this.classLoader));
        objectMapper.registerModule(new SimpleModule().addAbstractTypeMapping(UserDetails.class, AuthUserDetails.class));
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
}
