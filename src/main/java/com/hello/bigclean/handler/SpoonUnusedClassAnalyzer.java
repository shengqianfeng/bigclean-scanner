package com.hello.bigclean.handler;

import com.hello.bigclean.spoon.reference.Reference;
import com.hello.bigclean.spoon.strategy.ScanStrategy;
import com.hello.bigclean.spoon.strategy.StrategyFactory;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.ArrayList;
import java.util.List;

public class SpoonUnusedClassAnalyzer {
    /**
     * 构建无用类的树形结构模型
     * 你可以在这里调用你的无用类分析代码
     */
    public static DefaultTreeModel buildUnusedClassTreeSpoon(@NotNull Project project) {
        String projectPath = project.getBasePath();
        if (projectPath == null) {
            return DefaultModelHandler.createEmptyTreeModel("项目路径不可用");
        }
        try {
            // 调用你的无用类分析方法
            List<Reference> unusedClasses = getUnusedClasses(projectPath);
            return buildTreeModelFromUnusedClasses(unusedClasses);

        } catch (Exception e) {
            return DefaultModelHandler.createErrorTreeModel("分析失败: " + e.getMessage());
        }
    }


    /**
     * 从无用类列表构建树形模型
     */
    private static DefaultTreeModel buildTreeModelFromUnusedClasses(List<Reference> unusedClasses) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("无用类分析结果");

        if (unusedClasses == null || unusedClasses.isEmpty()) {
            root.add(new DefaultMutableTreeNode("未发现无用类"));
        } else {
            for (Reference reference : unusedClasses) {
                root.add(new DefaultMutableTreeNode(reference));
            }
        }

        return new DefaultTreeModel(root);
    }


    public static List<Reference> getUnusedClasses(String rootDir) {
        List<Reference> result = new ArrayList<>();
        try {
            ScanStrategy scanStrategy = StrategyFactory.obtainScanner("SCAN_ALL_CLASS");
            Object scanResult = scanStrategy.scan(rootDir);
            
            // 安全的类型转换
            if (scanResult instanceof List<?>) {
                List<?> list = (List<?>) scanResult;
                for (Object item : list) {
                    if (item instanceof Reference) {
                        result.add((Reference) item);
                    }
                }
            }
            
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

}
