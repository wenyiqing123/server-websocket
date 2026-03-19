package cn.wyq.serverwebsocket.sentinel;

import cn.wyq.serverwebsocket.common.Result;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import lombok.extern.slf4j.Slf4j;
// 引入你项目中的统一返回类和实体类（请根据实际包名替换）
// import cn.wyq.common.core.Result;
// import cn.wyq.pojo.User;

@Slf4j
public class GlobalBlockHandler {

    /**
     * 针对根据 ID 查询用户的限流/降级处理方法
     * * ⚠️ 【严格要求】：
     * 1. 必须是 public static
     * 2. 返回值必须与 Controller 原方法完全一致（这里是 Result<User>）
     * 3. 参数列表必须与原方法完全一致（这里是 String id），最后加上 BlockException
     */
    public static Result handleGetUserBlock(int id, BlockException ex) {

        // 1. 判断是被“限流”了，还是被“熔断”了
        if (ex instanceof FlowException) {
            log.warn("【流控拦截】接口请求太频繁。查询ID: {}", id);
            // 429 是 HTTP 标准状态码，代表 Too Many Requests
            return Result.error(429, "系统太火爆啦，请喝口水再来试吧~");

        } else if (ex instanceof DegradeException) {
            log.error("【熔断拦截】下游服务响应太慢，触发熔断保护。查询ID: {}", id);
            // 503 是 HTTP 标准状态码，代表 Service Unavailable
            return Result.error(503, "服务暂时开小差了，请稍后再试！");
        }

        // 2. 兜底的其他拦截类型（如系统规则、授权规则等）
        log.error("【Sentinel拦截】未知拦截类型：{}", ex.getClass().getSimpleName());
        return Result.error(500, "系统繁忙，请重试");
    }

    /**
     * 如果你有其他接口，比如无参数的查询列表接口，可以继续在这里加方法
     */
    // public static Result<List<User>> handleListUsersBlock(BlockException ex) { ... }
}