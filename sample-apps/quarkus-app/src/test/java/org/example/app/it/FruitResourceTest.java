package org.example.app.it;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.example.app.Fruit;
import org.example.app.FruitResource;
import org.junit.jupiter.api.Test;
import org.microshed.testing.SharedContainerConfig;
import org.microshed.testing.jaxrs.RESTClient;
import org.microshed.testing.jupiter.MicroShedTest;

import io.quarkus.test.junit.QuarkusTest;

@MicroShedTest
@QuarkusTest
@SharedContainerConfig(QuarkusTestEnvironment.class)
public class FruitResourceTest {
    
    @RESTClient
    public static FruitResource fruitResource;
  
    @Test
    public void testAddFruit() {
        Fruit apple = new Fruit("Apple", "red");
        List<Fruit> allFruit = fruitResource.add(apple);
        assertTrue(allFruit.size() >= 1, "Should be at least 2 fruit but there were only " + allFruit.size());
        assertThat(allFruit, hasItem(apple));
    }
    
    @Test
    public void testListFruit() {
        Fruit banana = new Fruit("Banana", "yellow");
        Fruit grape = new Fruit("Grape", "purple");
        Fruit orange = new Fruit("Orange", "orange");
        fruitResource.add(banana);
        fruitResource.add(grape);
        
        List<Fruit> allFruit = fruitResource.list();
        assertTrue(allFruit.size() >= 2, "Should be at least 2 fruit but there were only " + allFruit.size());
        assertThat(allFruit, hasItem(banana));
        assertThat(allFruit, hasItem(grape));
        assertThat(allFruit, not(hasItem(orange)));
    }
    
}