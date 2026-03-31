package cn.wyq.serverwebsocket.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class KnowledgeDataLoader {

    private final VectorStore vectorStore;

    @Value("classpath:static/dataset/kg.txt")
    private Resource kgResource;

    public KnowledgeDataLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    // 项目启动后自动加载数据
    // 项目启动后自动加载数据
    @PostConstruct
    public void init() {
        // 🚀 核心修改 1：指定绝对的相对路径，指向源码目录
// 🚀 核心修改：在最前面加上 web/ 模块名前缀
        String cacheFilePath = "web/src/main/resources/static/dataset/mecha_vector_cache.json";
        java.io.File vectorFile = new java.io.File(cacheFilePath);
        log.info("🔍 准备操作缓存文件，它的真实物理路径是: {}", vectorFile.getAbsolutePath());
        // 🚀 核心修改 2：确保父级目录存在，如果不存在则自动创建
        if (!vectorFile.getParentFile().exists()) {
            vectorFile.getParentFile().mkdirs();
            log.info("📁 自动创建了目录: {}", vectorFile.getParentFile().getAbsolutePath());
        }

        org.springframework.ai.vectorstore.SimpleVectorStore simpleVectorStore =
                (org.springframework.ai.vectorstore.SimpleVectorStore) this.vectorStore;

        // 检查是否存在缓存文件
        if (vectorFile.exists()) {
            log.info("⚡ 发现本地机甲知识库缓存文件，正在从 {} 光速加载...", cacheFilePath);
            simpleVectorStore.load(vectorFile);
            log.info("✅ 缓存加载完毕！启动耗时缩短 99%！");
            return;
        }

        log.info("🚀 未发现缓存，正在读取 kg.txt 并调用大模型 API 生成向量...");

        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(kgResource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {

            List<org.springframework.ai.document.Document> documents = reader.lines()
                    .filter(line -> line.contains(","))
                    .map(line -> {
                        String cleanLine = line.replaceAll("[\\[\\]']", "");
                        String[] parts = cleanLine.split(",");
                        String content = String.format("%s的%s是%s", parts[0].trim(), parts[1].trim(), parts[2].trim());
                        return new org.springframework.ai.document.Document(content, java.util.Map.of("subject", parts[0].trim()));
                    })
                    .collect(java.util.stream.Collectors.toList());

            if (!documents.isEmpty()) {
                simpleVectorStore.add(documents);
                // 保存快照到指定的 static/dataset 目录下
                simpleVectorStore.save(vectorFile);
                log.info("✅ 成功生成向量并保存快照至 {}！", cacheFilePath);
            }

        } catch (Exception e) {
            log.error("❌ 初始化机甲知识库失败！", e);
        }
    }
}