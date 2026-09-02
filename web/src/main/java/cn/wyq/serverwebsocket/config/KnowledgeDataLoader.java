package cn.wyq.serverwebsocket.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@Slf4j
public class KnowledgeDataLoader {

    private static final String CACHE_FILE_PATH = "web/src/main/resources/static/dataset/mecha_vector_cache.json";
    private static final String ENGINEERING_KNOWLEDGE_BASE = "engineering";
    private static final Pattern ENGINEERING_METADATA_PATTERN =
            Pattern.compile("\\\"knowledgeBase\\\"\\s*:\\s*\\\"engineering\\\"");

    private final VectorStore vectorStore;

    @Value("classpath:static/dataset/kg.txt")
    private Resource kgResource;

    public KnowledgeDataLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void init() {
        java.io.File vectorFile = new java.io.File(CACHE_FILE_PATH);
        log.info("🔍 准备操作缓存文件，它的真实物理路径是: {}", vectorFile.getAbsolutePath());
        if (!vectorFile.getParentFile().exists()) {
            vectorFile.getParentFile().mkdirs();
            log.info("📁 自动创建了目录: {}", vectorFile.getParentFile().getAbsolutePath());
        }

        org.springframework.ai.vectorstore.SimpleVectorStore simpleVectorStore =
                (org.springframework.ai.vectorstore.SimpleVectorStore) this.vectorStore;

        if (vectorFile.exists()) {
            log.info("⚡ 发现本地机甲知识库缓存文件，正在从 {} 光速加载...", CACHE_FILE_PATH);
            simpleVectorStore.load(vectorFile);
            log.info("✅ 缓存加载完毕！");
            if (hasEngineeringMetadata(vectorFile)) {
                return;
            }
            log.warn("⚠️ 检测到旧版知识库缓存，正在补建带工程知识标签的静态知识向量");
        }

        if (!vectorFile.exists()) {
            log.info("🚀 未发现缓存，正在读取 kg.txt 并调用大模型 API 生成向量...");
        }
        addEngineeringDocuments(simpleVectorStore, vectorFile);
    }

    private void addEngineeringDocuments(org.springframework.ai.vectorstore.SimpleVectorStore simpleVectorStore,
                                          java.io.File vectorFile) {
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(kgResource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {

            List<org.springframework.ai.document.Document> documents = reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && line.contains(","))
                    .map(line -> {
                        String cleanLine = line.replaceAll("[\\[\\]']", "");
                        String[] parts = cleanLine.split(",", 3);
                        if (parts.length < 3) {
                            return null;
                        }
                        String subject = parts[0].trim();
                        String attribute = parts[1].trim();
                        String value = parts[2].trim();
                        if (subject.isEmpty() || attribute.isEmpty() || value.isEmpty()) {
                            return null;
                        }
                        String content = String.format("%s的%s是%s", subject, attribute, value);
                        return new org.springframework.ai.document.Document(content, Map.of(
                                "subject", subject,
                                "knowledgeBase", ENGINEERING_KNOWLEDGE_BASE,
                                "source", "kg.txt"));
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toList());

            if (!documents.isEmpty()) {
                simpleVectorStore.add(documents);
                simpleVectorStore.save(vectorFile);
                log.info("✅ 成功加载 {} 条工程知识并保存快照至 {}！", documents.size(), vectorFile.getPath());
            }

        } catch (Exception e) {
            log.error("❌ 初始化机甲知识库失败！", e);
        }
    }

    private boolean hasEngineeringMetadata(java.io.File vectorFile) {
        try {
            String cacheContent = java.nio.file.Files.readString(
                    vectorFile.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            return ENGINEERING_METADATA_PATTERN.matcher(cacheContent).find();
        } catch (java.io.IOException e) {
            log.warn("⚠️ 无法检查知识库缓存版本，将按旧版缓存处理", e);
            return false;
        }
    }
}
