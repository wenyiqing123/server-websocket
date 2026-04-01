package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.mapper.SysDocumentMapper;
import cn.wyq.serverwebsocket.pojo.entity.SysDocument;
import cn.wyq.serverwebsocket.service.DocumentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentServiceImpl extends ServiceImpl<SysDocumentMapper, SysDocument> implements DocumentService {

    @Autowired
    private VectorStore vectorStore;

    // 从配置文件读取上传目录，如果没有配置则默认存放在项目根目录下的 uploads 文件夹
    @Value("${app.upload.dir:${user.dir}/uploads/}")
    private String uploadDir;

    @Override
    @Transactional(rollbackFor = Exception.class) // 开启事务，保证 MySQL 和 文件状态一致
    public String uploadAndParse(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }

        // 1. 生成唯一文档 ID 和本地保存路径
        String docId = UUID.randomUUID().toString().replace("-", "");
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFileName = docId + fileExtension;

        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs(); // 确保目录存在
        }

        File destFile = new File(dir, newFileName);

        try {
            // 2. 将文件物理保存到服务器本地磁盘
            file.transferTo(destFile);

            // 3. 将文档信息存入 MySQL 数据库
            SysDocument sysDocument = new SysDocument();
            sysDocument.setId(docId);
            sysDocument.setFileName(originalFilename);
            sysDocument.setFilePath(destFile.getAbsolutePath());
            sysDocument.setFileSize(file.getSize());
            this.save(sysDocument);

            // 4. 调用 Spring AI 读取 PDF 内容
            // 注意：这里以 PDF 为例，如果是 TXT 需要换成 TextReader
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(destFile.getAbsolutePath());
            List<Document> documents = pdfReader.get();

            // 5. 将长文本进行切片 (RAG 的关键步骤)
            TokenTextSplitter textSplitter = new TokenTextSplitter(
                    800,  // Chunk 大小：800 Token
                    400,  // 重叠大小：400 Token (保证上下文连贯)
                    5,
                    10000,
                    true
            );
            List<Document> splitDocuments = textSplitter.apply(documents);

            // 6. 为每个切片打上 Metadata 标签，方便后续精准检索
            for (Document doc : splitDocuments) {
                doc.getMetadata().put("docId", docId);
                doc.getMetadata().put("fileName", originalFilename);
            }

            // 7. 灌入 Redis 向量数据库 (会自动调用 Embedding 模型转化为向量)
            vectorStore.accept(splitDocuments);

            return docId;

        } catch (IOException e) {
            throw new RuntimeException("文件处理失败: " + e.getMessage(), e);
        }
    }
}