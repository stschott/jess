package de.upb.sse.jess.comparison;

public class MethodComparison {
    private String className;
    private final String methodName;
    private final String actualContent;
    private final String expectedContent;

    public MethodComparison(String methodName, String actualContent, String expectedContent) {
        this("unknown", methodName, actualContent, expectedContent);
    }

    public MethodComparison(String className, String methodName, String actualContent, String expectedContent) {
        this.className = className;
        this.methodName = methodName;
        this.actualContent = actualContent;
        this.expectedContent = expectedContent;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getActualContent() {
        return actualContent;
    }

    public String getExpectedContent() {
        return expectedContent;
    }

    public boolean isEqual() {
        if (actualContent.equals(expectedContent)) return true;
        if (isWildcardEqual()) {
            System.out.println("WILDCARD EQUALITY");
            return true;
        }
        return false;
    }

    public boolean isWildcardEqual() {
        String replaceRegex = "\\$\\d+\\(";
        String wildcardActual = actualContent.replaceAll(replaceRegex, "\\$x(");
        String wildcardExpected = expectedContent.replaceAll(replaceRegex, "\\$x(");
        return wildcardActual.equals(wildcardExpected);
    }

    @Override
    public String toString() {
        if (isEqual()) {
            return "MethodComparison{" + '\n' +
                    "className='" + className + '\'' + '\n' +
                    ", methodName='" + methodName + '\'' + '\n' +
                    ", equal='" + isEqual() + '\'' + '\n' +
                    '}';
        } else {
            return "--- ActualContent ---" + "\n\t" + actualContent + '\n' +
                    "---ExpectedContent ---" + "\n\t" + expectedContent;
        }
    }
}
