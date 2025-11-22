//package cn.wqk.serverwebsocket.controller;
//
//import cn.wqk.serverwebsocket.service.GarbageModelService;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.Map;
//
//@RestController
//@CrossOrigin
//@RequestMapping("/garbage")
//public class GarbageController {
//
//    private final GarbageModelService garbageModelService;
//
//    // 构造器注入 PredictService
//    public GarbageController(@Value("${model.path:model/garbage_classifier.zip}") String modelPath) throws Exception {
////        this.garbageModelService = new GarbageModelService(modelPath);
//    }
//
//    /**
//     * 上传图片预测垃圾类别
//     * @param file 前端上传图片
//     * @return Map 包含 name(具体垃圾名称) 和 bigCategory(大类)
//     */
//    @PostMapping("/predict")
//    public Map<String, String> predict(@RequestParam("file") MultipartFile file) throws IOException {
//        // 保存上传的临时文件
//        File tempFile = File.createTempFile("upload_", ".jpg");
//        file.transferTo(tempFile);
//
//        try {
//            // 调用预测服务
////            return garbageModelService.predict(tempFile);
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new RuntimeException("预测失败: " + e.getMessage());
//        } finally {
//            // 删除临时文件
//            tempFile.delete();
//        }
//    }
//}
