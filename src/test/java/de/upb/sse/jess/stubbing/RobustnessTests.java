package de.upb.sse.jess.stubbing;


import de.upb.sse.jess.Jess;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class RobustnessTests {

    @Test
    void missingInterface() {
        Jess jess = new Jess(Collections.singleton("src/test/resources/stubbing/robustness/interface"), Collections.emptyList());
        jess.getConfig().setFailOnAmbiguity(false);
        assertNotEquals(2, jess.parse("src/test/resources/stubbing/robustness/interface/interface1/MissingInterface.java"));
    }

    @Test
    void missingSuperClass() {
        Jess jess = new Jess(Collections.singleton("src/test/resources/stubbing/robustness/superclass"), Collections.emptyList());
        jess.getConfig().setFailOnAmbiguity(false);
        assertNotEquals(2, jess.parse("src/test/resources/stubbing/robustness/superclass/MissingSuperClass.java"));
    }

    @Test
    @DisplayName("Mix of resolvable and not resolvable interfaces")
    void interface2() throws IOException {
        Jess jess = new Jess(Collections.singleton("src/test/resources/stubbing/robustness/interface"), Collections.emptyList());
        jess.preSlice( "src/test/resources/stubbing/robustness/interface/interface2/Interface2.java", List.of("method()"));
        assertEquals(0, jess.parse("src/test/resources/stubbing/robustness/interface/interface2/Interface2.java"));
    }

    @Test
    @DisplayName("Mix of resolvable and not resolvable interfaces via interface extension")
    void interface3() throws IOException {
        Jess jess = new Jess(Collections.singleton("src/test/resources/stubbing/robustness/interface"), Collections.emptyList());
        jess.preSlice( "src/test/resources/stubbing/robustness/interface/interface3/Interface3.java", List.of("method()"));
        assertEquals(0, jess.parse("src/test/resources/stubbing/robustness/interface/interface3/Interface3.java"));
    }
}
