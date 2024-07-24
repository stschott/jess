package de.upb.sse.jess.stubbing;

import de.upb.sse.jess.Jess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GenericsTests {
    private static Jess jess;

    @BeforeEach
    void setupTests() {
        jess = new Jess();
    }

    @Test
    @DisplayName("Generic object creation")
    void generics1() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/generics/Generics1.java"));
    }

    @Test
    @DisplayName("Generic wildcard object creation")
    void generics2() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/generics/Generics2.java"));
    }

    @Test
    @DisplayName("Nested generic object creation")
    void generics3() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/generics/Generics3.java"));
    }

    @Test
    @DisplayName("Method declaration with parameter that has a generic wildcard")
    void generics4() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/generics/Generics4.java"));
    }

    @Test
    @DisplayName("Use of Reflection Type with an unknown type parameter")
    void generics5() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/generics/Generics5.java"));
    }
}
