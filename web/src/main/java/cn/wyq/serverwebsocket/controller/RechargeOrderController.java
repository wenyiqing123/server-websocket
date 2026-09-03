package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.pojo.entity.RechargeOrder;
import cn.wyq.serverwebsocket.pojo.vo.OrderQueryDTO;
import cn.wyq.serverwebsocket.service.RechargeOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RechargeOrderController：Web 层控制器，负责接收并处理 HTTP 请求。
 */
@RestController
@RequestMapping("/finance/order")
public class RechargeOrderController {

    @Autowired
    private RechargeOrderService orderService;

    /**
     * 分页多条件查询订单
     */
    @GetMapping("/list")
    public Result getOrderList(OrderQueryDTO queryDTO) {
        PageResult<List<RechargeOrder>> page = orderService.getOrderPage(queryDTO);
        return Result.success(page);
    }

    /**
     * 管理员手动确认补单 (极其重要)
     */
    @PostMapping("/manualConfirm/{outTradeNo}")
    public Result manualConfirm(@PathVariable String outTradeNo) {
        // 实际开发中，这里可以从 Token 中提取操作的管理员 ID
        orderService.manualConfirmOrder(outTradeNo);
        return Result.success("补单成功");
    }
}