//import org.springframework.ai.embedding.EmbeddingModel;
//import org.springframework.ai.vectorstore.SimpleVectorStore;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class VectorStoreConfig {
//
//    // 💡 注入配置好的 BGE-M3 模型，构建一个内存向量数据库
//    @Bean
//    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
//        // SimpleVectorStore 是缓存在内存里的，重启会丢失，最适合本地测试
//        return new SimpleVectorStore(embeddingModel);
//    }
//}