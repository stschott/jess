package de.upb.sse.jess.stubbing;

import de.upb.sse.jess.Jess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExceptionTests {
    private static Jess jess;

    @BeforeEach
    void setupTests() {
        jess = new Jess();
    }

    @Test
    @DisplayName("Throws in method signature")
    void exceptionTest1() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/exceptions/Exception1.java"));
    }

    @Test
    @DisplayName("Try-Catch")
    void exceptionTest2() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/exceptions/Exception2.java"));
    }

    @Test
    @DisplayName("Throw exception in method")
    void exceptionTest3() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/exceptions/Exception3.java"));
    }

    @Test
    @DisplayName("Throw java.lang.Exception in method signature")
    void exceptionTest4() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/exceptions/Exception4.java"));
    }
}
