package com.hello.bigclean.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CodeUtils {
    /**
     * 从文件路径推导出类的全限定名
     * @param filePath 文件路径
     * @param rootDir 项目根目录
     * @param className 类名
     * @return 类的全限定名
     */
    public static String deriveFullClassName(Path filePath, String rootDir, String className) {
        try {
            // 获取相对于项目根目录的路径
            Path rootPath = Paths.get(rootDir);
            Path relativePath = rootPath.relativize(filePath);


            // 将路径分隔符转换为包分隔符
            String packagePath = relativePath.getParent() != null ?
                    relativePath.getParent().toString().replace('/', '.') : "";

            // 移除.java扩展名
            String fileName = relativePath.getFileName().toString();
            if (fileName.endsWith(".java")) {
                fileName = fileName.substring(0, fileName.length() - 5);
            }

            // 构建全限定名
            if (packagePath.isEmpty()) {
                return fileName;
            } else {
                return packagePath + "." + fileName;
            }

        } catch (Exception e) {
            // 如果推导失败，返回原始类名
            return className;
        }
    }

}
