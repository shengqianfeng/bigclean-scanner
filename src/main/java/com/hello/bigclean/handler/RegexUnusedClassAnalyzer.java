package com.hello.bigclean.handler;

import com.hello.bigclean.scan.FindUnusedClasses;
import com.hello.bigclean.util.CodeUtils;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RegexUnusedClassAnalyzer {
    /**
     * 构建无用类的树形结构模型
     * 你可以在这里调用你的无用类分析代码
     */
    public static DefaultTreeModel buildUnusedClassTreeRegex(@NotNull Project project) {
        String projectPath = project.getBasePath();
        if (projectPath == null) {
            return DefaultModelHandler.createEmptyTreeModel("项目路径不可用");
        }

        try {
            // 调用你的无用类分析方法
            List<String> unusedClasses = getUnusedClasses(projectPath);

            return buildTreeModelFromUnusedClasses(unusedClasses);

        } catch (Exception e) {
            return DefaultModelHandler.createErrorTreeModel("分析失败: " + e.getMessage());
        }
    }


    /**
     * 从无用类列表构建树形模型
     */
    private static DefaultTreeModel buildTreeModelFromUnusedClasses(List<String> unusedClasses) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("无用类分析结果");

        if (unusedClasses == null || unusedClasses.isEmpty()) {
            root.add(new DefaultMutableTreeNode("未发现无用类"));
        } else {
            for (String className : unusedClasses) {
                root.add(new DefaultMutableTreeNode(className));
            }
        }

        return new DefaultTreeModel(root);
    }

    /**
     * 获取示例无用类数据 - 替换为你的实际分析调用
     */
    public static List<String> getUnusedClasses(String rootDir) {
        List<String> result = new ArrayList<>();
        FindUnusedClasses finder = new FindUnusedClasses(rootDir);
        long start = System.currentTimeMillis();
        try {
            List<Map.Entry<Path, String>> unusedClasses = finder.findUnusedClasses();

            if (!unusedClasses.isEmpty()) {
                for (Map.Entry<Path, String> entry : unusedClasses) {
                    // 从文件路径推导出类的全限定名
                    String fullClassName = CodeUtils.deriveFullClassName(entry.getKey(), rootDir, entry.getValue());
                    result.add(fullClassName);
                }
            } else {
                result.add("未找到未使用的类。");
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            finder.shutdown();
        }
        return result;
    }
}
