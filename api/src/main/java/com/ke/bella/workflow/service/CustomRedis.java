package com.ke.bella.workflow.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomRedis implements AutoCloseable {

    private JedisPooled jedisPooled;

    public JedisPooled conn() {
        return jedisPooled;
    }

    public static CustomRedis using(String host, int port, String user, String password, int database) {
        JedisPooled jedisPooled;
        HostAndPort hostAndPort = new HostAndPort(host, port);
        DefaultJedisClientConfig.Builder clientConfigBuilder = DefaultJedisClientConfig.builder().database(database);
        if(StringUtils.isNotEmpty(user)) {
            clientConfigBuilder.user(user);
        }
        if (StringUtils.isNotEmpty(password)) {
            clientConfigBuilder.password(password);
        }
        jedisPooled = new JedisPooled(hostAndPort, clientConfigBuilder.build());

        return CustomRedis.builder()
                .jedisPooled(jedisPooled)
                .build();
    }

    public void close() {
        if (jedisPooled != null) {
            jedisPooled.close();
        }
    }
}
