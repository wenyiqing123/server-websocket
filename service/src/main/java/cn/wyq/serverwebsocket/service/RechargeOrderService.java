package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.pojo.entity.RechargeOrder;
import cn.wyq.serverwebsocket.pojo.vo.OrderQueryDTO;

import java.util.List;

/**
 * RechargeOrderService：业务服务接口，定义对应领域的业务操作契约。
 */
public interface RechargeOrderService {
    /**
     * 获取订单分页列表
     * @param queryDTO 包含页码、页大小和筛选条件的 DTO
     * @return 分页结果封装类
     */
    PageResult<List<RechargeOrder>> getOrderPage(OrderQueryDTO queryDTO);

    /**
     * 管理员手动确认订单并补发资金 (包含核心财务事务)
     * @param outTradeNo 内部订单号
     */
    void manualConfirmOrder(String outTradeNo);
}
