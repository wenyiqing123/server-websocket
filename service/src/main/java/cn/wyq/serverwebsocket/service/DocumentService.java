package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.pojo.entity.SysDocument;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * DocumentService：业务服务接口，定义对应领域的业务操作契约。
 */
public interface DocumentService extends IService<SysDocument> {
    /**
     * 上传并解析当前用户的文档，再写入向量库。
     *
     * @param file 前端传来的文件
     * @param ownerUserId 当前登录用户编号
     * @return 返回生成的文档 ID
     */
    String uploadAndParse(MultipartFile file, Integer ownerUserId);

    /** 查询当前用户可见的文档；没有可信归属的历史文档不会返回。 */
    List<SysDocument> listByOwner(Integer ownerUserId);

    /** 仅删除当前用户自己上传的文档。 */
    void deleteDocument(String docId, Integer ownerUserId);

    /**
     * 读取当前用户拥有的私有文档，供受鉴权保护的下载接口使用。
     *
     * @param docId 文档编号
     * @param ownerUserId 当前登录用户编号
     * @return 文档内容及下载响应需要的元数据
     */
    DownloadableDocument loadOwnedDocument(String docId, Integer ownerUserId);

    /** 受控下载使用的文档内容与元数据。 */
    record DownloadableDocument(Resource resource, String fileName, long fileSize) {
    }
}
