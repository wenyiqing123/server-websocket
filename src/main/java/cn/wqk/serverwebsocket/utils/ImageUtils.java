package cn.wqk.serverwebsocket.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {

    private static final float[] MEAN = {0.485f, 0.456f, 0.406f};
    private static final float[] STD = {0.229f, 0.224f, 0.225f};

    // 读取并缩放图片到 224x224
    public static BufferedImage resize(InputStream inputStream, int width, int height) throws IOException {
        BufferedImage originalImage = ImageIO.read(inputStream);
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, width, height, null);
        g.dispose();
        return resizedImage;
    }

    // 转换成 NCHW 格式 float[1][3][224][224]
    public static float[][][][] toNHWCFloatArray(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        float[][][][] data = new float[1][3][height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                data[0][0][y][x] = (r / 255.0f - MEAN[0]) / STD[0];
                data[0][1][y][x] = (g / 255.0f - MEAN[1]) / STD[1];
                data[0][2][y][x] = (b / 255.0f - MEAN[2]) / STD[2];
            }
        }
        return data;
    }
}
