# Jess

**Jess** is a tool that enables a targeted compilation of Java programs. By doing so, it provides the abillity to compile specific areas of interest of a program without the need to invoke build scripts, even when only part of its code base is available.

## Installation

To install Jess you just have to invoke `mvn install`.

## Usage

Jess can be programmaticaly invoked from a Java application in the following way:

1. Create a Jess configuration
```java
boolean exitOnCompilationFail = false;
boolean exitOnParsingFail = false;
boolean looseSignatureMatching = true; // if true one only needs to specify parameter types, not names
boolean keepAsteriskImports = true;
boolean failOnAmbiguity = false;
boolean disableStubbing = false;

JessConfiguration config = new JessConfiguration(exitOnCompilationFail, exitOnParsingFail, 
    looseSignatureMatching, keepAsteriskImports, failOnAmbiguity, disableStubbing);
```

2. Point to Java project for compilation via Jess
```java
String projectPath = "path/to/project";
Set<String> packages = PackageFinder.findPackageRoots(projectPath);
Set<String> jars = Collections.emptyList(); // can specify additional JAR-files here
``` 

3. Create a Jess instance and point to target file
```java
Jess jess = new Jess(config, packages, jars);
```

4. Specify targets for compilation
```java
String targetFile = "org/example/Target.java";
List<String> targetMethods = Collections.singletonList("method(String, String, boolean)")";
List<String> targetClinit = Collections.emptyList(); // static fields/initializers
List<String> targetInit = Collections.emptyList(); // non-static fields/initializers
```

5. Start targeted compilation
```java
jess.preSlice(targetClass, targetMethods, targetClinit, targetInit);
int compilationResult = jess.parse(targetClass); 
```
