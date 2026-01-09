package cn.wyq.serverwebsocket.controller;

import ai.djl.modality.Classifications;
import ai.djl.translate.TranslateException;
import cn.wyq.serverwebsocket.service.GarbageClassifierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/classify")
@Tag(name = "垃圾分类服务", description = "根据图片分类垃圾分类")
@RequiredArgsConstructor
public class GarbageController {

//    @Autowired
    private final GarbageClassifierService garbageClassifierService;

    // POST 请求，接收文件上传
    // ⭐️ 核心：使用 @Autowired 注入 Service 实例
//    @Autowired
    private final GarbageClassifierService classifierService; // 实例名称与 Service 类名对应

    // POST 请求，接收文件上传
    @PostMapping(consumes = {"multipart/form-data"})
    @Operation(summary = "图片分类",
            description = "根据上传的图片文件，返回分类结果")
    public ResponseEntity<?> predictImage(@RequestParam("file") MultipartFile file) {
        String localImagePath = "dataset/train/儿童玩具/img_8270.jpg";
        File fileToTest = new File(localImagePath);
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("上传文件不能为空。");
        }

        try {
            byte[] bytes = file.getBytes();
            Path path = fileToTest.toPath();
            byte[] imageBytes = Files.readAllBytes(path);


            // ⭐️ 调用 Service 中的 predict 方法
            Classifications classifications = classifierService.predict(bytes);

            // 格式化输出 (只显示 Top 5 结果)
            List<String> results = classifications.topK(5).stream()
                    .map(item -> String.format("%s: %.2f%%", item.getClassName(), item.getProbability() * 100))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(results);

        } catch (IOException | TranslateException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("模型推理失败：" + e.getMessage());
        }
    }
}