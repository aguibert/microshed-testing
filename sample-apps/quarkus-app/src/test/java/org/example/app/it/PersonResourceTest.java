package org.example.app.it;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.example.app.Person;
import org.junit.jupiter.api.Test;
import org.microshed.testing.SharedContainerConfig;
import org.microshed.testing.jupiter.MicroShedTest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@MicroShedTest
@QuarkusTest
@SharedContainerConfig(QuarkusTestEnvironment.class)
public class PersonResourceTest {
  
    @Test
    public void testCreatePerson() {
        given()
          .queryParam("first", "Bob")
          .queryParam("last", "Bobington")
          .when()
            .post("/people")
          .then()
             .statusCode(200);
    }
    
    @Test
    public void testGetPerson() {
        long calID = given()
          .queryParam("first", "Cal")
          .queryParam("last", "Ifornia")
          .when()
            .post("/people")
          .then()
             .statusCode(200)
             .contentType(ContentType.JSON)
             .extract()
             .as(long.class);
        
        Person cal = given()
            .pathParam("id", calID)
            .when()
            .get("/people/{id}")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .extract()
            .as(Person.class);
        assertEquals("Cal", cal.firstName);
        assertEquals("Ifornia", cal.lastName);
        assertEquals(calID, cal.id);
    }
    
}