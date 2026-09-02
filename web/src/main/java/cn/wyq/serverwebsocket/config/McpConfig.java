//package cn.wyq.serverwebsocket.config;
//
//// 🌟 1. 补充引入正确的 StdioClientTransport
//import io.modelcontextprotocol.client.McpClient;
//import io.modelcontextprotocol.client.McpSyncClient;
//import io.modelcontextprotocol.client.transport.ServerParameters;
//import io.modelcontextprotocol.client.transport.StdioClientTransport;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.time.Duration;
//
//@Configuration
//public class McpConfig {
//
//    // 推荐加上 destroyMethod = "close"，确保 Spring Boot 停止时，能优雅地关掉底层的 Node.js 子进程
//    @Bean(destroyMethod = "close")
//    public McpSyncClient mcpClient() {
//        // 1. 配置使用 Node.js 运行官方的 sqlite mcp server
//        ServerParameters params = ServerParameters.builder("npx")
//                .args("-y", "@modelcontextprotocol/server-sqlite", "/path/to/your/database.db")
//                .build();
//
//        // 🌟 2. 修正：官方传输类的正确名称是 StdioClientTransport
//        StdioClientTransport transport = new StdioClientTransport(params);
//
//        // 🌟 3. 修正：使用 Builder 模式，必须调用 .build() 才能生成最终的同步客户端 (McpSyncClient)
//        McpSyncClient mcpClient = McpClient.sync(transport)
//                .requestTimeout(Duration.ofSeconds(20)) // 建议加上超时时间，防止进程无响应卡死
//                .build();
//
//        // 4. 初始化连接并与外挂 Server 握手
//        mcpClient.initialize();
//
//        return mcpClient;
//    }
//}