//package cn.wyq.serverwebsocket.controller;
//
//import com.azure.ai.inference.ChatCompletionsClient;
//import com.azure.ai.inference.ChatCompletionsClientBuilder;
//import com.azure.ai.inference.models.ChatCompletions;
//import com.azure.ai.inference.models.ChatCompletionsOptions;
//import com.azure.ai.inference.models.ChatRequestMessage;
//import com.azure.ai.inference.models.ChatRequestSystemMessage;
//import com.azure.ai.inference.models.ChatRequestUserMessage;
//import com.azure.core.credential.KeyCredential;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class GitHubModelsDemo {
//
//    public static void main(String[] args) {
//        // 1. 配置凭证和网关地址
//        String githubToken = "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"; // 替换为你刚才申请的 ghp_...
//        // 这是 GitHub Models 的官方统一 API 网关
//        String endpoint = "https://models.inference.ai.azure.com";
//
//        // 2. 初始化客户端 (Client是线程安全的，如果是 SpringBoot 项目，建议配置成单例 Bean)
//        ChatCompletionsClient client = new ChatCompletionsClientBuilder()
//                .credential(new KeyCredential(githubToken))
//                .endpoint(endpoint)
//                .buildClient();
//
//        // 3. 构建历史消息列表
//        List<ChatRequestMessage> chatMessages = new ArrayList<>();
//        // 系统提示词：定义 AI 的人设
//        chatMessages.add(new ChatRequestSystemMessage("你是一个幽默的 Java 架构师。"));
//        // 用户消息：当前的提问
//        chatMessages.add(new ChatRequestUserMessage("用一句话形容解决并发冲突的感受。"));
//
//        // 4. 配置请求参数 (模型名称、温度等)
//        ChatCompletionsOptions options = new ChatCompletionsOptions(chatMessages)
//                .setModel("gpt-4o") // 🌟 等将来发布了 gpt-5，直接把这里改成 "gpt-5" 即可
//                .setTemperature(0.7);
//
//        // 5. 发送同步请求并获取结果
//        System.out.println("正在向 GitHub Models 发送请求...");
//        try {
//            ChatCompletions response = client.complete(options);
//            // 解析并打印 AI 的回复
//            String aiReply = response.getChoices().get(0).getMessage().getContent();
//            System.out.println("AI 回复: " + aiReply);
//        } catch (Exception e) {
//            System.err.println("请求失败，请检查网络或 Token 权限: " + e.getMessage());
//        }
//    }
//}