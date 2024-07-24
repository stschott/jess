package de.upb.sse.jess.annotation;

public class Annotations {
    public static final String KEEP_ALL_TEMPLATE = "package {{package}};\n" +
            "import java.lang.annotation.*;\n" +
            "\n" +
            "@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE, ElementType.FIELD})\n" +
            "@Retention(RetentionPolicy.CLASS)\n" +
            "public @interface KeepAll {\n" +
            "}";

    public static final String TARGET_METHOD_TEMPLATE = "package {{package}};\n" +
            "import java.lang.annotation.*;\n" +
            "\n" +
            "@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE, ElementType.FIELD})\n" +
            "@Retention(RetentionPolicy.CLASS)\n" +
            "public @interface TargetMethod {\n" +
            "}";
}
