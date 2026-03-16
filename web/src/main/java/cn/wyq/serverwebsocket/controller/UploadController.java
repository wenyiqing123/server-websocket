package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/file")
@Slf4j
@Tag(name = "文件上传接口", description = "处理文件/图片上传")
public class UploadController {

    @Value("${image.mapping}")
    private String mapping;

    @Value("${image.location}")
    private String location;

    @Value("${server.port}")
    private String serverPort;

    @PostMapping("/upload")
    @Operation(summary = "文件上传", description = "上传文件到服务器，返回文件访问URL")
    public Result upload(MultipartFile file) {
        // 1. 文件判空拦截
        if (file == null || file.isEmpty()) {
            log.warn("请求上传文件被拦截：上传的文件为空");
            return Result.error("上传文件不能为空");
        }

        String filename = file.getOriginalFilename();
        log.info("请求上传文件，原文件名: {}, 文件大小: {} bytes", filename, file.getSize());

        // 2. 安全截取后缀名（防止某些文件没有后缀名导致报错）
        String suffix = "";
        if (filename != null && filename.contains(".")) {
            suffix = filename.substring(filename.lastIndexOf("."));
        }

        String replace = UUID.randomUUID().toString().replace("-", "");
        String fileNewName = replace + suffix;

        try {
            // 3. 检查目标文件夹是否存在，不存在则自动创建
            File targetDir = new File(location);
            if (!targetDir.exists()) {
                boolean created = targetDir.mkdirs();
                log.info("本地存储目录不存在，已自动创建: {}, 结果: {}", targetDir.getAbsolutePath(), created);
            }

            // 4. 保存文件
            File targetFile = new File(location, fileNewName);
            file.transferTo(targetFile);

            // 5. 拼接返回路径
            String fileUrl = "http://localhost:" + serverPort + mapping + fileNewName;
            log.info("文件上传成功，已保存至硬盘: {}, 生成访问路径: {}", targetFile.getAbsolutePath(), fileUrl);

            return Result.success(fileUrl);

        } catch (IOException e) {
            log.error("文件上传失败，原文件名: {}", filename, e);
            return Result.error("文件上传失败");
        }
    }
}