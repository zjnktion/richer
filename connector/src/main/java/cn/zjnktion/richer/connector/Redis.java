package cn.zjnktion.richer.connector;

import cn.zjnktion.richer.core.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author zjnktion
 */
public class Redis {

    private static final Logger LOGGER = LoggerFactory.getLogger(Redis.class);

    private static RedisUtil.CompositeTemplate compositeTemplate;

    static void initTemplate(RedisUtil.CompositeTemplate compositeTemplate) {
        if (Redis.compositeTemplate != null) {
            throw new IllegalStateException("Template had been init before");
        }
        Redis.compositeTemplate = compositeTemplate;
    }

    public static RedisTemplate getStringTemplate() {
        return Redis.compositeTemplate.getStringTemplate();
    }

    public static RedisTemplate getReferenceTemplate() {
        return Redis.compositeTemplate.getReferenceTemplate();
    }
}
