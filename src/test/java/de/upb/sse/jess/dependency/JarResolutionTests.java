package de.upb.sse.jess.dependency;

import de.upb.sse.jess.Jess;
import de.upb.sse.jess.configuration.JessConfiguration;
import de.upb.sse.jess.finder.PomFinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JarResolutionTests {

    @BeforeEach
    void cleanupJars() {
        MavenDependencyResolver.cleanupJars();
    }

    @Test
    void jarResolutionTest1() {
        JessConfiguration conf = new JessConfiguration();
        conf.setDisableStubbing(true);

        Set<String> pomFilePaths = PomFinder.findPomFilePaths("src/test/resources/dependency/jar1");
        MavenDependencyResolver.resolveDependencies(pomFilePaths, true);
//        Set<String> jars = JarFinder.find(Jess.JAR_DIRECTORY);
        Set<String> jars = Collections.emptySet();
        Jess jess = new Jess(conf, Collections.emptySet(), jars);
        int parseResult = jess.parse("src/test/resources/dependency/jar1/src/main/java/Test.java");
        assertEquals(0, parseResult);
    }
}
