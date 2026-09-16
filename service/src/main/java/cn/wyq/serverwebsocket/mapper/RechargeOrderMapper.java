package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.entity.RechargeOrder;
import cn.wyq.serverwebsocket.pojo.vo.OrderQueryDTO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * RechargeOrderMapper：MyBatis 数据访问接口，负责数据库查询与持久化操作。
 */
@Mapper
public interface RechargeOrderMapper extends BaseMapper<RechargeOrder> {
    @Select("SELECT status FROM recharge_order WHERE out_trade_no = #{outTradeNo}")
    Integer selectStatusByOutTradeNo(@Param("outTradeNo") String outTradeNo);

    /**
     * 动态条件查询订单列表 (用于分页)
     * @param queryDTO 查询条件
     * @return 订单列表
     */
    List<RechargeOrder> selectOrderList(OrderQueryDTO queryDTO);

    /**
     * 开启排他锁查询订单 (防并发)
     * @param outTradeNo 内部订单号
     * @return 订单信息
     */
    RechargeOrder selectOrderForUpdate(@Param("outTradeNo") String outTradeNo);
    int updateOrderStatusToClosed(@Param("outTradeNo") String outTradeNo);
    /**
     * 更新订单状态为支付成功
     * @param outTradeNo 内部订单号
     * @return 影响行数
     */
    int updateOrderStatusToSuccess(@Param("outTradeNo") String outTradeNo);

    /**
     * 为用户账户增加余额
     * @param userId 用户ID
     * @param amount 充值金额
     * @return 影响行数
     */
    int addAccountBalance(@Param("userId") Integer userId, @Param("amount") BigDecimal amount);

    /**
     * 查询用户当前余额
     * @param userId 用户ID
     * @return 当前余额
     */
    BigDecimal getUserBalance(@Param("userId") Integer userId);

    /**
     * 插入资金变动流水明细
     * @param userId 用户ID
     * @param type 变动类型 (1: 充值)
     * @param amount 变动金额
     * @param balanceAfter 变动后余额
     * @param orderNo 关联单号
     * @param remark 备注
     * @return 影响行数
     */
    int insertAccountLog(
            @Param("userId") Integer userId,
            @Param("type") Integer type,
            @Param("amount") BigDecimal amount,
            @Param("balanceAfter") BigDecimal balanceAfter,
            @Param("orderNo") String orderNo,
            @Param("remark") String remark
    );
}
