package de.upb.sse.jess.model.stubs;

import com.github.javaparser.ast.CompilationUnit;
import lombok.*;


@RequiredArgsConstructor
@AllArgsConstructor
@Getter
@ToString
public class TypeContext {
    private final String type;
    @Setter private CompilationUnit context = null;
}
