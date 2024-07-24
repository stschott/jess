package de.upb.sse.jess.finder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class JarFinder {

    public static Set<String> find(String rootPath) {
        if (!new File(rootPath).exists()) return new HashSet<>();
        try {
            return Files.find(Paths.get(rootPath), 999,
                            (p, bfa) -> bfa.isRegularFile() && p.getFileName().toString().endsWith(".jar")
                    ).map(Path::toString)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
            return new HashSet<>();
        }
    }
}
