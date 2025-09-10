package com.hello.bigclean.handler;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class DefaultModelHandler {
    /**
     * 创建空树模型
     */
    public static DefaultTreeModel createEmptyTreeModel(String message) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("分析结果");
        root.add(new DefaultMutableTreeNode(message));
        return new DefaultTreeModel(root);
    }


    /**
     * 创建错误树模型
     */
    public static DefaultTreeModel createErrorTreeModel(String errorMessage) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("错误");
        root.add(new DefaultMutableTreeNode(errorMessage));
        return new DefaultTreeModel(root);
    }
}
