package cn.zjnktion.richer.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author zjnktion
 */
public class JDBC {

    private static final Logger LOGGER = LoggerFactory.getLogger(JDBC.class);

    private static JdbcTemplate jdbcTemplate;

    static void initTemplate(JdbcTemplate jdbcTemplate) {
        if (JDBC.jdbcTemplate != null) {
            throw new IllegalStateException("Template had been init before.");
        }
        JDBC.jdbcTemplate = jdbcTemplate;
    }

    public static JdbcTemplate getTemplate() {
        return jdbcTemplate;
    }
}
