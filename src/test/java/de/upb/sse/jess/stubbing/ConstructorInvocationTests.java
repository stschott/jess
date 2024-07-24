package de.upb.sse.jess.stubbing;

import de.upb.sse.jess.Jess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConstructorInvocationTests {
    private static Jess jess;

    @BeforeEach
    void setupTests() {
        jess = new Jess();
    }

    @Test
    @DisplayName("Super call with no arguments")
    void constructorInvocation1() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/constructorinvocation/ConstructorInvocation1.java"));
    }

    @Test
    @DisplayName("Super call with arguments")
    void constructorInvocation2() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/constructorinvocation/ConstructorInvocation2.java"));
    }
}
