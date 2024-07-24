package de.upb.sse.jess.old;

import de.upb.sse.jess.Jess;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
public class LibraryTests {

    @Test
    @DisplayName("Apache Commons-IO")
    void commonsIO() throws IOException{
        runTests("input/commons-io/src/main/java", "input/commons-io/src/main", new Integer[]{ 54 });
    }

    @Test
    @DisplayName("Apache Commons-Lang")
    void commonsLang() throws IOException{
        runTests("input/commons-lang/src/main/java", "input/commons-lang/src/main", new Integer[]{ 3, 5, 34, 69, 135, 182 });
    }

    private void runTests(String target, String packageRoot, Integer[] exclusions) throws IOException {
        int index = 1;
        List<String> javaFiles = getAllJavaFiles(packageRoot);
        for (String javaFile : javaFiles) {
            if (Arrays.asList(exclusions).contains(index)) {
                index++;
                continue;
            }
            Jess jess = new Jess(Collections.singleton(target), Collections.emptyList());

            System.out.println(index++ + ": " + javaFile);
            assertEquals(0, jess.parse(javaFile));
            System.out.println("-------------------------------------------------");
//            System.exit(0);
        }
    }

    private List<String> getAllJavaFiles(String dir) {
        List<String> javaFiles = new ArrayList<>();
        try {
            javaFiles = Files.find(Paths.get(dir), 999,
                            (p, bfa) -> bfa.isRegularFile() &&
                                    p.getFileName().toString().matches(".*\\.java") &&
                                    !p.getFileName().toString().contains("package-info"))
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return javaFiles;
    }
}
