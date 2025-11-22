package cn.wqk.serverwebsocket.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GarbageService {

    private OrtEnvironment env;
    private OrtSession session;
    private List<String> classNames;

    @PostConstruct
    public void init() throws Exception {
        // 1. 初始化 ONNX 环境
        env = OrtEnvironment.getEnvironment();

        // 2. 加载模型（放在 resources/models/resnet50.onnx）
        try (InputStream modelStream = getClass().getResourceAsStream("/models/resnet50.onnx")) {
            if (modelStream == null) {
                throw new FileNotFoundException("未找到模型文件 /models/resnet50.onnx");
            }
            Path tempModelPath = Files.createTempFile("model", ".onnx");
            Files.copy(modelStream, tempModelPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            session = env.createSession(tempModelPath.toString(), new OrtSession.SessionOptions());
        }

        // 3. 读取 ImageNet 类名（resources/data/imagenet_classes.txt）
        try (InputStream classStream = getClass().getResourceAsStream("/data/imagenet_classes.txt")) {
            if (classStream == null) {
                throw new FileNotFoundException("未找到 /data/imagenet_classes.txt");
            }
            classNames = Files.readAllLines(Paths.get("src/main/resources/data/imagenet_classes.txt"));
        }

        System.out.println("✅ 模型与类别文件加载成功，类别数: " + classNames.size());
    }

    public Map<String, Object> predict(MultipartFile file) throws Exception {
        // 1. 读取图片
        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            throw new IllegalArgumentException("上传文件不是有效图片");
        }

        // 2. 调整为模型输入大小 224x224
        BufferedImage resized = new BufferedImage(224, 224, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.drawImage(image, 0, 0, 224, 224, null);
        g.dispose();

        // 3. 构建输入张量（NCHW 格式）
        float[][][][] inputData = new float[1][224][224][3];
        for (int y = 0; y < 224; y++) {
            for (int x = 0; x < 224; x++) {
                int rgb = resized.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g1 = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                inputData[0][y][x][0] = r / 255.0f; // R
                inputData[0][y][x][1] = g1 / 255.0f; // G
                inputData[0][y][x][2] = b / 255.0f; // B
            }
        }


        // 4. 转成 ONNX Tensor
        long[] shape = new long[]{1, 3, 224, 224};
        FloatBuffer buffer = FloatBuffer.allocate(1 * 3 * 224 * 224);
        for (int c = 0; c < 3; c++) {
            for (int y = 0; y < 224; y++) {
                for (int x = 0; x < 224; x++) {
                    buffer.put(inputData[0][c][y][x]);
                }
            }
        }
        buffer.rewind();

        // 确定输入名（自动获取）
        String inputName = session.getInputInfo().keySet().iterator().next();

//        OnnxTensor tensor = OnnxTensor.createTensor(env, buffer, shape);
        OnnxTensor tensor = OnnxTensor.createTensor(env, inputData);


        // 5. 执行推理
        OrtSession.Result result = session.run(Collections.singletonMap(inputName, tensor));
        float[][] output = (float[][]) result.get(0).getValue();

        // 6. Softmax + 找出最大概率类别
        float[] probs = softmax(output[0]);
        int maxIdx = argmax(probs);
        String predictedClass = classNames.get(maxIdx);
        float confidence = probs[maxIdx];

        // 7. 返回结果
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("classIndex", maxIdx);
        map.put("className", predictedClass);
        map.put("probability", confidence);

        System.out.println("predict = " + map);
        return map;
    }

    private float[] softmax(float[] logits) {
        // 1. 先找最大值
        float max = Float.NEGATIVE_INFINITY;
        for (float v : logits) {
            if (v > max) {
                max = v;
            }
        }

        // 2. 计算 exp(v - max)
        double sum = 0.0;
        double[] exps = new double[logits.length];
        for (int i = 0; i < logits.length; i++) {
            exps[i] = Math.exp(logits[i] - max);
            sum += exps[i];
        }

        // 3. 归一化
        float[] probs = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            probs[i] = (float) (exps[i] / sum);
        }

        return probs;
    }

    private int argmax(float[] array) {
        int maxIdx = 0;
        float maxVal = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxVal) {
                maxVal = array[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }

}
