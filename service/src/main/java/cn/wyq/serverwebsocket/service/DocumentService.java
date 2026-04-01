package cn.wyq.serverwebsocket.service;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {
    /**
     * 上传并解析文档存入向量库
     * @param file 前端传来的文件
     * @return 返回生成的文档 ID
     */
    String uploadAndParse(MultipartFile file);
}