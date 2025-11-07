package com.hello.bigclean.spoon.helper;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.*;

/**
 * @author hello.sheng
 * @version v1.0
 * @date 2025/7/17-23:54
 */
public class MethodScanHelper {
    private static final Map<String, List<CtClass<?>>> interfaceToImplMap = new HashMap<>();
    private static final Map<String, CtMethod<?>> methodSigToMethodMap = new HashMap<>();

    private static final Set<String> CT_EXECUTABLE_REFERENCE_SIMPLE_NAMES = new HashSet<>(Arrays.asList(
            "get", "set", "add", "remove", "contains", "size", "clear", "isEmpty",
            "put", "getOrDefault", "computeIfAbsent", "computeIfPresent",
            "compute", "merge", "replaceAll", "forEach", "entrySet", "values", "keySet", "stream", "parallelStream"
    ));

    public static Set<CtMethod<?>> getUsedMethods(List<CtMethod<?>> allMethods, CtModel ctModel) {
        for (CtMethod<?> method : allMethods) {
            String methodSignature = method.getDeclaringType().getSimpleName() + "#" + method.getSimpleName() + "#" + CollectionUtils.size(method.getParameters());
            methodSigToMethodMap.put(methodSignature, method);
        }
        Set<CtMethod<?>> usedMethods = new HashSet<>();

        // 获取所有类
        List<CtClass<?>> ctClassesList = ctModel.getElements(new TypeFilter<>(CtClass.class));
        List<CtClass<?>> allClasses = new ArrayList<>();
        for (CtClass<?> ctClass : ctClassesList) {
            boolean exclued = "{}".equals(ctClass.toString()) ||
                    ctClass.hasAnnotation(Configuration.class) ||
                    ctClass.hasAnnotation(ConfigurationProperties.class);
            if (!exclued) {
                allClasses.add(ctClass);
            }
        }
        //预先构建接口到实现类的映射
        for (CtClass<?> clazz : allClasses) {
            populateInterfaceToImplMap(clazz.getReference(), clazz);
        }

        List<CtInvocation<?>> ctInvocations = new ArrayList<>();
        List<CtExecutableReferenceExpression> ctExecutableReferenceExpressions = new ArrayList<>();
        List<CtElement> ctElements = ctModel.getElements(new TypeFilter<>(CtElement.class));
        for (CtElement ctElement : ctElements) {
            if (ctElement instanceof CtInvocation) {
                ctInvocations.add((CtInvocation<?>) ctElement);
            } else if (ctElement instanceof CtExecutableReferenceExpression) {
                ctExecutableReferenceExpressions.add((CtExecutableReferenceExpression) ctElement);
            }
        }
        
        // 处理可执行引用表达式（方法引用）
        for (CtExecutableReferenceExpression expression : ctExecutableReferenceExpressions) {
            CtExecutableReference executable = expression.getExecutable();
            
            // 使用增强的方法获取逻辑
            CtMethod<?> method = getMethodFromExecutableReference(executable);
            if (method == null) {
                // 如果获取失败，尝试备用方法
                method = findMethodFromExecutableReference(executable, expression);
            }
            if (method != null) {
                usedMethods.add(method);
                // 如果是接口方法，查找实现类
                setImplMethodForCurrentInterface(usedMethods, method, expression.getType());
            } else {
                // 调试信息
                if (executable.getDeclaringType() != null && !nonBizCLass(executable.getDeclaringType())) {
                    System.out.println("⚠️ 未找到方法引用: " + executable.getSimpleName() + " (声明类型: " + executable.getDeclaringType().getQualifiedName() + ")");
                }
            }
        }

        // 处理方法调用
        for (CtInvocation<?> invocation : ctInvocations) {
            try {
                if (Objects.equals(invocation.toString(), "super()")) {
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            CtExecutableReference<?> execRef = invocation.getExecutable();
            CtMethod<?> method = null;
            
            if (execRef != null) {
                if (CT_EXECUTABLE_REFERENCE_SIMPLE_NAMES.contains(execRef.getSimpleName()) && filterMiddleWare(execRef)) {
                    continue;
                }
                
                // 使用增强的方法获取逻辑
                method = getMethodFromExecutableReference(execRef);
                if (method == null) {
                    // 如果获取失败，尝试备用方法
                    method = findMethodBySignature(execRef, invocation);
                }
                
                if (method != null) {
                    usedMethods.add(method);
                    CtExpression<?> target = invocation.getTarget();
                    if (target instanceof CtVariableRead) {
                        CtTypeReference<?> declaringType = invocation.getExecutable().getDeclaringType();
                        if (Objects.isNull(declaringType)) {
                            declaringType = invocation.getTarget().getType();
                        }
                        setImplMethodForCurrentInterface(usedMethods, method, declaringType);
                    } else if (target instanceof CtArrayRead) {
                        //TODO
                    } else if(target instanceof CtInvocation) {
                        setImplMethodForCurrentInterface(usedMethods, method, target.getType());
                    } else {
                        setImplMethodForCurrentInterface(usedMethods, method, invocation.getTarget().getType());
                    }
                } else {
                    // 增强的调试信息
                    CtTypeReference ctTypeReference = invocation.getExecutable().getDeclaringType();
                    if(Objects.nonNull(ctTypeReference) && !nonBizCLass(ctTypeReference)){
                        System.out.println("⚠️ 未找到方法: " + invocation + " (声明类型: " + ctTypeReference.getQualifiedName() + ")");
                    }
                }
            }
        }

        return usedMethods;
    }

    public static boolean nonBizCLass(CtTypeReference<?> ctTypeReference) {
        String qualifiedName = ctTypeReference.getQualifiedName();
        return StringUtils.isNotEmpty(qualifiedName)
                && !qualifiedName.startsWith("cn.") && !qualifiedName.startsWith("com.");

    }

    public static void setImplMethodForCurrentInterface(Set<CtMethod<?>> usedMethods, CtMethod<?> ctMethod, CtTypeReference<?> ctTypeReference) {
        try {
            if (ctTypeReference == null || nonBizCLass(ctTypeReference)) return;
            List<CtClass<?>> implClasses = interfaceToImplMap.getOrDefault(ctTypeReference.getSimpleName(), Collections.emptyList());
            if (implClasses.isEmpty()) {
                // 如果没有找到实现类，可能是因为接口没有被实现
                return;
            }
            for (CtClass<?> clazz : implClasses) {
                addImplMethodForSameSignature(usedMethods, clazz.getMethods(), ctMethod);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * @param usedMethods
     * @param methods     实现类的方法集合
     * @param ctMethod    接口方法
     */
    static void addImplMethodForSameSignature(Set<CtMethod<?>> usedMethods, Set<CtMethod<?>> methods, CtMethod<?> ctMethod) {
        for (CtMethod<?> method : methods) {
            if (isSameSignature(method, ctMethod)) {
                usedMethods.add(method);
                break;
            }
        }
    }

    static boolean isSameSignature(CtMethod<?> m1, CtMethod<?> m2) {
        if (!m1.getSimpleName().equals(m2.getSimpleName())) {
            return false;
        }
        List<CtParameter<?>> params1 = m1.getParameters();
        List<CtParameter<?>> params2 = m2.getParameters();
        if (params1.size() != params2.size()) {
            return false;
        }
        for (int i = 0; i < params1.size(); i++) {
            if (!params1.get(i).getType().getSimpleName().equals(params2.get(i).getType().getSimpleName())) {
                return false;
            }
        }
        return true;
    }

    private static void populateInterfaceToImplMap(CtTypeReference<?> type, CtClass<?> clazz) {
        if (type == null) {
            return;
        }
        interfaceToImplMap.computeIfAbsent(type.getSimpleName(), k -> new ArrayList<>()).add(clazz);

        // 递归处理父类和接口
        CtTypeReference<?> superclass = type.getSuperclass();
        if (superclass != null) {
            populateInterfaceToImplMap(superclass, clazz);
        }
        Set<CtTypeReference<?>> superInterfaces = type.getSuperInterfaces();
        for (CtTypeReference<?> iface : superInterfaces) {
            populateInterfaceToImplMap(iface, clazz);
        }
    }

    /**
     * 从 CtExecutableReference 获取对应的 CtMethod
     * 支持多种获取策略
     */
    public static CtMethod<?> getMethodFromExecutableReference(CtExecutableReference<?> execRef) {
        try {
            // 1. 直接获取方法声明（最可靠）
            if (execRef.getDeclaration() instanceof CtMethod) {
                return (CtMethod<?>) execRef.getDeclaration();
            }
            
            // 2. 通过方法签名匹配
            if (execRef.getDeclaringType() != null) {
                String className = execRef.getDeclaringType().getSimpleName();
                String methodName = execRef.getSimpleName();
                int paramCount = CollectionUtils.size(execRef.getParameters());
                
                // 尝试不同的签名格式
                String[] signatures = {
                    className + "#" + methodName + "#" + paramCount,
                    className + "#" + methodName + "#0",
                    className + "#" + methodName + "#1"
                };
                
                for (String sig : signatures) {
                    if (methodSigToMethodMap.containsKey(sig)) {
                        return methodSigToMethodMap.get(sig);
                    }
                }
                
                // 尝试全限定名匹配
                String fullClassName = execRef.getDeclaringType().getQualifiedName();
                if (fullClassName != null) {
                    String fullClassNameSimple = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
                    String[] fullSignatures = {
                        fullClassNameSimple + "#" + methodName + "#" + paramCount,
                        fullClassNameSimple + "#" + methodName + "#0",
                        fullClassNameSimple + "#" + methodName + "#1"
                    };
                    
                    for (String sig : fullSignatures) {
                        if (methodSigToMethodMap.containsKey(sig)) {
                            return methodSigToMethodMap.get(sig);
                        }
                    }
                }
            }
            
            // 3. 调试信息
            System.out.println("⚠️ 无法获取方法: " + execRef.getSimpleName() + 
                " (声明类型: " + (execRef.getDeclaringType() != null ? execRef.getDeclaringType().getQualifiedName() : "null") + 
                ", 参数数量: " + CollectionUtils.size(execRef.getParameters()) + ")");
            
        } catch (Exception e) {
            System.out.println("⚠️ 获取方法异常: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * 从可执行引用中查找方法（用于方法引用）
     */
    private static CtMethod<?> findMethodFromExecutableReference(CtExecutableReference<?> executable, CtExecutableReferenceExpression expression) {
        try {
            // 1. 尝试直接获取方法声明
            if (executable.getDeclaration() instanceof CtMethod) {
                return (CtMethod<?>) executable.getDeclaration();
            }
            
            // 2. 通过方法签名匹配
            String className = null;
            if (executable.getDeclaringType() != null) {
                className = executable.getDeclaringType().getSimpleName();
            } else if (expression.getTarget() instanceof CtTypeAccess) {
                // 处理静态方法引用，如 RouterHelper::random
                CtTypeAccess<?> typeAccess = (CtTypeAccess<?>) expression.getTarget();
                className = typeAccess.getAccessedType().getSimpleName();
            } else {
                // 尝试从target获取类名
                className = expression.getTarget().toString();
            }
            
            if (className != null) {
                // 尝试不同的参数数量匹配
                String methodName = executable.getSimpleName();
                
                // 对于方法引用，参数数量可能为0（如 RouterHelper::random）
                String sig = className + "#" + methodName + "#0";
                if (methodSigToMethodMap.containsKey(sig)) {
                    return methodSigToMethodMap.get(sig);
                }
                
                // 尝试参数数量为1（如 Function<T,R> 的情况）
                sig = className + "#" + methodName + "#1";
                if (methodSigToMethodMap.containsKey(sig)) {
                    return methodSigToMethodMap.get(sig);
                }
                
                // 尝试实际参数数量
                int paramCount = CollectionUtils.size(executable.getParameters());
                sig = className + "#" + methodName + "#" + paramCount;
                if (methodSigToMethodMap.containsKey(sig)) {
                    return methodSigToMethodMap.get(sig);
                }
                
                // 尝试全限定名匹配
                if (executable.getDeclaringType() != null) {
                    String fullClassName = executable.getDeclaringType().getQualifiedName();
                    if (fullClassName != null) {
                        String fullClassNameSimple = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
                        sig = fullClassNameSimple + "#" + methodName + "#0";
                        if (methodSigToMethodMap.containsKey(sig)) {
                            return methodSigToMethodMap.get(sig);
                        }
                        sig = fullClassNameSimple + "#" + methodName + "#1";
                        if (methodSigToMethodMap.containsKey(sig)) {
                            return methodSigToMethodMap.get(sig);
                        }
                        sig = fullClassNameSimple + "#" + methodName + "#" + paramCount;
                        if (methodSigToMethodMap.containsKey(sig)) {
                            return methodSigToMethodMap.get(sig);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            System.out.println("⚠️ 方法引用处理异常: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * 增强的方法签名匹配逻辑
     */
    private static CtMethod<?> findMethodBySignature(CtExecutableReference<?> execRef, CtInvocation<?> invocation) {
        String sig = null;
        try {
            if (execRef.getDeclaringType() != null) {
                // 标准方法调用
                sig = execRef.getDeclaringType().getSimpleName() + "#" + execRef.getSimpleName() + "#" + CollectionUtils.size(execRef.getParameters());
                if (methodSigToMethodMap.containsKey(sig)) {
                    return methodSigToMethodMap.get(sig);
                }
                
                // 尝试使用全限定名
                String fullClassName = execRef.getDeclaringType().getQualifiedName();
                if (fullClassName != null) {
                    String className = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
                    sig = className + "#" + execRef.getSimpleName() + "#" + CollectionUtils.size(execRef.getParameters());
                    if (methodSigToMethodMap.containsKey(sig)) {
                        return methodSigToMethodMap.get(sig);
                    }
                }
            } else {
                // 处理this访问
                if (invocation.getTarget() instanceof CtThisAccess) {
                    String fileName = invocation.getPosition().getCompilationUnit().toString();
                    String clsName = fileName.substring(0, fileName.lastIndexOf(".java"));
                    sig = clsName + "#" + execRef.getSimpleName() + "#" + CollectionUtils.size(((CtInvocation)execRef.getParent()).getArguments());
                    if (methodSigToMethodMap.containsKey(sig)) {
                        return methodSigToMethodMap.get(sig);
                    }
                }
                
                // 处理静态方法调用
                CtExpression<?> target = invocation.getTarget();
                if (target instanceof CtTypeAccess) {
                    CtTypeAccess<?> typeAccess = (CtTypeAccess<?>) target;
                    String className = typeAccess.getAccessedType().getSimpleName();
                    sig = className + "#" + execRef.getSimpleName() + "#" + CollectionUtils.size(execRef.getParameters());
                    if (methodSigToMethodMap.containsKey(sig)) {
                        return methodSigToMethodMap.get(sig);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ 方法签名匹配异常: " + e.getMessage());
        }
        return null;
    }

    private static boolean filterMiddleWare(CtExecutableReference<?> execRef) {
        return  Objects.nonNull(execRef.getDeclaringType()) && !execRef.getDeclaringType().getSimpleName().contains("redis") && !execRef.getDeclaringType().getSimpleName().contains("Redis");
    }

}
