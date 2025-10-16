package com.hello.bigclean;

import com.hello.bigclean.handler.RegexUnusedClassAnalyzer;
import com.hello.bigclean.handler.MethodModelHandler;
import com.hello.bigclean.handler.SpoonUnusedClassAnalyzer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

public class ClassPathAnalyzerFactory implements ToolWindowFactory {
    private JTree unusedClassTreeRegex;
    private JTree unusedClassTreeSpoon;
    private JTree unusedMethodTree;
    private Project currentProject;
    
    // 静态初始化块 - 插件加载时就会执行
    static {
        System.out.println("=== ClassPathAnalyzerFactory 类被加载 ===");
    }
    
    // 构造函数 - 创建实例时执行
    public ClassPathAnalyzerFactory() {
        System.out.println("=== ClassPathAnalyzerFactory 构造函数被调用 ===");
    }
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        System.out.println("=== ClassPathAnalyzerFactory.createToolWindowContent() 被调用 ===");
        System.out.println("项目名称: " + project.getName());
        System.out.println("项目路径: " + project.getBasePath());
        this.currentProject = project;
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(UIUtil.getPanelBackground());
        
        // 添加项目路径显示面板
        JPanel projectInfoPanel = createProjectInfoPanel(project);
        mainPanel.add(projectInfoPanel, BorderLayout.NORTH);
        
        JTabbedPane mainTabbedPane = createStyledTabbedPane();

        // 初始化树组件
        unusedClassTreeRegex = createStyledTree();
        unusedClassTreeSpoon = createStyledTree();
        unusedMethodTree = createStyledTree();
        
        // 为每个树组件添加鼠标点击事件监听器
        addNavigationListener(unusedClassTreeRegex);
        addNavigationListener(unusedClassTreeSpoon);
        addNavigationListener(unusedMethodTree);
        
        // 创建无用类的子标签页
        JTabbedPane unusedClassTabbedPane = createUnusedClassTabbedPane();
        
        // 移除自动触发，改为手动触发
        // refreshAllAnalysisData();
        
        // 初始化空的树模型并显示提示信息
        initializeEmptyTrees();
        
        mainTabbedPane.addTab("无用类", unusedClassTabbedPane);
        mainTabbedPane.addTab("无用方法", createUnusedMethodTabbedPane());

