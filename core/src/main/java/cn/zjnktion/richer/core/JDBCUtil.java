package cn.zjnktion.richer.core;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;

/**
 * @author zjnktion
 */
public class JDBCUtil {

    public static JdbcTemplate generateMysqlTemplate(DBConfiguration configuration) throws SQLException {
        if (configuration == null) {
            throw new IllegalArgumentException("dbConfiguration");
        }

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName(configuration.getDriverClassName());
        dataSource.setUrl(configuration.getJdbcUrl());
        dataSource.setUsername(configuration.getUsername());
        dataSource.setPassword(configuration.getPassword());
        dataSource.setMaxActive(configuration.getMaxActive());
        dataSource.setMaxIdle(configuration.getMaxIdle());
        dataSource.setMinIdle(configuration.getMinIdle());
        dataSource.setValidationQuery(configuration.getValidationQuery());
        dataSource.setTestOnBorrow(configuration.isTestOnBorrow());
        dataSource.init();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return jdbcTemplate;
    }

    public static void destoryMysqlTemplate(JdbcTemplate jdbcTemplate) {
        if (jdbcTemplate == null) {
            return;
        }

        DruidDataSource dataSource = (DruidDataSource) jdbcTemplate.getDataSource();
        if (dataSource != null) {
            dataSource.close();
        }

        jdbcTemplate = null;
    }

    public static class DBConfiguration {
        private String driverClassName;
        private String jdbcUrl;
        private String username;
        private String password;
        private int maxActive = 20;
        private int maxIdle = 20;
        private int minIdle = 1;
        private String validationQuery = "select 1";
        private boolean testOnBorrow = true;

        public String getDriverClassName() {
            return driverClassName;
        }

        protected void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        protected void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        public String getUsername() {
            return username;
        }

        protected void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        protected void setPassword(String password) {
            this.password = password;
        }

        public int getMaxActive() {
            return maxActive;
        }

        protected void setMaxActive(int maxActive) {
            this.maxActive = maxActive;
        }

        public int getMaxIdle() {
            return maxIdle;
        }

        protected void setMaxIdle(int maxIdle) {
            this.maxIdle = maxIdle;
        }

        public int getMinIdle() {
            return minIdle;
        }

        protected void setMinIdle(int minIdle) {
            this.minIdle = minIdle;
        }

        public String getValidationQuery() {
            return validationQuery;
        }

        protected void setValidationQuery(String validationQuery) {
            this.validationQuery = validationQuery;
        }

        public boolean isTestOnBorrow() {
            return testOnBorrow;
        }

        protected void setTestOnBorrow(boolean testOnBorrow) {
            this.testOnBorrow = testOnBorrow;
        }

        public static class Builder {

            private DBConfiguration configuration;

            private Builder() {
                this.configuration = new DBConfiguration();
            }

            public static Builder newBuilder() {
                return new Builder();
            }

            public DBConfiguration build() {
                if (configuration.getDriverClassName() == null) {
                    throw new IllegalArgumentException("driverClassName");
                }
                if (configuration.getJdbcUrl() == null) {
                    throw new IllegalArgumentException("jdbcUrl");
                }
                if (configuration.getUsername() == null) {
                    throw new IllegalArgumentException("username");
                }
                if (configuration.getPassword() == null) {
                    throw new IllegalArgumentException("password");
                }
                return configuration;
            }

            public Builder setDriverClassName(String driverClassName) {
                configuration.setDriverClassName(driverClassName);
                return this;
            }

            public Builder setJdbcUrl(String jdbcUrl) {
                configuration.setJdbcUrl(jdbcUrl);
                return this;
            }

            public Builder setUsername(String username) {
                configuration.setUsername(username);
                return this;
            }

            public Builder setPassword(String password) {
                configuration.setPassword(password);
                return this;
            }

            public Builder setMaxActive(int maxActive) {
                configuration.setMaxActive(maxActive);
                return this;
            }

            public Builder setMaxIdle(int maxIdle) {
                configuration.setMaxIdle(maxIdle);
                return this;
            }

            public Builder setMinIdle(int minIdle) {
                configuration.setMinIdle(minIdle);
                return this;
            }

            public Builder setValidationQuery(String validationQuery) {
                configuration.setValidationQuery(validationQuery);
                return this;
            }

            public Builder setTestOnBorrow(boolean testOnBorrow) {
                configuration.setTestOnBorrow(testOnBorrow);
                return this;
            }
        }

    }
}
