package org.example.app.it;

import org.microshed.testing.SharedContainerConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

public class QuarkusTestEnvironment implements SharedContainerConfiguration {
    
    // No need for an ApplicationContainer because we let the 
    // quarkus-maven-plugin handle starting Quarkus
    
    @Container
    public static PostgreSQLContainer<?> db = new PostgreSQLContainer<>();
    
    @Container
    public static GenericContainer<?> mongo = new GenericContainer<>("mongo:3.4")
        .withExposedPorts(27017);
    
    @Container
    public static KafkaContainer kafka = new KafkaContainer()
        .withNetwork(Network.SHARED);

}
