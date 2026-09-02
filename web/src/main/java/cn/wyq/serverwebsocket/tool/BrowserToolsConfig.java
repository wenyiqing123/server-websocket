//package cn.wyq.serverwebsocket.tool;
//
//import com.microsoft.playwright.Browser;
//import com.microsoft.playwright.BrowserType;
//import com.microsoft.playwright.Page;
//import com.microsoft.playwright.Playwright;
//import jakarta.annotation.PostConstruct;
//import jakarta.annotation.PreDestroy;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Description;
//
//import java.util.function.Function;
//
//@Configuration
//public class BrowserToolsConfig {
//
//    private Playwright playwright;
//    private Browser browser;
//    private Page page;
//
//    // 1. 初始化一个真实的浏览器窗口
//    @PostConstruct
//    public void init() {
//        playwright = Playwright.create();
//        // setHeadless(false) 意味着你能亲眼看到浏览器弹出来并自己乱动
//        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
//        page = browser.newPage();
//    }
//
//    // 在服务关闭时销毁浏览器
//    @PreDestroy
//    public void close() {
//        if (playwright != null) playwright.close();
//    }
//
//    // ==========================================
//    // 定义入参的数据结构 (Spring AI 会自动将其转为 JSON Schema 发给大模型)
//    // ==========================================
//    public record NavigateRequest(String url) {}
//    public record ClickRequest(String selector) {}
//    public record InputRequest(String selector, String text) {}
//    public record ReadRequest(String placeholder) {} // 占位
//
//    // ==========================================
//    // 定义供大模型调用的工具 (Tools)
//    // ==========================================
//
//    @Bean
//    @Description("导航/跳转到指定的网址 (URL)")
//    public Function<NavigateRequest, String> navigateTool() {
//        return request -> {
//            page.navigate(request.url());
//            // 返回页面标题给大模型，让它知道跳转成功了
//            return "成功跳转，当前页面标题为: " + page.title();
//        };
//    }
//
//    @Bean
//    @Description("在指定的网页元素(CSS选择器)中输入文本内容")
//    public Function<InputRequest, String> inputTool() {
//        return request -> {
//            page.fill(request.selector(), request.text());
//            return "已在元素 " + request.selector() + " 中输入了: " + request.text();
//        };
//    }
//
//    @Bean
//    @Description("点击指定的网页元素(CSS选择器)，例如按钮或链接")
//    public Function<ClickRequest, String> clickTool() {
//        return request -> {
//            page.click(request.selector());
//            return "成功点击了元素: " + request.selector();
//        };
//    }
//
//    @Bean
//    @Description("获取当前页面的纯文本内容，用于分析页面上有哪些信息")
//    public Function<ReadRequest, String> readPageTool() {
//        return request -> {
//            // 获取整个 body 的纯文本。真实场景中可能需要提取 DOM 树或截图
//            return page.locator("body").innerText();
//        };
//    }
//}