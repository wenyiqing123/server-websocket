package cn.wyq.serverwebsocket.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorStoreConfig {

    /**
     * 手动注册 VectorStore Bean，彻底解决找不到 Bean 的报错
     * @param embeddingModel Spring 会自动把硅基流动的向量模型注入进来
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        // 🚀 降维打击：直接使用 Spring AI 内置的内存向量库！
        // M8 版本推荐使用 builder 语法
        return SimpleVectorStore.builder(embeddingModel).build();

        // 💡 备用方案：如果上面这行在你的 IDEA 里报红提示找不到 builder()，
        // 请注释掉上面那行，改用老语法： return new SimpleVectorStore(embeddingModel);
    }
}