package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.pojo.entity.SysDocument;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

/**
 * DocumentService：业务服务接口，定义对应领域的业务操作契约。
 */
public interface DocumentService extends IService<SysDocument> {
    /**
     * 上传并解析文档存入向量库
     * @param file 前端传来的文件
     * @return 返回生成的文档 ID
     */
    String uploadAndParse(MultipartFile file);

    void deleteDocument(String docId);
}