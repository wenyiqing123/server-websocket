package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.framework.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
@RestController
@RequestMapping("/file")
@CrossOrigin


@Slf4j
@Tag(name = "文件上传接口")
public class UploadController {

    @Value("${image.mapping}")
    private String mapping;

    @Value("${image.location}")
    private String location;

    @Value("${server.port}")
    private String serverPort;

    @PostMapping("/upload")
    @Operation(summary = "文件上传",
            description = "上传文件到服务器，返回文件访问URL")
    public Result upload(MultipartFile file) {
        String filename = file.getOriginalFilename();
        String suffix = filename.substring(filename.lastIndexOf("."));
        String replace = UUID.randomUUID().toString().replace("-", "");
        String fileNewName = replace + "." + suffix;
        try {
            file.transferTo(new File(location, fileNewName));
        } catch (IOException e) {
            return Result.error("上传失败");
        }
        return Result.success("http://localhost:" + serverPort + mapping + fileNewName);
    }
}
