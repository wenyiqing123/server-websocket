//package cn.wyq.serverwebsocket.config;
//
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/ai")
//public class ChatController {
//
//    private final ChatClient chatClient;
//
//    public ChatController(ChatClient.Builder builder, VectorStore vectorStore) {
//        // 配置 ChatClient，挂载 RAG（问答增强）能力
//        this.chatClient = builder
//                .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults()
//                        .withTopK(3) // 每次检索最相关的 3 条知识
//                        .withSimilarityThreshold(0.7) // 相似度阈值，过滤不相关的
//                ))
//                // 也可以在这里同时挂载你的 Tool（函数调用）
//                // .defaultFunctions("materialPriceTool")
//                .build();
//    }
//
//    @GetMapping("/chat")
//    public String chat(@RequestParam String message) {
//        return chatClient.prompt()
//                .user(message)
//                .call()
//                .content();
//    }
//}