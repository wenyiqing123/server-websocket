package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.mapper.RechargeOrderMapper;
import cn.wyq.serverwebsocket.pojo.entity.RechargeOrder;
import cn.wyq.serverwebsocket.pojo.vo.OrderQueryDTO;
import cn.wyq.serverwebsocket.service.RechargeOrderService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RechargeOrderServiceImpl implements RechargeOrderService {

    @Autowired
    private RechargeOrderMapper orderMapper;

    @Override
    public PageResult<List<RechargeOrder>> getOrderPage(OrderQueryDTO queryDTO) {
        // 开启 PageHelper 分页
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        List<RechargeOrder> list = orderMapper.selectOrderList(queryDTO);
        PageInfo<RechargeOrder> pageInfo = new PageInfo<>(list);
        return  PageResult.success(pageInfo.getTotal(), pageInfo.getList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 强事务控制，防止钱加了订单没改
    public void manualConfirmOrder(String outTradeNo) {
        // 1. 锁订单防并发 (排他锁)
        RechargeOrder order = orderMapper.selectOrderForUpdate(outTradeNo);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new RuntimeException("该订单状态不是待支付，无法补单");
        }

        // 2. 更新订单状态为 1 (支付成功) 和 pay_time
        orderMapper.updateOrderStatusToSuccess(outTradeNo);

        // 3. 为用户钱包表加钱 (u_account)
        orderMapper.addAccountBalance(order.getUserId(), order.getAmount());

        // 4. 查询变更后的余额，用于记录流水
        // 假设你有一个查询当前余额的方法
        BigDecimal currentBalance = orderMapper.getUserBalance(order.getUserId());

        // 5. 插入资金流水表 (u_account_log), type=1 代表充值
        orderMapper.insertAccountLog(order.getUserId(), 1, order.getAmount(), currentBalance, outTradeNo, "管理员手动补单");
    }
}