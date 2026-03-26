package cn.wyq.serverwebsocket.config;

import com.github.pagehelper.PageInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class MybatisConfig {

    /**
     * 手动配置 PageHelper 拦截器
     */
    @Bean
    public PageInterceptor pageInterceptor() {
        PageInterceptor pageInterceptor = new PageInterceptor();

        // 配置 PageHelper 的参数
        Properties properties = new Properties();
        // 指定数据库方言，这里以 MySQL 为例
        properties.setProperty("helperDialect", "mysql");
        // 分页合理化参数：如果 pageNum<=0 会查询第一页，pageNum>pages 会查询最后一页
        properties.setProperty("reasonable", "true");
        // 支持通过 Mapper 接口参数来传递分页参数
        properties.setProperty("supportMethodsArguments", "true");

        pageInterceptor.setProperties(properties);
        return pageInterceptor;
    }
}