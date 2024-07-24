package de.upb.sse.jess.stubbing;

import de.upb.sse.jess.Jess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ObjectCreationTests {
    private static Jess jess;

    @BeforeEach
    void setupTests() {
        jess = new Jess();
    }

    @Test
    @DisplayName("Simple object creation without arguments")
    void objectCreation1() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/objectcreation/ObjectCreation1.java"));
    }

    @Test
    @DisplayName("Simple object creation with arguments")
    void objectCreation2() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/objectcreation/ObjectCreation2.java"));
    }

    @Test
    @DisplayName("Array object creation")
    void objectCreation3() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/objectcreation/ObjectCreation3.java"));
    }
}
