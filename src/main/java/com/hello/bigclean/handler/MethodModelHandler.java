package com.hello.bigclean.handler;

import com.hello.bigclean.spoon.reference.MethodReference;
import com.hello.bigclean.spoon.strategy.ScanStrategy;
import com.hello.bigclean.spoon.strategy.StrategyFactory;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.ArrayList;
import java.util.List;

public class MethodModelHandler {
    /**
     * 构建无用方法的树形结构模型
     * 你可以在这里调用你的无用方法分析代码
     */
    public static DefaultTreeModel buildUnusedMethodTree(@NotNull Project project) {
        String projectPath = project.getBasePath();
        if (projectPath == null) {
            return DefaultModelHandler.createEmptyTreeModel("项目路径不可用");
        }

        try {
            ScanStrategy scanStrategy = StrategyFactory.obtainScanner("SCAN_ALL_METHOD");
            Object scanResult = scanStrategy.scan(projectPath);

            List<MethodReference> result = new ArrayList<>();
            // 安全的类型转换
            if (scanResult instanceof List<?>) {
                List<?> list = (List<?>) scanResult;
                for (Object item : list) {
                    if (item instanceof MethodReference) {
                        result.add((MethodReference) item);
                    }
                }
            }
            return buildTreeModelFromUnusedMethods(result);

        } catch (Exception e) {
            return DefaultModelHandler.createErrorTreeModel("分析失败: " + e.getMessage());
        }
    }


    /**
     * 从无用方法列表构建树形模型（直接使用MethodReference对象）
     */
    public static DefaultTreeModel buildTreeModelFromUnusedMethods(List<MethodReference> unusedMethods) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("无用方法分析结果");

        if (unusedMethods == null || unusedMethods.isEmpty()) {
            root.add(new DefaultMutableTreeNode("未发现无用方法"));
        } else {
            for (MethodReference method : unusedMethods) {
                // 直接使用MethodReference对象，而不是字符串
                root.add(new DefaultMutableTreeNode(method));
            }
        }

        return new DefaultTreeModel(root);
    }
}
