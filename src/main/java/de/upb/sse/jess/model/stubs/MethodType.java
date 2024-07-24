package de.upb.sse.jess.model.stubs;

import com.github.javaparser.ast.type.ReferenceType;
import lombok.*;

import java.util.ArrayList;
import java.util.List;


@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class MethodType {
    private final String name;
    private final List<TypeContext> parameterTypes;
    private final TypeContext returnType;
    private final boolean constructor;
    private final boolean staticMethod;
    private Visibility visibility = Visibility.PUBLIC;
    private List<ReferenceType> thrownExceptions = new ArrayList<>();
}
