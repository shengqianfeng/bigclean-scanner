package com.hello.bigclean.spoon.helper;

import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author hello.sheng
 * @version v1.0
 * @date 2025/4/2-19:40
 */
public class SpoonHelper {

    private static Map<CtType<?>, Set<CtType<?>>> ACTUAL_CTTYPE_MAPPING = new ConcurrentHashMap<>();

    public static boolean isClassUsedBy(CtType<?> classA, CtType<?> classB) {
        if (Objects.equals(classA.getSimpleName(), classB.getSimpleName()) && Objects.equals(classA.getPackage().getSimpleName(), classB.getPackage().getSimpleName())) {
            return false;
        }

        Set<CtType<?>> ctTypes = ACTUAL_CTTYPE_MAPPING.get(classB);
        if (Objects.nonNull(ctTypes) && ctTypes.stream().anyMatch(ctType -> ctType.equals(classA))) {
            return true;
        }
        return false;
    }

    public static void loadAllActualCtTypeMapping(List<CtType> refElements) {
        Map<CtType<?>, Set<CtType<?>>> resultMap = new HashMap<>();
        for (CtType refCtype : refElements) {
            List<CtTypeReference<?>> refs = refCtype.getElements(new TypeFilter<>(CtTypeReference.class));
            refs.stream()
                    .forEach(ctTypeReference -> {
                CtType<?> actualType = ctTypeReference.getTypeDeclaration();
                if (Objects.nonNull(actualType)) {
                    Set<CtType<?>> actualTypes = resultMap.containsKey(refCtype) ? resultMap.get(refCtype) : new HashSet<>();
                    actualTypes.add(actualType);
                    resultMap.put(refCtype, actualTypes);
                }
            });

        }
        ACTUAL_CTTYPE_MAPPING =  resultMap;
        System.out.println("✅loadAllActualCtTypeMapping done.........");
    }

    public static boolean filterCtype(CtType<?> ctType) {
        return Objects.nonNull(ctType.getPackage());
    }
}