        mainPanel.add(mainTabbedPane, BorderLayout.CENTER);
        toolWindow.getComponent().add(mainPanel);
    }
    
    /**
     * 创建无用类的子标签页
     */
    private JTabbedPane createUnusedClassTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // 正则表达式分析法标签页
        JPanel regexPanel = createTabPanelWithRefreshButton(
            unusedClassTreeRegex, 
            "使用正则表达式分析Java文件，快速识别潜在的无用类",
            "正则表达式分析",
            () -> refreshRegexAnalysis()
        );
        tabbedPane.addTab("正则表达式分析", regexPanel);
        
        // Spoon AST分析法标签页
        JPanel spoonPanel = createTabPanelWithRefreshButton(
            unusedClassTreeSpoon, 
            "使用Spoon AST分析，提供更精确的语法分析和依赖关系检查",
            "Spoon AST分析",
            () -> refreshSpoonAnalysis()
        );
        tabbedPane.addTab("Spoon AST分析", spoonPanel);
        
        return tabbedPane;
    }
    
    /**
     * 创建无用方法的标签页
     */
    private JPanel createUnusedMethodTabbedPane() {
        return createTabPanelWithRefreshButton(
            unusedMethodTree, 
            "分析项目中未被使用的方法",
            "无用方法分析",
            () -> refreshMethodAnalysis()
        );
    }
    
    /**
     * 创建带有刷新按钮的标签页面板
     */
    private JPanel createTabPanelWithRefreshButton(JTree tree, String description, String tabName, Runnable refreshAction) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIUtil.getPanelBackground());
        
        // 顶部：描述和刷新按钮
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));
        topPanel.setBackground(UIUtil.getPanelBackground());
        
        // 描述标签
        JLabel descLabel = new JLabel(description);
        descLabel.setForeground(JBColor.foreground());
        descLabel.setFont(UIUtil.getLabelFont());
        
        // 耗时显示标签
        JLabel timingLabel = new JLabel("上次执行耗时: -");
        timingLabel.setForeground(JBColor.GRAY);
        timingLabel.setFont(UIUtil.getLabelFont().deriveFont(UIUtil.getLabelFont().getSize() - 1f));
        
        // 刷新按钮
        JButton refreshButton = createStyledButton("Refresh", 120, 32);
        refreshButton.addActionListener(e -> {
            refreshButton.setEnabled(false);
            refreshButton.setText("Analyzing...");
            timingLabel.setText("Executing...");
            
            long startTime = System.currentTimeMillis();
            SwingUtilities.invokeLater(() -> {
                try {
                    refreshAction.run();
                } finally {
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;
                    
                    refreshButton.setEnabled(true);
                    refreshButton.setText("Refresh");
                    timingLabel.setText("上次执行耗时: " + formatDuration(duration));
                }
            });
        });
        
        // 左侧面板：描述和耗时
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setOpaque(false);
        leftPanel.add(descLabel, BorderLayout.NORTH);
        leftPanel.add(timingLabel, BorderLayout.SOUTH);
        
        topPanel.add(leftPanel, BorderLayout.WEST);
        topPanel.add(refreshButton, BorderLayout.EAST);
        
        // 中间：树组件
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(UIUtil.getPanelBackground());
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建样式化的标签页组件
     */
    private JTabbedPane createStyledTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // 设置字体
        tabbedPane.setFont(UIUtil.getLabelFont());
        
        // 设置颜色
        tabbedPane.setBackground(UIUtil.getPanelBackground());
        tabbedPane.setForeground(JBColor.foreground());
        
        // 设置边框
        tabbedPane.setBorder(BorderFactory.createEmptyBorder());
        
        return tabbedPane;
    }
    
    /**
     * 创建样式化的树组件
     */
    private JTree createStyledTree() {
        JTree tree = new JTree();
        
        // 设置字体
        tree.setFont(UIUtil.getTreeFont());
        
        // 设置颜色
        tree.setBackground(UIUtil.getTreeBackground());
        tree.setForeground(JBColor.foreground());
        
        // 设置边框
        tree.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        
        // 设置行高
        tree.setRowHeight(22);
        
        // 设置选择模式
        tree.getSelectionModel().setSelectionMode(javax.swing.tree.DefaultTreeSelectionModel.SINGLE_TREE_SELECTION);
        
        // 设置根节点可见性
        tree.setRootVisible(false);
        
        return tree;
    }
    
    /**
     * 创建样式化的按钮 - 强制颜色设置，不依赖主题
     */
    private JButton createStyledButton(String text, int width, int height) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(width, height));
        
        // 强制设置字体 - 使用系统默认字体，确保可见
        button.setFont(new Font("Dialog", Font.BOLD, 12));
        
        // 强制设置颜色 - 橘红色字体，在任何背景下都清晰可见
        button.setForeground(new Color(255, 69, 0)); // 橘红色
        button.setBackground(new Color(52, 152, 219));
        
        // 强制设置边框
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // 强制设置不透明
        button.setOpaque(true);
        
        // 添加悬停效果
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(new Color(41, 128, 185));
                    button.setForeground(new Color(255, 69, 0)); // 橘红色字体
                }
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(new Color(52, 152, 219));
                    button.setForeground(new Color(255, 69, 0)); // 橘红色字体
                }
            }
        });
        
        // 强制属性变化监听器
        button.addPropertyChangeListener("enabled", evt -> {
            if (!button.isEnabled()) {
                // 禁用状态：橘红色字体 + 浅灰色背景
                button.setForeground(new Color(255, 69, 0)); // 橘红色字体
                button.setBackground(new Color(200, 200, 200));
            } else {
                // 启用状态：橘红色字体 + 蓝色背景
                button.setForeground(new Color(255, 69, 0)); // 橘红色字体
                button.setBackground(new Color(52, 152, 219));
            }
        });
        
        return button;
    }
    
    /**
     * 创建显示项目信息的面板
     */
    private JPanel createProjectInfoPanel(@NotNull Project project) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));
        panel.setBackground(UIUtil.getPanelBackground());
        
        // 左侧：项目信息
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        infoPanel.setOpaque(false);
        
        // 项目名称标签
        JLabel projectNameLabel = new JLabel("项目: " + project.getName());
        projectNameLabel.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD, UIUtil.getLabelFont().getSize() + 2f));
        projectNameLabel.setForeground(JBColor.foreground());
        
        // 项目路径标签
        String projectPath = project.getBasePath();
        JLabel projectPathLabel = new JLabel("路径: " + (projectPath != null ? projectPath : "未知"));
        projectPathLabel.setFont(UIUtil.getLabelFont().deriveFont(UIUtil.getLabelFont().getSize() - 1f));
        projectPathLabel.setForeground(JBColor.GRAY);
        
        infoPanel.add(projectNameLabel);
        infoPanel.add(projectPathLabel);
        
        // 右侧：联系信息
        JPanel contactPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        contactPanel.setOpaque(false);
        
        JLabel contactLabel = new JLabel("如有问题请联系：1007424128@qq.com");
        contactLabel.setFont(UIUtil.getLabelFont().deriveFont(UIUtil.getLabelFont().getSize() - 1f));
        contactLabel.setForeground(JBColor.GRAY);
        
        contactPanel.add(contactLabel);
        
        // 将信息面板和联系面板添加到主面板
        panel.add(infoPanel, BorderLayout.WEST);
        panel.add(contactPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    
    /**
     * 刷新正则表达式分析
     */
    private void refreshRegexAnalysis() {
        if (currentProject == null) return;
        
        try {
            DefaultTreeModel unusedClassTreeModelRegex = RegexUnusedClassAnalyzer.buildUnusedClassTreeRegex(currentProject);
            unusedClassTreeRegex.setModel(unusedClassTreeModelRegex);
        } catch (Exception e) {
            showError("正则表达式分析失败: " + e.getMessage());
        }
    }
    
    /**
     * 刷新Spoon AST分析
     */
    private void refreshSpoonAnalysis() {
        if (currentProject == null) return;
        
        try {
            DefaultTreeModel unusedClassTreeModelSpoon = SpoonUnusedClassAnalyzer.buildUnusedClassTreeSpoon(currentProject);
            unusedClassTreeSpoon.setModel(unusedClassTreeModelSpoon);
        } catch (Exception e) {
            showError("Spoon AST分析失败: " + e.getMessage());
        }
    }
    
    /**
     * 刷新方法分析
     */
    private void refreshMethodAnalysis() {
        if (currentProject == null) return;
        
        try {
            DefaultTreeModel unusedMethodTreeModel = MethodModelHandler.buildUnusedMethodTree(currentProject);
            unusedMethodTree.setModel(unusedMethodTreeModel);
        } catch (Exception e) {
            showError("方法分析失败: " + e.getMessage());
        }
    }
    
    /**
     * 显示错误信息
     */
    private void showError(String errorMessage) {
        JOptionPane.showMessageDialog(null, errorMessage, "错误", JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * 格式化耗时显示
     */
    private String formatDuration(long durationMs) {
        if (durationMs < 1000) {
            return durationMs + "ms";
        } else if (durationMs < 60000) {
            return String.format("%.1fs", durationMs / 1000.0);
        } else {
            long minutes = durationMs / 60000;
            long seconds = (durationMs % 60000) / 1000;
            return minutes + "分" + seconds + "秒";
        }
    }
    
    /**
     * 初始化空的树模型
     */
    private void initializeEmptyTrees() {
        // 创建空的根节点
        javax.swing.tree.DefaultMutableTreeNode regexRoot = new javax.swing.tree.DefaultMutableTreeNode("正则表达式分析结果");
        javax.swing.tree.DefaultMutableTreeNode spoonRoot = new javax.swing.tree.DefaultMutableTreeNode("Spoon AST分析结果");
        javax.swing.tree.DefaultMutableTreeNode methodRoot = new javax.swing.tree.DefaultMutableTreeNode("无用方法分析结果");
        
        // 设置空的树模型
        unusedClassTreeRegex.setModel(new javax.swing.tree.DefaultTreeModel(regexRoot));
        unusedClassTreeSpoon.setModel(new javax.swing.tree.DefaultTreeModel(spoonRoot));
        unusedMethodTree.setModel(new javax.swing.tree.DefaultTreeModel(methodRoot));
        
        // 展开根节点
        unusedClassTreeRegex.expandRow(0);
        unusedClassTreeSpoon.expandRow(0);
        unusedMethodTree.expandRow(0);
    }
    
    /**
     * 为JTree添加导航监听器
     */
    private void addNavigationListener(JTree tree) {
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // 双击事件
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        Object lastPathComponent = path.getLastPathComponent();
                        if (lastPathComponent instanceof javax.swing.tree.DefaultMutableTreeNode) {
                            javax.swing.tree.DefaultMutableTreeNode node = (javax.swing.tree.DefaultMutableTreeNode) lastPathComponent;
                            Object userObject = node.getUserObject();
                            
                            // 尝试导航到代码位置
                            navigateToCode(userObject);
                        }
                    }
                }
            }
        });
    }
    
    /**
     * 导航到代码位置
     */
    private void navigateToCode(Object userObject) {
        if (currentProject == null) return;
        
        try {
            String className = null;
            String methodName = null;
            
            // 根据不同的对象类型提取类名和方法名
            if (userObject instanceof com.hello.bigclean.spoon.reference.Reference) {
                com.hello.bigclean.spoon.reference.Reference ref = (com.hello.bigclean.spoon.reference.Reference) userObject;
                className = ref.getOriginalClass();
            } else if (userObject instanceof com.hello.bigclean.spoon.reference.MethodReference) {
                com.hello.bigclean.spoon.reference.MethodReference methodRef = (com.hello.bigclean.spoon.reference.MethodReference) userObject;
                className = methodRef.getOriginalClass();
                methodName = methodRef.getMethod();
            } else if (userObject instanceof String) {
                String str = (String) userObject;
                // 尝试解析字符串中的类名和方法名
                if (str.contains("(") && str.contains(")")) {
                    // 格式: methodName(className)
                    int parenIndex = str.indexOf("(");
                    methodName = str.substring(0, parenIndex);
                    className = str.substring(parenIndex + 1, str.lastIndexOf(")"));
                } else {
                    className = str;
                }
            }
            
            if (className != null) {
                navigateToClass(className, methodName);
            }
            
        } catch (Exception e) {
            showError("导航失败: " + e.getMessage());
        }
    }
    
    /**
     * 导航到指定的类
     */
    private void navigateToClass(String className, String methodName) {
        try {
            // 将类名转换为文件路径
            String filePath = className.replace(".", "/") + ".java";
            String projectPath = currentProject.getBasePath();
            
            if (projectPath != null) {
                File javaFile = new File(projectPath + "/src/main/java/" + filePath);
                if (!javaFile.exists()) {
                    // 尝试其他可能的路径
                    javaFile = new File(projectPath + "/src/" + filePath);
                }
                if (!javaFile.exists()) {
                    // 尝试在项目根目录查找
                    javaFile = findJavaFileInProject(projectPath, className);
                }
                
                if (javaFile.exists()) {
                    VirtualFile virtualFile = VfsUtil.findFileByIoFile(javaFile, true);
                    if (virtualFile != null) {
                        // 打开文件
                        FileEditorManager fileEditorManager = FileEditorManager.getInstance(currentProject);
                        
                        if (methodName != null) {
                            // 如果有方法名，尝试定位到方法
                            int lineNumber = findMethodLineNumber(javaFile, methodName);
                            OpenFileDescriptor descriptor = new OpenFileDescriptor(currentProject, virtualFile, lineNumber);
                            fileEditorManager.openTextEditor(descriptor, true);
                        } else {
                            // 只打开文件
                            OpenFileDescriptor descriptor = new OpenFileDescriptor(currentProject, virtualFile);
                            fileEditorManager.openTextEditor(descriptor, true);
                        }
                    }
                } else {
                    showError("找不到文件: " + className);
                }
            }
        } catch (Exception e) {
            showError("导航到类失败: " + e.getMessage());
        }
    }
    
    /**
     * 在Java文件中查找方法的行号
     */
    private int findMethodLineNumber(File javaFile, String methodName) {
        try {
            java.nio.file.Path path = javaFile.toPath();
            java.util.List<String> lines = java.nio.file.Files.readAllLines(path);
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                // 查找方法定义，支持多种格式
                if (line.contains(methodName + "(") && 
                    (line.contains("public ") || line.contains("private ") || 
                     line.contains("protected ") || line.contains("static "))) {
                    return i; // 返回行号（从0开始，但IntelliJ使用1开始）
                }
            }
        } catch (Exception e) {
            System.err.println("查找方法行号失败: " + e.getMessage());
        }
        return 0; // 如果找不到方法，返回第1行
    }
    
    /**
     * 在项目中查找Java文件
     */
    private File findJavaFileInProject(String projectPath, String className) {
        File projectDir = new File(projectPath);
        String fileName = className.substring(className.lastIndexOf(".") + 1) + ".java";
        
        return findFileRecursively(projectDir, fileName);
    }
    
    /**
     * 递归查找文件
     */
    private File findFileRecursively(File dir, String fileName) {
        if (!dir.isDirectory()) return null;
        
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    File result = findFileRecursively(file, fileName);
                    if (result != null) return result;
                } else if (file.getName().equals(fileName)) {
                    return file;
                }
            }
        }
        return null;
    }
}
