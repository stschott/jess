package de.upb.sse.jess.stubbing;

import de.upb.sse.jess.Jess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnhancementTests {
    private static Jess jess;

    @BeforeEach
    void setupTests() {
        jess = new Jess(Collections.singleton("src/test/resources/stubbing/enhance/"), Collections.emptyList());
    }

    @Test
    @DisplayName("Enhancing a sliced class")
    void enhancementTest1() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/enhance/existing1/Existing1.java"));
    }

    @Test
    @DisplayName("Enhancing a sliced class with methods with same name")
    void enhancementTest2() {
        assertEquals(0, jess.parse("src/test/resources/stubbing/enhance/existing2/Existing2.java"));
    }
}
