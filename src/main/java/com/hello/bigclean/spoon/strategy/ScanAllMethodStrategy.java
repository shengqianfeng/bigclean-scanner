package com.hello.bigclean.spoon.strategy;

import com.hello.bigclean.spoon.helper.MethodScanHelper;
import com.hello.bigclean.spoon.reference.MethodReference;
import com.hello.bigclean.spoon.reference.Reference;
import org.apache.commons.collections.CollectionUtils;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.*;

import javax.annotation.PostConstruct;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author hello.sheng
 * @version v1.0
 * @date 2025/4/12-22:30
 */
public class ScanAllMethodStrategy implements ScanStrategy {
    //添加注解集合
    private static final Set<Class<? extends Annotation>> EXCLUDED_TYPE_ANNOTATIONS = new HashSet<>(Arrays.asList(
            Controller.class,
            RestController.class,
            RestControllerAdvice.class,
            ControllerAdvice.class,
            Aspect.class,
            Configuration.class
    ));
    //判断类型是否有任一排除注解
    private static boolean hasAnyAnnotation(CtType<?> type, Set<Class<? extends Annotation>> annotationClasses) {
        for (Class<? extends Annotation> ann : annotationClasses) {
            if (type.hasAnnotation(ann)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<? extends Reference> scan(String path) throws InterruptedException {
        try {
            Launcher launcher = createSpoonLauncher();
            launcher.addInputResource(path);
            
            // 配置环境以处理重复类名
            launcher.getEnvironment().setNoClasspath(true);
            launcher.getEnvironment().setAutoImports(false);
            launcher.getEnvironment().setIgnoreDuplicateDeclarations(true);  // 忽略重复声明
            launcher.getEnvironment().setShouldCompile(false);  // 不编译，只解析
            
            CtModel ctModel = null;
            try {
                ctModel = launcher.buildModel();
            } catch (Exception e) {
                System.err.println("方法扫描遇到重复类名，尝试使用容错模式: " + e.getMessage());
                // 重新创建launcher，使用更宽松的配置
                launcher = createTolerantSpoonLauncher();
                launcher.addInputResource(path);
                ctModel = launcher.buildModel();
            }
            
            if (ctModel == null) {
                System.err.println("无法构建Spoon模型用于方法扫描");
                return new ArrayList<>();
            }
            
            launcher.process();
        System.out.println("----------------------");
        // 获取所有项目中的方法，排除main方法和测试方法
        Collection<CtType<?>> allTypes = ctModel.getAllTypes();
        List<CtMethod<?>> allMethods = new ArrayList<>();
        for (CtType<?> type : allTypes) {
            allMethods.addAll(type.getMethods());
        }
        Set<CtMethod<?>> usedMethods = MethodScanHelper.getUsedMethods(allMethods, ctModel);

        // 找出未使用的方法
        List<CtMethod> unusedMethods = allMethods.stream()
                .filter(m -> !usedMethods.contains(m))
                .collect(Collectors.toList());

        List<MethodReference> unusedMethodsSet = new ArrayList<>();
        for (CtMethod method : unusedMethods) {
            MethodReference methodReference = new MethodReference();
            String qualifiedName = method.getDeclaringType().getQualifiedName();
            if (qualifiedName.contains("Controller")
                    || hasAnyAnnotation(method.getDeclaringType(), EXCLUDED_TYPE_ANNOTATIONS)
                    || qualifiedName.contains("Test")
                    || qualifiedName.contains("FallbackFactory")
                    || method.hasAnnotation(PostConstruct.class)
                    || hasMapStructAnnotation(method)
                    || isBuilderChainMethod(method)
                    || method.getDeclaringType() instanceof CtInterface
                    || method.getDeclaringType() instanceof CtAnnotationType
                    || Objects.equals(method.getSimpleName(), "afterPropertiesSet")
            ) {
                continue;
            }

            methodReference.setOriginalClass(qualifiedName);
            methodReference.setMethod(method.getSimpleName());
            methodReference.setMethodDescriptor(method.getSignature());
            methodReference.setMethodLines(lines(method));
            unusedMethodsSet.add(methodReference);
        }
        Collections.sort(unusedMethodsSet, (o1, o2) -> o2.getMethodLines() - o1.getMethodLines());
        System.out.println("Unused methods (" + unusedMethods.size() + "):");
        return unusedMethodsSet;
        
        } catch (Exception e) {
            System.err.println("方法扫描失败: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        } catch (AssertionError e) {
            System.err.println("方法扫描断言错误: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    private boolean isBuilderChainMethod(CtMethod method) {
        return method.getDeclaringType().getQualifiedName().endsWith("Builder");
    }



    public Integer lines(CtMethod method) {
        SourcePosition position = method.getPosition();
        int startLine = position.getLine();    // 方法起始行
        int endLine = position.getEndLine();   // 方法结束行
        return endLine - startLine + 1;
    }


    private static boolean hasMapStructAnnotation(CtMethod method) {
        List<CtAnnotation<? extends Annotation>> annotations = method.getDeclaringType().getAnnotations();
        if (CollectionUtils.isNotEmpty(annotations)) {
            return annotations.stream().anyMatch(annotation ->
                    annotation.toString().contains("org.mapstruct"));

        }
        return false;
    }
    
    /**
     * 创建容错模式的Spoon Launcher，处理重复类名问题
     */
    private Launcher createTolerantSpoonLauncher() {
        try {
            System.out.println("方法扫描使用容错模式创建Spoon Launcher");
            
            Launcher launcher = new Launcher();
            
            // 更宽松的配置
            launcher.getEnvironment().setAutoImports(false);
            launcher.getEnvironment().setNoClasspath(true);
            launcher.getEnvironment().setIgnoreDuplicateDeclarations(true);
            launcher.getEnvironment().setShouldCompile(false);
            launcher.getEnvironment().setComplianceLevel(8);
            
            // 设置更多容错选项
            launcher.getEnvironment().setIgnoreSyntaxErrors(true);
            launcher.getEnvironment().setPreserveLineNumbers(false);
            
            return launcher;
            
        } catch (Exception e) {
            System.err.println("创建容错Spoon Launcher失败: " + e.getMessage());
            throw new RuntimeException("无法初始化容错Spoon", e);
        }
    }
    
    /**
     * 创建Spoon Launcher，避免类加载器冲突
     */
    private Launcher createSpoonLauncher() {
        try {
            // 设置系统属性以避免日志冲突
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
            System.setProperty("org.slf4j.simpleLogger.showLogName", "false");
            System.setProperty("org.slf4j.simpleLogger.showShortLogName", "false");
            System.setProperty("org.slf4j.simpleLogger.showDateTime", "false");
            
            // 设置Spoon环境属性以避免断言错误
            System.setProperty("spoon.ignoreErrors", "true");
            System.setProperty("spoon.ignoreMissingTypes", "true");
            System.setProperty("spoon.ignoreSyntaxErrors", "true");
            
            Launcher launcher = new Launcher();
            
            // 配置环境 - 添加重复类名处理
            launcher.getEnvironment().setAutoImports(false);
            launcher.getEnvironment().setNoClasspath(true);
            launcher.getEnvironment().setComplianceLevel(8);
            launcher.getEnvironment().setIgnoreDuplicateDeclarations(true);  // 忽略重复声明
            launcher.getEnvironment().setShouldCompile(false);  // 不编译，只解析
            
            return launcher;
            
        } catch (Exception e) {
            System.err.println("创建Spoon Launcher失败: " + e.getMessage());
            throw new RuntimeException("无法初始化Spoon", e);
        }
    }
}
