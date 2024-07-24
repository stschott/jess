package de.upb.sse.jess.dependency;

import de.upb.sse.jess.Jess;
import de.upb.sse.jess.util.FileUtil;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MavenDependencyResolver {

    public static List<String> extractProfiles(Collection<String> inputPoms) {
        List<String> profiles = new ArrayList<>();
        for (String inputPom : inputPoms) {
            profiles.addAll(extractProfiles(inputPom));
        }
        return profiles;
    }

    public static List<String> extractProfiles(String inputPom) {
        List<String> profiles = new ArrayList<>();
        try {
            String outDir = new File(Jess.JAR_DIRECTORY).getAbsolutePath();
            String mavenExecutable = System.getProperty("os.name").startsWith("Windows") ? "mvn.cmd" : "mvn";
            ProcessBuilder processBuilder = new ProcessBuilder(mavenExecutable, "help:all-profiles");
            processBuilder.directory(Path.of(inputPom).toFile());

            Process process = processBuilder.start();

            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String output = "";
            String line;
            while ((line = reader.readLine()) != null) {
                output += line;
                output += '\n';
                System.out.println(line);
            }
            reader.close();
            int exitCode = process.waitFor();

            System.out.println("Process exited with code: " + exitCode);


            String profileRegex = "Profile\\sId:\\s(.+)\\s\\(";
            Pattern pattern = Pattern.compile(profileRegex);
            Matcher matcher = pattern.matcher(output);

            while (matcher.find()) {
                String profile = matcher.group(1); // This will give you the first capture group
                profiles.add(profile);
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return profiles;
    }

    public static void resolveDependencies(Collection<String> inputPoms, boolean transitive) {
        for (String inputPom : inputPoms) {
            resolveDependencies(inputPom, transitive, Collections.emptyList());
        }
    }

    public static void resolveDependencies(Collection<String> inputPoms, boolean transitive, Collection<String> profiles) {
        for (String inputPom : inputPoms) {
            resolveDependencies(inputPom, transitive, profiles);
        }
    }

    public static void resolveDependencies(String inputPom, boolean transitive) {
        resolveDependencies(inputPom, transitive, Collections.emptyList());
    }

    public static void resolveDependencies(String inputPom, boolean transitive, Collection<String> profiles) {
        try {
            String outDir = new File(Jess.JAR_DIRECTORY).getAbsolutePath();
            String mavenExecutable = System.getProperty("os.name").startsWith("Windows") ? "mvn.cmd" : "mvn";

            List<String> command = new ArrayList<>();
            command.add(mavenExecutable);
            command.add("de.upb.sse:dependency-download-maven-plugin:dependency-download");
            command.add("-Dddload.transitive=" + transitive);
            command.add("-Dddload.outdir=" + outDir);
            for (String profile : profiles) {
                command.add("-P" + profile);
            }

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(Path.of(inputPom).toFile());

            Process process = processBuilder.start();

            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            reader.close();

            int exitCode = process.waitFor();

            System.out.println("Process exited with code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void resolveDependencies2(String inputPom) {
        try {
            String outDir = new File(Jess.JAR_DIRECTORY).getAbsolutePath();
            String mavenExecutable = System.getProperty("os.name").startsWith("Windows") ? "mvn.cmd" : "mvn";

            List<String> command = new ArrayList<>();
            command.add(mavenExecutable);
            command.add("-U");
            command.add("de.upb.sse:dependency-download-maven-plugin:1.1:dependency-download-2");
            command.add("-DoutputDirectory=" + outDir);


            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(Path.of(inputPom).toFile());

            Process process = processBuilder.start();

            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            reader.close();

            int exitCode = process.waitFor();

            System.out.println("Process exited with code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void cleanupJars() {
        FileUtil.deleteRecursively(new File(Jess.JAR_DIRECTORY));
    }

    public static Set<String> getJars() {
        String[] filesInJarDir = new File(Jess.JAR_DIRECTORY).list();

        return filesInJarDir == null ? new HashSet<>() :
                Arrays.stream(filesInJarDir).filter(f -> f.endsWith(".jar")).collect(Collectors.toSet());
    }

}
