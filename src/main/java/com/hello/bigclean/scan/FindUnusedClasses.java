package com.hello.bigclean.scan;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FindUnusedClasses {
    // 排除的文件模式
    private static final List<String> EXCLUDED_PATTERNS = Arrays.asList(
            "Impl\\.java$",
            "Controller\\.java$",
            "Configuration\\.java$",
            "Pointcut\\.java$",
            "Aspect\\.java$",
            "Test\\.java$",
            "Tester\\.java$",
            "Strategy\\.java$"
    );

    // 用于检测类引用的模式
    private static final List<String> REFERENCE_PATTERNS = Arrays.asList(
            "import\\s+.*\\.%s\\s*;",                  // Import语句
            "extends\\s+%s[\\s{]",                     // 继承
            "implements\\s+.*%s[\\s,{]",               // 实现接口
            "@%s[\\s(]",                               // 注解
            "<\\s*%s\\s*>",                            // 泛型类型
            "<\\s*\\?\\s+extends\\s+%s\\s*>",          // 通配符extends
            "<\\s*\\?\\s+super\\s+%s\\s*>",            // 通配符super
            "%s\\s+\\w+\\s*[;=]",                      // 变量声明
            "%s\\[\\]",                                // 数组声明
            "new\\s+%s\\s*\\(",                        // 对象实例化
            "throws\\s+.*%s",                          // 异常声明
            "catch\\s*\\(\\s*%s",                      // Catch块
            "public\\s+.*%s\\s+\\w+\\s*\\(",           // 方法返回类型
            "\\(\\s*%s\\s+\\w+",                       // 方法参数
            ",\\s*%s\\s+\\w+",                         // 逗号后的方法参数
            "static\\s+.*%s\\s+\\w+",                  // 静态字段
            "%s\\.class",                              // 类字面量
            "%s\\.",                                   // 静态方法/字段访问
            "instanceof\\s+%s"                         // instanceof检查
    );

    private final String rootDir;
    private final ExecutorService executor;

    public FindUnusedClasses(String rootDir) {
        this.rootDir = rootDir;
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 查找所有Java文件，排除特定模式
     */
    public List<Path> findJavaFiles() throws IOException {
        System.out.println("查找Java文件（排除指定模式）...");
        try (Stream<Path> paths = Files.walk(Paths.get(rootDir))) {
            List<Path> javaFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !isExcluded(path.toString()))
                    .collect(Collectors.toList());
            System.out.println("找到 " + javaFiles.size() + " 个Java文件进行分析。");
            return javaFiles;
        }
    }

    /**
     * 获取所有Java文件，包括排除模式的文件
     */
    public List<Path> getAllJavaFiles() throws IOException {
        System.out.println("获取所有Java文件用于引用检查...");
        try (Stream<Path> paths = Files.walk(Paths.get(rootDir))) {
            List<Path> allJavaFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .collect(Collectors.toList());
            System.out.println("用于引用检查的Java文件总数: " + allJavaFiles.size());
            return allJavaFiles;
        }
    }

    /**
     * 检查文件是否应该被排除
     */
    private boolean isExcluded(String filePath) {
        for (String pattern : EXCLUDED_PATTERNS) {
            if (filePath.matches(".*" + pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从Java文件中提取类名
     */
    public String extractClassName(Path filePath) throws IOException {
        String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
        Pattern pattern = Pattern.compile("(public|private|protected)?\\s+(class|interface|enum)\\s+(\\w+)");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(3);
        } else {
            // 回退到文件名
            String fileName = filePath.getFileName().toString();
            return fileName.substring(0, fileName.length() - 5); // 移除.java后缀
        }
    }

    /**
     * 检查类是否在任何文件中被使用
     */
    public boolean checkClassUsage(String className, Path filePath, List<Path> allFiles) throws IOException {
        for (Path file : allFiles) {
            // 跳过定义该类的文件
            if (file.equals(filePath)) {
                continue;
            }

            String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            
            // 检查各种可能引用类的方式
            for (String patternTemplate : REFERENCE_PATTERNS) {
                String patternString = String.format(patternTemplate, Pattern.quote(className));
                Pattern pattern = Pattern.compile(patternString);
                if (pattern.matcher(content).find()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 查找未使用的类
     */
    public List<Map.Entry<Path, String>> findUnusedClasses() throws IOException, InterruptedException, ExecutionException {
        List<Path> javaFiles = findJavaFiles();
        List<Path> allJavaFiles = getAllJavaFiles();
        
        System.out.println("提取类名...");
        Map<Path, String> classInfo = new HashMap<>();
        for (Path filePath : javaFiles) {
            try {
                String className = extractClassName(filePath);
                classInfo.put(filePath, className);
            } catch (Exception e) {
                System.err.println("提取类名时出错 " + filePath + ": " + e.getMessage());
            }
        }
        
        System.out.println("分析类的使用情况（这可能需要一些时间）...");
        List<Map.Entry<Path, String>> unusedClasses = new ArrayList<>();
        
        // 使用CompletionService进行并行处理
        CompletionService<Map.Entry<Path, Boolean>> completionService = 
                new ExecutorCompletionService<>(executor);
        
        int totalTasks = 0;
        for (Map.Entry<Path, String> entry : classInfo.entrySet()) {
            final Path filePath = entry.getKey();
            final String className = entry.getValue();
            
            completionService.submit(() -> {
                boolean isUsed = checkClassUsage(className, filePath, allJavaFiles);
                return new AbstractMap.SimpleEntry<>(filePath, isUsed);
            });
            totalTasks++;
        }
        
        int completed = 0;
        while (completed < totalTasks) {
            Future<Map.Entry<Path, Boolean>> future = completionService.take();
            Map.Entry<Path, Boolean> result = future.get();
            completed++;
            
            if (completed % 10 == 0) {
                System.out.println("已处理 " + completed + "/" + totalTasks + " 个类...");
            }
            
            if (!result.getValue()) { // 如果类未被使用
                Path filePath = result.getKey();
                String className = classInfo.get(filePath);
                unusedClasses.add(new AbstractMap.SimpleEntry<>(filePath, className));
            }
        }
        
        return unusedClasses;
    }
}

    