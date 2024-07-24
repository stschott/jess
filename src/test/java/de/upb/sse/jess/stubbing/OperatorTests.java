package de.upb.sse.jess.stubbing;

import de.upb.sse.jess.Jess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OperatorTests {
    private static Jess jess;

    @BeforeEach
    void setupTests() {
        jess = new Jess();
        jess.getConfig().setFailOnAmbiguity(false);
    }

    @Test
    @DisplayName("Single string concat")
    void operatorTest1() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/operators/Operators1.java"));
    }

}
