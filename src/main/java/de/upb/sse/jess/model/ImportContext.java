package de.upb.sse.jess.model;

import lombok.Data;

@Data
public class ImportContext {
    private final String name;
    private final boolean staticImport;
}
