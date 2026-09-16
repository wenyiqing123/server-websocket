package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.service.SysConfigService;
import cn.wyq.serverwebsocket.utils.SystemConfigUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * UploadController：Web 层控制器，负责接收并处理 HTTP 请求。
 */
@RestController
@RequestMapping("/file")
@Slf4j
@Tag(name = "文件上传接口", description = "处理文件/图片上传")
public class UploadController {

    @Value("${image.mapping}")
    private String mapping;

    @Value("${image.location}")
    private String location;

    /** 对外访问地址，生产环境通过 APP_DOMAIN 配置，避免把 localhost 写入数据库。 */
    @Value("${app.domain:}")
    private String appDomain;

    @Autowired
    private SysConfigService sysConfigService;

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

        long maxUploadBytes = configuredMaxUploadBytes();
        if (file.getSize() > maxUploadBytes) {
            log.warn("请求上传文件超过系统限制，文件大小: {} bytes，限制: {} bytes", file.getSize(), maxUploadBytes);
            return Result.error("文件大小超出限制，最大允许 " + formatBytes(maxUploadBytes));
        }

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
            String fileUrl = buildFileUrl(fileNewName);
            log.info("文件上传成功，已保存至硬盘: {}, 生成访问路径: {}", targetFile.getAbsolutePath(), fileUrl);

            return Result.success(fileUrl);

        } catch (IOException e) {
            log.error("文件上传失败，原文件名: {}", filename, e);
            return Result.error("文件上传失败");
        }
    }

    private String buildFileUrl(String fileName) {
        String path = (mapping == null ? "/file/" : mapping.trim());
        if (path.isEmpty()) path = "/file/";
        if (!path.startsWith("/")) path = "/" + path;
        if (!path.endsWith("/")) path += "/";
        String domain = appDomain == null ? "" : appDomain.trim();
        if (domain.isEmpty()) return path + fileName;
        while (domain.endsWith("/")) domain = domain.substring(0, domain.length() - 1);
        return domain + path + fileName;
    }

    /** 读取平台配置中的普通文件大小上限，配置不可用时回退到 10 MB。 */
    private long configuredMaxUploadBytes() {
        try {
            return SystemConfigUtils.longValue(sysConfigService.getConfigMap(),
                    "file_max_upload_bytes", 10L * 1024 * 1024,
                    1_024L, 1_073_741_824L);
        } catch (RuntimeException exception) {
            log.warn("读取普通文件大小配置失败，将使用默认 10 MB", exception);
            return 10L * 1024 * 1024;
        }
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1024L * 1024 * 1024) {
            return (bytes / (1024L * 1024 * 1024)) + "GB";
        }
        if (bytes >= 1024L * 1024) {
            return (bytes / (1024L * 1024)) + "MB";
        }
        if (bytes >= 1024L) {
            return (bytes / 1024L) + "KB";
        }
        return bytes + "B";
    }
}
