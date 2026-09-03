package cn.wyq.serverwebsocket.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * VectorStoreConfig：Spring 配置类，负责注册和配置应用组件。
 */
@Configuration
public class VectorStoreConfig {

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        // 🌟 核心修复：M8 版本必须使用 builder 模式来构建
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}