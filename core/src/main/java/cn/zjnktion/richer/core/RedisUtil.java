package cn.zjnktion.richer.core;

import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author zjnktion
 */
public class RedisUtil {

    public static CompositeTemplate generateRedisTemplate(Configuration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration");
        }

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxIdle(configuration.getMaxIdle());
        poolConfig.setMaxTotal(configuration.getMaxTotal());
        poolConfig.setTestOnBorrow(configuration.isTestOnBorrow());

        JedisConnectionFactory connectionFactory = new JedisConnectionFactory();
        connectionFactory.setHostName(configuration.getHostName());
        connectionFactory.setPort(configuration.getPort());
        connectionFactory.setPassword(configuration.getPassword());
        connectionFactory.setUsePool(configuration.isUsePool());
        connectionFactory.setPoolConfig(poolConfig);
        connectionFactory.afterPropertiesSet();

        RedisTemplate stringTemplate = new StringRedisTemplate(connectionFactory);
        stringTemplate.afterPropertiesSet();

        RedisTemplate referenceTemplate = new RedisTemplate();
        referenceTemplate.setConnectionFactory(connectionFactory);
        referenceTemplate.setKeySerializer(new StringRedisSerializer());
        referenceTemplate.setHashKeySerializer(new StringRedisSerializer());
        referenceTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        referenceTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        referenceTemplate.afterPropertiesSet();

        CompositeTemplate compositeTemplate = new CompositeTemplate();
        compositeTemplate.setStringTemplate(stringTemplate);
        compositeTemplate.setReferenceTemplate(referenceTemplate);
        return compositeTemplate;
    }

    public static class CompositeTemplate {

        private RedisTemplate referenceTemplate;
        private RedisTemplate stringTemplate;

        public RedisTemplate getReferenceTemplate() {
            return referenceTemplate;
        }

        protected void setReferenceTemplate(RedisTemplate referenceTemplate) {
            this.referenceTemplate = referenceTemplate;
        }

        public RedisTemplate getStringTemplate() {
            return stringTemplate;
        }

        protected void setStringTemplate(RedisTemplate stringTemplate) {
            this.stringTemplate = stringTemplate;
        }
    }

    public static class Configuration {

        private String hostName;
        private int port = 6379;
        private String password = "";
        private boolean usePool = true;
        private int maxIdle = 20;
        private int maxTotal = 200;
        private boolean testOnBorrow = true;

        public String getHostName() {
            return hostName;
        }

        protected void setHostName(String hostName) {
            this.hostName = hostName;
        }

        public int getPort() {
            return port;
        }

        protected void setPort(int port) {
            this.port = port;
        }

        public String getPassword() {
            return password;
        }

        protected void setPassword(String password) {
            this.password = password;
        }

        public boolean isUsePool() {
            return usePool;
        }

        protected void setUsePool(boolean usePool) {
            this.usePool = usePool;
        }

        public int getMaxIdle() {
            return maxIdle;
        }

        protected void setMaxIdle(int maxIdle) {
            this.maxIdle = maxIdle;
        }

        public int getMaxTotal() {
            return maxTotal;
        }

        protected void setMaxTotal(int maxTotal) {
            this.maxTotal = maxTotal;
        }

        public boolean isTestOnBorrow() {
            return testOnBorrow;
        }

        protected void setTestOnBorrow(boolean testOnBorrow) {
            this.testOnBorrow = testOnBorrow;
        }

        public static class Builder {
            private Configuration configuration;

            private Builder() {
                this.configuration = new Configuration();
            }

            public static Builder newBuilder() {
                return new Builder();
            }

            public Configuration build() {
                if (configuration.getHostName() == null || "".equals(configuration.getHostName())) {
                    throw new IllegalArgumentException("hostName");
                }
                return configuration;
            }

            public Builder setHostName(String hostName) {
                configuration.setHostName(hostName);
                return this;
            }

            public Builder setPort(int port) {
                configuration.setPort(port);
                return this;
            }

            public Builder setPassword(String password) {
                configuration.setPassword(password);
                return this;
            }

            public Builder setUsePool(boolean usePool) {
                configuration.setUsePool(usePool);
                return this;
            }

            public Builder setMaxIdle(int maxIdle) {
                configuration.setMaxIdle(maxIdle);
                return this;
            }

            public Builder setMaxTotal(int maxTotal) {
                configuration.setMaxTotal(maxTotal);
                return this;
            }

            public Builder setTestOnBorrow(boolean testOnBorrow) {
                configuration.setTestOnBorrow(testOnBorrow);
                return this;
            }
        }
    }
}
