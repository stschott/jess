package de.upb.sse.jess.stubbing;

import de.upb.sse.jess.Jess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VariousTests {
    private static Jess jess;

    @BeforeEach
    void setupTests() {
        jess = new Jess();
    }

    @Test
    @DisplayName("instanceof expression usage")
    void variousTest1() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/various/InstanceOf.java"));
    }

    @Test
    @DisplayName("Typecast")
    void variousTest2() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/various/Typecast.java"));
    }

    @Test
    @DisplayName("Class expression (Something.class)")
    void variousTest3() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/various/ClassExpression.java"));
    }

    @Test
    @DisplayName("For-Each loop")
    void variousTest4() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/various/ForEachLoop.java"));
    }

    @Test
    @DisplayName("Class expression as argument to function call")
    void variousTest5() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/various/ClassExpressionParameter.java"));
    }
}
