package com.fileload.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class EmailChangeRequestSchemaMigration implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(EmailChangeRequestSchemaMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public EmailChangeRequestSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    ALTER TABLE email_change_requests
                    ADD COLUMN pending_user_id BIGINT
                    GENERATED ALWAYS AS (CASE WHEN status = 'pending' THEN user_id ELSE NULL END) STORED
                    """);
        } catch (DataAccessException ex) {
            logger.debug("Email change pending_user_id column already exists or could not be added: {}", ex.getMessage());
        }

        try {
            jdbcTemplate.execute("CREATE UNIQUE INDEX uk_email_change_pending_user ON email_change_requests (pending_user_id)");
        } catch (DataAccessException ex) {
            logger.debug("Email change pending-user index already exists or could not be added: {}", ex.getMessage());
        }
    }
}
