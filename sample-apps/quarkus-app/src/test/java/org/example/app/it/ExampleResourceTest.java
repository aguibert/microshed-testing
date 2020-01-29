package org.example.app.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;
import org.microshed.testing.SharedContainerConfig;
import org.microshed.testing.jupiter.MicroShedTest;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@MicroShedTest
@SharedContainerConfig(QuarkusTestEnvironment.class)
public class ExampleResourceTest {
  
    @Test
    public void testHelloEndpoint() {
        given()
          .when().get("/hello")
          .then()
             .statusCode(200)
             .body(is("hello"));
    }
    
}