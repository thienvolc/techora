package com.techora.inventory.infra.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class InventoryPostgresIndexInitializer implements ApplicationRunner {
    private static final String POSTGRESQL = "PostgreSQL";
    private static final String CREATE_RESERVED_RESERVATION_INDEX = """
            create index if not exists idx_inventory_reservations_reserved_product
            on inventory_reservations (product_id)
            where status = 'RESERVED'
            """;

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (isPostgres()) {
            jdbcTemplate.execute(CREATE_RESERVED_RESERVATION_INDEX);
        }
    }

    private boolean isPostgres() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return POSTGRESQL.equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
        }
    }
}
