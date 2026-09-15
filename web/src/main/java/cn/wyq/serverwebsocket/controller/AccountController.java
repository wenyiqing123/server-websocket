package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.pojo.dto.RechargeDTO;
import cn.wyq.serverwebsocket.service.AccountService;
import cn.wyq.serverwebsocket.utils.BaseContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * AccountController：Web 层控制器，负责接收并处理 HTTP 请求。
 */
@RestController
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @PostMapping("/recharge")
    public Result recharge(@RequestBody RechargeDTO dto) {
        Integer userId = BaseContext.getCurrentId().intValue();
        try {
            BigDecimal newBalance = accountService.recharge(userId, dto.getAmount());
            return Result.success(newBalance);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("充值失败，请稍后再试");
        }
    }
    @GetMapping("/balance")
    public Result getBalance(@RequestParam(value = "fresh", defaultValue = "false") boolean fresh) {
         Long currentId = BaseContext.getCurrentId();

        // 支付成功确认后使用数据库回源，避免读取到支付事务提交前的旧缓存。
        BigDecimal balance = fresh
                ? accountService.getCurrentUserBalanceFresh(currentId)
                : accountService.getCurrentUserBalance(currentId);

        // 使用你项目中统一定义的 Result 包装返回
        return Result.success(balance);
    }
}
