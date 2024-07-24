package de.upb.sse.jess;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import de.upb.sse.jess.configuration.JessConfiguration;
import de.upb.sse.jess.model.ResolutionInformation;
import de.upb.sse.jess.annotation.Annotator;
import de.upb.sse.jess.visitors.*;
import de.upb.sse.jess.visitors.pre.PreSlicingVisitor;
import de.upb.sse.jess.visitors.slicing.SlicingVisitor;
import lombok.RequiredArgsConstructor;

import java.util.*;

@RequiredArgsConstructor
public class Slicer {

    private final JessConfiguration config;
    private final String targetClass;
    private final JavaSymbolSolver symbolSolver;
    private final Map<String, CompilationUnit> annotatedUnits;

    public Map<String, CompilationUnit> slice() {
        Annotator ann = new Annotator();

        for (Map.Entry<String, CompilationUnit> entry : annotatedUnits.entrySet()) {
            CompilationUnit currentUnit = entry.getValue();
            this.symbolSolver.inject(currentUnit);

            PreSlicingVisitor psv = new PreSlicingVisitor(this.targetClass, ann);
            psv.visit(currentUnit, null);

            SlicingVisitor sv = new SlicingVisitor(this.targetClass);
            sv.visit(currentUnit, null);

            ResolutionVisitor rv = new ResolutionVisitor(ann);
            rv.visit(currentUnit, null);

            ResolutionInformation usedTypesInfo = new ResolutionInformation();
            TypeUsageVisitor tuv = new TypeUsageVisitor();
            tuv.visit(currentUnit, usedTypesInfo);

            Set<String> usedTypes = usedTypesInfo.getAllTypes();
            UnusedImportsVisitor uiv = new UnusedImportsVisitor(usedTypes, config.isKeepAsteriskImports());
            uiv.visit(currentUnit, null);
        }

        Map<String, CompilationUnit> slicedUnits = ann.getAnnotatedUnits();
        slicedUnits.remove(this.targetClass);

        Map<String, CompilationUnit> fullySlicedUnits = new HashMap<>(slicedUnits);
        while (fullySlicedUnits.size() > 0) {
            Annotator fullySlicedAnnotator = new Annotator();
            for (Map.Entry<String, CompilationUnit> entry : fullySlicedUnits.entrySet()) {
                CompilationUnit currentUnit = entry.getValue();
                String currentClass = entry.getKey();
                this.symbolSolver.inject(currentUnit);

                PreSlicingVisitor psv = new PreSlicingVisitor(this.targetClass, ann);
                psv.visit(currentUnit, null);

                SlicingVisitor sv = new SlicingVisitor(this.targetClass);
                sv.visit(currentUnit, null);

                ResolutionVisitor rv = new ResolutionVisitor(fullySlicedAnnotator);
                rv.visit(currentUnit, null);

                ResolutionInformation usedTypesInfo = new ResolutionInformation();
                TypeUsageVisitor tuv = new TypeUsageVisitor();
                tuv.visit(currentUnit, usedTypesInfo);

                Set<String> usedTypes = usedTypesInfo.getAllTypes();
                UnusedImportsVisitor uiv = new UnusedImportsVisitor(usedTypes, config.isKeepAsteriskImports());
                uiv.visit(currentUnit, null);

                annotatedUnits.putIfAbsent(currentClass, currentUnit);
            }

            Map<String, CompilationUnit> newlyDiscoveredUnits = fullySlicedAnnotator.getAnnotatedUnits();
            for (Map.Entry<String, CompilationUnit> entry : annotatedUnits.entrySet()) {
                newlyDiscoveredUnits.remove(entry.getKey());
            }
            fullySlicedUnits = new HashMap<>(newlyDiscoveredUnits);
        }

        return annotatedUnits;
    }

}
