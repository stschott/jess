package de.upb.sse.jess.stubbing;

import de.upb.sse.jess.Jess;
import de.upb.sse.jess.exceptions.AmbiguityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MethodCallTests {
    private static Jess jess;

    @BeforeEach
    void setupTests() {
        jess = new Jess();
    }

    @Test
    @DisplayName("Virtual method call without arguments and no return (ambiguous)")
    void methodCall1ambiguous() {
        assertThrows(AmbiguityException.class, () -> jess.parse("src/test/resources/stubbing/methodcall/MethodCall1.java"));
    }

    @Test
    @DisplayName("Virtual method call without arguments and no return")
    void methodCall1() {
        jess.getConfig().setFailOnAmbiguity(false);
        assertEquals(0, jess.parse("src/test/resources/stubbing/methodcall/MethodCall1.java"));
    }

    @Test
    @DisplayName("Virtual method call without arguments and direct return assignment")
    void methodCall2() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/methodcall/MethodCall2.java"));
    }

    @Test
    @DisplayName("Static method call")
    void methodCall3() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/methodcall/MethodCall3.java"));
    }

    @Test
    @DisplayName("Virtual method call with constant arguments")
    void methodCall4() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/methodcall/MethodCall4.java"));
    }

    @Test
    @DisplayName("Virtual method call without arguments and indirect return assignment")
    void methodCall5() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/methodcall/MethodCall5.java"));
    }

    @Test
    @DisplayName("Virtual method call with indirect arguments")
    void methodCall6() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/methodcall/MethodCall6.java"));
    }

    @Test
    @DisplayName("Method call with null as argument (ambiguous)")
    void methodCall7ambiguous() {
        assertThrows(AmbiguityException.class, () -> jess.parse("src/test/resources/stubbing/methodcall/MethodCall7.java"));
    }

    @Test
    @DisplayName("Method call with null as argument")
    void methodCall7() {
        jess.getConfig().setFailOnAmbiguity(false);
        assertEquals(0, jess.parse("src/test/resources/stubbing/methodcall/MethodCall7.java"));
    }

    @Test
    @DisplayName("Resolvable nested method calls")
    void methodCall8() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/methodcall/MethodCall8.java"));
    }

    @Test
    @DisplayName("Method call with typecast")
    void methodCall9() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/methodcall/MethodCall9.java"));
    }

    @Test
    @DisplayName("Method call with external object as return type")
    void methodCall10() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/methodcall/MethodCall10.java"));
    }

    @Test
    @DisplayName("Chained method call with second method with second method being unresolvable")
    void methodCall11() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/methodcall/MethodCall11.java"));
    }

    @Test
    @DisplayName("Chained method call directly after object creation")
    void methodCall12() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/methodcall/MethodCall12.java"));
    }

    @Test
    @DisplayName("Method call with unresolvable super")
    void methodCall13() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/methodcall/MethodCall13.java"));
    }

    @Test
    @DisplayName("Method call with resolvable this (should always be resolvable)")
    void methodCall14() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/methodcall/MethodCall14.java"));
    }

    @Test
    @DisplayName("Method returning java.lang.Object")
    void methodCall15() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/methodcall/MethodCall15.java"));
    }

    @Test
    @DisplayName("Method call and scope within if-block")
    void methodCall16() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/methodcall/MethodCall16.java"));
    }

    @Test
    @DisplayName("Method call via super keyword from overwritten method with protected visibility and thrown exception in declaration")
    void methodCall17() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/methodcall/MethodCall17.java"));
    }

    @Test
    @DisplayName("Method call within return statement")
    void methodCall18() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/methodcall/MethodCall18.java"));
    }

}
