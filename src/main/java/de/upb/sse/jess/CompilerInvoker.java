package de.upb.sse.jess;

import de.upb.sse.jess.finder.JarFinder;
import de.upb.sse.jess.util.FileUtil;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class CompilerInvoker {
    private String targetVersion;
    private boolean silentCompilation;

    public CompilerInvoker() {
        this(false);
    }

    public CompilerInvoker(boolean silentCompilation) {
        this(null, silentCompilation);
    }

    public CompilerInvoker(String targetVersion, boolean silentCompilation) {
        this.targetVersion = targetVersion;
        this.silentCompilation = silentCompilation;
    }

    public boolean compileFile(String fileString, String output) {
        return this.compileFile(List.of(fileString), output);
    }

    public boolean compileFile(List<String> fileStrings, String output) {
        List<String> filesToCompile = new ArrayList<>();
        for (String fileString : fileStrings) {
            filesToCompile.addAll(getFileNames(new ArrayList<>(), Path.of(fileString)));
        }

        String classPath = "." + (FileUtil.isWindows() ? ";" : ":") + JarFinder.find(Jess.JAR_DIRECTORY).stream()
                .collect(Collectors.joining(FileUtil.isWindows() ? ";" : ":"));

        List<String> argLine = new ArrayList<>(filesToCompile);
        if (targetVersion != null && !targetVersion.equals("unknown")) {
            argLine.add("-source");
            argLine.add(this.targetVersion);
            argLine.add("-target");
            argLine.add(this.targetVersion);
        }
        argLine.add("-Xlint:-options");
        argLine.add("-cp");
        argLine.add(classPath);
        argLine.add("-d");
        argLine.add(output);
//        argLine.add("-Xlint:none");

//        cleanUp(output);

        String[] argLineString = argLine.toArray(String[]::new);
        JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
        OutputStream oStream = silentCompilation ? OutputStream.nullOutputStream() : null;
        InputStream iStream = silentCompilation ? InputStream.nullInputStream() : null;
        int result = comp.run(iStream, oStream, oStream, argLineString);
        return result == 0;
    }

    private List<String> getFileNames(List<String> fileNames, Path dir) {
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if(path.toFile().isDirectory()) {
                    getFileNames(fileNames, path);
                } else {
                    fileNames.add(path.toAbsolutePath().toString());
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        return fileNames;
    }

    private void cleanUp(String output) {
        File outputFile = new File(output);
        if (!outputFile.exists()) return;
        FileUtil.deleteRecursively(outputFile);
    }
}
