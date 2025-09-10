package com.hello.bigclean.spoon.strategy;

import com.google.common.collect.Lists;
import com.hello.bigclean.spoon.helper.SpoonHelper;
import com.hello.bigclean.spoon.reference.Reference;
import org.aspectj.lang.annotation.Aspect;
import org.mapstruct.Mapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author hello.sheng
 * @version v1.0
 * @date 2025/4/3-14:06
 */
public class ScanAllClassStrategy implements ScanStrategy {
    private AtomicLong atomicLong = new AtomicLong(0);

    @Override
    public List<Reference> scan(String path) throws InterruptedException {
        Map<String, Reference> referenceMap = new HashMap<String, Reference>();
        try {
            // 使用更安全的Spoon初始化方法
            Launcher launcher = createSpoonLauncher();
            
            // 添加输入资源
            launcher.addInputResource(path);
            
            // 构建模型 - 添加错误处理
            launcher.getEnvironment().setAutoImports(false);
            launcher.getEnvironment().setNoClasspath(true);
            CtModel ctModel = launcher.buildModel();

            
            if (ctModel == null) {
                System.err.println("无法构建Spoon模型");
                return new ArrayList<>();
            }
            
            // 安全地获取元素
            List<CtType> elements = new ArrayList<>();
            try {
                elements = ctModel.getElements(new TypeFilter<>(CtType.class));
            } catch (Exception e) {
                System.err.println("获取Spoon元素时出错: " + e.getMessage());
                return new ArrayList<>();
            }
            
            if (elements.isEmpty()) {
                System.out.println("未找到任何Java类");
                return new ArrayList<>();
            }
            
            // 安全地过滤元素
            List<CtType> filterOriElements = new ArrayList<>();
            List<CtType> refElements = new ArrayList<>();
            
            try {
                filterOriElements = elements.stream().filter(ctType -> {
                    try {
                        return SpoonHelper.filterCtype(ctType)
                                && !ctType.hasAnnotation(Component.class)
                                && !ctType.hasAnnotation(Service.class)
                                && !ctType.hasAnnotation(RestController.class)
                                && !ctType.hasAnnotation(Aspect.class)
                                && !ctType.hasAnnotation(Configuration.class)
                                && !ctType.hasAnnotation(Repository.class)
                                && !ctType.hasAnnotation(Mapper.class);
                    } catch (Exception e) {
                        System.err.println("过滤类时出错: " + e.getMessage());
                        return false;
                    }
                }).collect(Collectors.toList());
                
                refElements = elements.stream().filter(ctType -> {
                    try {
                        return SpoonHelper.filterCtype(ctType);
                    } catch (Exception e) {
                        System.err.println("过滤引用类时出错: " + e.getMessage());
                        return false;
                    }
                }).collect(Collectors.toList());
            } catch (Exception e) {
                System.err.println("过滤元素时出错: " + e.getMessage());
                return new ArrayList<>();
            }
            
            // 安全地加载映射
            try {
                SpoonHelper.loadAllActualCtTypeMapping(refElements);
            } catch (Exception e) {
                System.err.println("加载类型映射时出错: " + e.getMessage());
            }
            
            // 分批处理，减少内存压力
            List<List<CtType>> splitCTypeList = Lists.partition(filterOriElements, 20); // 减少批次大小
            ExecutorService executorService = Executors.newFixedThreadPool(2); // 减少线程数
            List<CompletableFuture<String>> futures = new ArrayList<>();
            
            // 创建final变量供lambda使用
            final List<CtType> finalRefElements = refElements;
            final int totalSize = filterOriElements.size();
            
            for (List<CtType> childCTypeList : splitCTypeList) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        for (CtType<?> originalCType : childCTypeList) {
                            try {
                                boolean ref = false;
                                long incr = atomicLong.incrementAndGet();
                                if (incr % 20 == 0) {
                                    System.out.println("进度：" + incr + "/" + totalSize);
                                }
                                
                                for (CtType<?> referenceCType : finalRefElements) {
                                    try {
                                        if (SpoonHelper.isClassUsedBy(originalCType, referenceCType)) {
                                            ref = true;
                                            break;
                                        }
                                    } catch (Exception e) {
                                        System.err.println("检查类使用关系时出错: " + e.getMessage());
                                        continue;
                                    }
                                }
                                
                                if (!ref) {
                                    try {
                                        String oriClass = originalCType.getPackage() + "." + originalCType.getSimpleName();
                                        referenceMap.put(oriClass, new Reference(oriClass, null));
                                    } catch (Exception e) {
                                        System.err.println("获取类名时出错: " + e.getMessage());
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("处理类时出错: " + e.getMessage());
                                continue;
                            }
                        }
                        return Thread.currentThread().getName() + "- 完成";
                    } catch (Exception e) {
                        System.err.println("异步任务执行出错: " + e.getMessage());
                        return Thread.currentThread().getName() + "- 失败";
                    }
                }, executorService));
            }

            try {
                List<String> results = CompletableFuture.allOf(
                        futures.toArray(new CompletableFuture[0])
                ).thenApply(v ->
                        futures.stream()
                                .map(future -> {
                                    try {
                                        return future.join();
                                    } catch (Exception e) {
                                        return "任务失败: " + e.getMessage();
                                    }
                                })
                                .collect(Collectors.toList())
                ).get();
                System.out.println("所有任务完成，结果: " + results);
            } catch (Exception e) {
                System.err.println("任务执行异常或超时: " + e.getMessage());
            } finally {
                executorService.shutdown();  // 关闭线程池
            }
            return new ArrayList<>(referenceMap.values());
            
        } catch (Exception e) {
            System.err.println("Spoon分析失败: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        } catch (AssertionError e) {
            System.err.println("Spoon断言错误: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
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
            
            // 测试Spoon是否能正常加载
            testSpoonAvailability();
            
            Launcher launcher = new Launcher();
            
            // 配置环境
            launcher.getEnvironment().setAutoImports(true);
            launcher.getEnvironment().setNoClasspath(false);
            launcher.getEnvironment().setComplianceLevel(8);
            
            return launcher;
            
        } catch (Exception e) {
            System.err.println("创建Spoon Launcher失败: " + e.getMessage());
            throw new RuntimeException("无法初始化Spoon", e);
        }
    }
    
    /**
     * 测试Spoon类是否能正常加载
     */
    private void testSpoonAvailability() {
        try {
            // 测试基本类是否能加载
            Class.forName("spoon.Launcher");
            Class.forName("spoon.reflect.CtModel");
            Class.forName("spoon.reflect.declaration.CtType");
            System.out.println("Spoon类加载成功");
        } catch (ClassNotFoundException e) {
            System.err.println("Spoon类加载失败: " + e.getMessage());
            throw new RuntimeException("Spoon类不可用", e);
        }
    }
}
