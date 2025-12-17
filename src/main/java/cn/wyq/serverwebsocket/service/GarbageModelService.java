package cn.wyq.serverwebsocket.service;

import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class GarbageModelService {

    private static final String MODEL_PATH = "model/model2/garbage_classifier.zip";
    private static final String LABEL_PATH = "model/model2/labels.txt";
    private static final int HEIGHT = 64;
    private static final int WIDTH = 64;

    public static void main(String[] args) throws Exception {
        // ✅ 单张图片测试
        predictSingleImage("dataset/儿童玩具/img_8565.jpg");

        // ✅ 批量测试整个文件夹
//        testFolder("dataset/train/");
    }

    /**
     * 预测单张图片
     */
    public static void predictSingleImage(String imagePath) throws Exception {
        MultiLayerNetwork model = ModelSerializer.restoreMultiLayerNetwork(MODEL_PATH);
        List<String> labels = Files.readAllLines(Paths.get(LABEL_PATH));

        NativeImageLoader loader = new NativeImageLoader(HEIGHT, WIDTH, 3);
        INDArray image = loader.asMatrix(new File(imagePath));

        DataNormalization scaler = new ImagePreProcessingScaler(0, 1);
        scaler.transform(image);

        INDArray output = model.output(image);
        int predicted = output.argMax(1).getInt(0);

        System.out.println("📸 图片: " + imagePath);
        System.out.println("🔹 预测类别: " + labels.get(predicted));
        System.out.println("🔹 输出概率: " + output);
    }

    /**
     * 批量测试文件夹中的所有图片
     */
    public static void testFolder(String folderPath) throws Exception {
        MultiLayerNetwork model = ModelSerializer.restoreMultiLayerNetwork(MODEL_PATH);
        List<String> labels = Files.readAllLines(Paths.get(LABEL_PATH));
        NativeImageLoader loader = new NativeImageLoader(HEIGHT, WIDTH, 3);
        DataNormalization scaler = new ImagePreProcessingScaler(0, 1);

        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".jpg") ||
                        name.toLowerCase().endsWith(".png"));

        if (files == null || files.length == 0) {
            System.out.println("⚠️ 没有找到图片: " + folderPath);
            return;
        }

        System.out.println("📂 共检测到 " + files.length + " 张图片");
        for (File file : files) {
            INDArray image = loader.asMatrix(file);
            scaler.transform(image);
            INDArray output = model.output(image);
            int predicted = output.argMax(1).getInt(0);
            System.out.println(file.getName() + " → " + labels.get(predicted));
        }
    }
}
