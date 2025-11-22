package cn.wqk.serverwebsocket.service;

import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.split.FileSplit;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class TrainService {

    public static void main(String[] args) throws Exception {
        // ================================
        // 一、基本参数
        // ================================
        int height = 64;
        int width = 64;
        int channels = 3;
        int batchSize = 32;
        int epochs = 30;
        long seed = 123;

        // 数据路径
        File trainData = new File("dataset/train");
        if (!trainData.exists()) {
            throw new IOException("❌ 数据路径不存在: " + trainData.getAbsolutePath());
        }

        // ================================
        // 二、准备数据
        // ================================
        ParentPathLabelGenerator labelMaker = new ParentPathLabelGenerator();
        FileSplit fileSplit = new FileSplit(trainData, NativeImageLoader.ALLOWED_FORMATS);
        ImageRecordReader recordReader = new ImageRecordReader(height, width, channels, labelMaker);
        recordReader.initialize(fileSplit);

        List<String> labels = recordReader.getLabels();
        int outputNum = labels.size();
        System.out.println("✅ 检测到类别: " + labels);

        // 保存标签顺序
        File labelFile = new File("model/model2/labels.txt");
        labelFile.getParentFile().mkdirs();
        Files.write(labelFile.toPath(), String.join("\n", labels).getBytes());

        DataSetIterator trainIter = new RecordReaderDataSetIterator(recordReader, batchSize, 1, outputNum);
        DataNormalization scaler = new ImagePreProcessingScaler(0, 1);
        scaler.fit(trainIter);
        trainIter.setPreProcessor(scaler);

        // ================================
        // 三、定义模型结构
        // ================================
        MultiLayerNetwork model = new MultiLayerNetwork(new NeuralNetConfiguration.Builder()
                .seed(seed)
                .updater(new Adam(1e-3))
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .list()
                .layer(new ConvolutionLayer.Builder(5, 5)
                        .nIn(channels).stride(1, 1).nOut(32).activation(Activation.RELU).build())
                .layer(new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                        .kernelSize(2, 2).stride(2, 2).build())
                .layer(new ConvolutionLayer.Builder(3, 3)
                        .stride(1, 1).nOut(64).activation(Activation.RELU).build())
                .layer(new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                        .kernelSize(2, 2).stride(2, 2).build())
                .layer(new DenseLayer.Builder().nOut(128).activation(Activation.RELU).build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .nOut(outputNum).activation(Activation.SOFTMAX).build())
                .setInputType(org.deeplearning4j.nn.conf.inputs.InputType.convolutional(height, width, channels))
                .build());

        model.init();
        model.setListeners(new ScoreIterationListener(10));

        // ================================
        // 四、训练模型 + 评估
        // ================================
        System.out.println("🚀 开始训练模型...");
        for (int i = 0; i < epochs; i++) {
            trainIter.reset();
            model.fit(trainIter);

            // 每轮后评估一次
            trainIter.reset();
            Evaluation eval = model.evaluate(trainIter);
            System.out.println("📊 第 " + (i + 1) + " 轮准确率: " + eval.accuracy());
            System.out.println(eval.stats());
        }

        // ================================
        // 五、保存模型
        // ================================
        File modelFile = new File("model/model2/garbage_classifier.zip");
        modelFile.getParentFile().mkdirs();
        ModelSerializer.writeModel(model, modelFile, true);
        System.out.println("💾 模型保存成功: " + modelFile.getAbsolutePath());

        // ================================
        // 六、单张图片预测示例
        // ================================
        System.out.println("🔍 预测示例: ");
        predictSingleImage("dataset/train/儿童玩具/img_8274.jpg", modelFile.getAbsolutePath(), labelFile.getAbsolutePath(), height, width);
    }

    /**
     * 单张图片预测
     */
    public static void predictSingleImage(String imagePath, String modelPath, String labelPath,
                                          int height, int width) throws Exception {
        // 加载模型
        MultiLayerNetwork model = ModelSerializer.restoreMultiLayerNetwork(modelPath);
        List<String> labels = Files.readAllLines(Paths.get(labelPath));

        // 加载图片
        NativeImageLoader loader = new NativeImageLoader(height, width, 3);
        INDArray image = loader.asMatrix(new File(imagePath));

        // 同样归一化
        DataNormalization scaler = new ImagePreProcessingScaler(0, 1);
        scaler.transform(image);

        // 预测
        INDArray output = model.output(image);
        int predicted = output.argMax(1).getInt(0);

        System.out.println("📸 图片路径: " + imagePath);
        System.out.println("🔹 预测类别: " + labels.get(predicted));
        System.out.println("🔹 输出概率: " + output);
    }
}
