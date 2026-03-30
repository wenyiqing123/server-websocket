//import jakarta.annotation.PostConstruct;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Map;
//
//@Service
//public class KnowledgeBaseService {
//
//    @Autowired
//    private VectorStore vectorStore;
//
//    // 项目启动时，自动往向量库里塞点知识
//    @PostConstruct
//    public void initKnowledge() {
//        // 💡 Document 是 Spring AI 的标准文档类，包含文本内容和元数据(Metadata)
//        List<Document> documents = List.of(
//                new Document("云聊小管家的底层技术栈是 Java、Spring Boot 和 Vue2，数据库使用了 MySQL 和 Redis。", Map.of("type", "系统架构")),
//                new Document("系统的开发者是文艺清，目前就读于潍坊科技学院的计算机科学与技术专业大三。", Map.of("type", "人员信息")),
//                new Document("遇到前端跨域问题，可以在 Spring Boot 的配置类中添加 CorsFilter 来解决。", Map.of("type", "技术问答"))
//        );
//
//        // 这行代码会自动调用云端的 BGE-M3 把这三句话转成向量，并存入内存库！
//        vectorStore.add(documents);
//        System.out.println("✅ 专属知识库初始化完毕！");
//    }
//}