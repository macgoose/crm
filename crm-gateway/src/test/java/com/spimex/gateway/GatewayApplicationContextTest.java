package com.spimex.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:gateway-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.liquibase.contexts=dev"
})
class GatewayApplicationContextTest {

    @Test
    void startsWithCleanDatabaseAndDevRoutes() {
    }
}
