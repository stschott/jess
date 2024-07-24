package de.upb.sse.jess.model.stubs;


import lombok.Data;

@Data
public class FieldType {
    private final String name;
    private final TypeContext type;
    private final boolean staticField;
    private final Visibility visibility;
}
