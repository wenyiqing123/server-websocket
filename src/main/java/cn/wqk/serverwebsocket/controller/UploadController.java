package cn.wqk.serverwebsocket.controller;

import cn.wqk.serverwebsocket.framework.common.Result;
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
public class UploadController {

    @Value("${image.mapping}")
    private String mapping;

    @Value("${image.location}")
    private String location;

    @Value("${server.port}")
    private String serverPort;

    @PostMapping("/upload")
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
