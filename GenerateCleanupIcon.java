import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class GenerateCleanupIcon {
    public static void main(String[] args) {
        // 生成不同尺寸的清理图标
        generateIcon(16, "src/main/resources/icons/pluginIcon_16.png");
        generateIcon(32, "src/main/resources/icons/pluginIcon_32.png");
        generateIcon(40, "src/main/resources/icons/pluginIcon_40.png");
        generateIcon(64, "src/main/resources/icons/pluginIcon_64.png");
        generateIcon(128, "src/main/resources/icons/pluginIcon_128.png");
        generateIcon(40, "src/main/resources/icons/pluginIcon.png"); // 默认尺寸
    }
    
    private static void generateIcon(int size, String filename) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        // 启用抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // 计算缩放比例
        float scale = size / 40.0f;
        
        // 背景圆形
        g2d.setColor(new Color(255, 107, 53)); // #FF6B35
        g2d.fillOval((int)(2 * scale), (int)(2 * scale), (int)(36 * scale), (int)(36 * scale));
        
        g2d.setColor(new Color(229, 90, 43)); // #E55A2B
        g2d.setStroke(new BasicStroke(2 * scale));
        g2d.drawOval((int)(2 * scale), (int)(2 * scale), (int)(36 * scale), (int)(36 * scale));
        
        // 代码文件图标 - 更大更明显
        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect((int)(6 * scale), (int)(4 * scale), (int)(20 * scale), (int)(24 * scale), 
                         (int)(2 * scale), (int)(2 * scale));
        
        g2d.setColor(new Color(255, 107, 53));
        g2d.setStroke(new BasicStroke(2 * scale));
        g2d.drawRoundRect((int)(6 * scale), (int)(4 * scale), (int)(20 * scale), (int)(24 * scale), 
                         (int)(2 * scale), (int)(2 * scale));
        
        // 代码文件标签
        g2d.setColor(new Color(255, 107, 53));
        g2d.fillRoundRect((int)(6 * scale), (int)(4 * scale), (int)(20 * scale), (int)(4 * scale), 
                         (int)(2 * scale), (int)(2 * scale));
        
        // 设置字体用于代码文本
        Font codeFont = new Font("Monospaced", Font.PLAIN, (int)(1.8f * scale));
        g2d.setFont(codeFont);
        
        // 实际代码内容
        g2d.setColor(new Color(255, 107, 53));
        g2d.drawString("public class", (int)(8 * scale), (int)(11 * scale));
        g2d.drawString("UnusedClass {", (int)(8 * scale), (int)(13 * scale));
        g2d.drawString("  public void", (int)(10 * scale), (int)(16 * scale));
        g2d.drawString("  oldMethod() {", (int)(10 * scale), (int)(18 * scale));
        
        // 无用代码 - 红色
        g2d.setColor(Color.RED);
        g2d.drawString("    // unused", (int)(12 * scale), (int)(21 * scale));
        g2d.drawString("    return;", (int)(12 * scale), (int)(23 * scale));
        
        // 结束
        g2d.setColor(new Color(255, 107, 53));
        g2d.drawString("  }", (int)(10 * scale), (int)(26 * scale));
        g2d.drawString("}", (int)(8 * scale), (int)(28 * scale));
        
        // 大叉叉 - 覆盖整个代码文件
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(3 * scale));
        g2d.drawLine((int)(7 * scale), (int)(5 * scale), (int)(25 * scale), (int)(27 * scale));
        g2d.drawLine((int)(25 * scale), (int)(5 * scale), (int)(7 * scale), (int)(27 * scale));
        
        // 垃圾桶主体 - 使用经典的垃圾桶颜色（深绿色）
        g2d.setColor(new Color(46, 125, 50)); // #2E7D32
        g2d.fillRoundRect((int)(24 * scale), (int)(18 * scale), (int)(10 * scale), (int)(12 * scale), 
                         (int)(1 * scale), (int)(1 * scale));
        
        g2d.setColor(new Color(27, 94, 32)); // #1B5E20
        g2d.setStroke(new BasicStroke(2 * scale));
        g2d.drawRoundRect((int)(24 * scale), (int)(18 * scale), (int)(10 * scale), (int)(12 * scale), 
                         (int)(1 * scale), (int)(1 * scale));
        
        // 垃圾桶盖子 - 深绿色
        g2d.setColor(new Color(56, 142, 60)); // #388E3C
        g2d.fillRoundRect((int)(22 * scale), (int)(16 * scale), (int)(14 * scale), (int)(4 * scale), 
                         (int)(1 * scale), (int)(1 * scale));
        
        g2d.setColor(new Color(27, 94, 32)); // #1B5E20
        g2d.drawRoundRect((int)(22 * scale), (int)(16 * scale), (int)(14 * scale), (int)(4 * scale), 
                         (int)(1 * scale), (int)(1 * scale));
        
        // 垃圾桶盖子把手 - 金属银色
        g2d.setColor(new Color(144, 164, 174)); // #90A4AE
        g2d.fillRoundRect((int)(26 * scale), (int)(14 * scale), (int)(6 * scale), (int)(3 * scale), 
                         (int)(1 * scale), (int)(1 * scale));
        
        g2d.setColor(new Color(84, 110, 122)); // #546E7A
        g2d.setStroke(new BasicStroke(1 * scale));
        g2d.drawRoundRect((int)(26 * scale), (int)(14 * scale), (int)(6 * scale), (int)(3 * scale), 
                         (int)(1 * scale), (int)(1 * scale));
        
        // 垃圾桶盖子把手细节
        g2d.setColor(new Color(236, 239, 241)); // #ECEFF1
        g2d.fillRoundRect((int)(27 * scale), (int)(13 * scale), (int)(4 * scale), (int)(2 * scale), 
                         (int)(0.5f * scale), (int)(0.5f * scale));
        
        // 垃圾桶上的"TRASH"标签
        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect((int)(25 * scale), (int)(19 * scale), (int)(8 * scale), (int)(3 * scale), 
                         (int)(0.5f * scale), (int)(0.5f * scale));
        
        g2d.setColor(new Color(46, 125, 50)); // #2E7D32
        Font trashFont = new Font("Arial", Font.BOLD, (int)(1.5f * scale));
        g2d.setFont(trashFont);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth("TRASH");
        g2d.drawString("TRASH", (int)(29 * scale - textWidth/2), (int)(21f * scale));
        
        // 垃圾桶内部线条 - 白色更清晰
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1.5f * scale));
        g2d.drawLine((int)(26 * scale), (int)(24 * scale), (int)(32 * scale), (int)(24 * scale));
        g2d.drawLine((int)(26 * scale), (int)(27 * scale), (int)(32 * scale), (int)(27 * scale));
        g2d.drawLine((int)(26 * scale), (int)(30 * scale), (int)(32 * scale), (int)(30 * scale));
        
        // 垃圾桶底部阴影
        g2d.setColor(new Color(27, 94, 32)); // #1B5E20
        g2d.fillRoundRect((int)(24 * scale), (int)(28 * scale), (int)(10 * scale), (int)(2 * scale), 
                         (int)(1 * scale), (int)(1 * scale));
        
        // 清理动作箭头 - 更明显
        g2d.setColor(Color.WHITE);
        int[] arrowX = {(int)(18 * scale), (int)(22 * scale), (int)(20 * scale), (int)(20 * scale), 
                       (int)(24 * scale), (int)(24 * scale), (int)(22 * scale), (int)(26 * scale)};
        int[] arrowY = {(int)(32 * scale), (int)(28 * scale), (int)(28 * scale), (int)(26 * scale), 
                       (int)(26 * scale), (int)(28 * scale), (int)(28 * scale), (int)(32 * scale)};
        g2d.fillPolygon(arrowX, arrowY, 8);
        
        // 垃圾桶侧面的高光效果
        g2d.setColor(new Color(76, 175, 80)); // #4CAF50
        g2d.fillRoundRect((int)(24 * scale), (int)(18 * scale), (int)(2 * scale), (int)(12 * scale), 
                         (int)(1 * scale), (int)(1 * scale));
        
        g2d.dispose();
        
        try {
            File outputFile = new File(filename);
            outputFile.getParentFile().mkdirs();
            ImageIO.write(image, "PNG", outputFile);
            System.out.println("Generated: " + filename);
        } catch (IOException e) {
            System.err.println("Error generating " + filename + ": " + e.getMessage());
        }
    }
}