package cn.wqk.serverwebsocket.service;

import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service
public class GarbageClassifierService {

    private Predictor<Image, Classifications> predictor;
    private ZooModel<Image, Classifications> model;

    // 模型加载和初始化 (在 Spring Boot 启动时执行)
    @PostConstruct
    public void init() throws ModelException, IOException {

        // 1. ⭐️ 修复点：读取 classes.txt 文件，获取类别列表 (Synset)
        List<String> synset = Files.readAllLines(Paths.get("src/main/resources/model/classes.txt"));

        String modelName = "garbage_model";

        // 2. 定义 Criteria: 指定加载哪个模型和如何加载
        Criteria<Image, Classifications> criteria = Criteria.builder()
                .setTypes(Image.class, Classifications.class)
                .optEngine("PyTorch") // 指定引擎为 PyTorch
                .optModelPath(Paths.get("src/main/resources/model")) // 模型文件所在的目录
                .optModelName(modelName) // 模型文件的主名 (不带 .pt)
                // 3. ⭐️ 修复点：传入第二个参数 synset
                .optTranslator(new cn.wqk.serverwebsocket.utils.ClassifierTranslator(224, synset))
                .optOption("mapLocation", "cuda") // 强制在 CPU 上运行 (推荐用于后端服务)
                .build();

        // 4. 从 ModelZoo 加载模型
        model = ModelZoo.loadModel(criteria);

        // 5. 创建 Predictor
        predictor = model.newPredictor();
        System.out.println("✅ DJL PyTorch Model Loaded Successfully.");


        // 3. 创建 Predictor

        predictor = model.newPredictor();

        System.out.println("✅ DJL PyTorch Model Loaded Successfully.");

    }


// 执行推理

    public Classifications predict(byte[] imageBytes) throws IOException, TranslateException {

// 从字节数组创建 DJL Image 对象

        Image image = ImageFactory.getInstance().fromInputStream(new java.io.ByteArrayInputStream(imageBytes));


// 执行预测

        return predictor.predict(image);

    }


// 资源清理 (在 Spring Boot 关闭时执行)

    @PreDestroy

    public void cleanup() {

        if (predictor != null) {

            predictor.close();

        }

        if (model != null) {

            model.close();

        }

    }
}