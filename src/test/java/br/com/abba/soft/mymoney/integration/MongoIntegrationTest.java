package br.com.abba.soft.mymoney.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
public abstract class MongoIntegrationTest {

    protected static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    @BeforeAll
    static void start() {
        // Skip if Docker is not available to avoid failures in environments without Docker
        org.junit.jupiter.api.Assumptions.assumeTrue(
                org.testcontainers.DockerClientFactory.instance().isDockerAvailable(),
                "Docker not available - skipping Testcontainers integration tests"
        );
        MONGO.start();
    }

    @AfterAll
    static void stop() {
        if (MONGO.isRunning()) {
            MONGO.stop();
        }
    }

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> MONGO.getConnectionString());
    }
}
