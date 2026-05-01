package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.entity.RechargeOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RechargeOrderMapper extends BaseMapper<RechargeOrder> {
    @Select("SELECT status FROM recharge_order WHERE out_trade_no = #{outTradeNo}")
    Integer selectStatusByOutTradeNo(@Param("outTradeNo") String outTradeNo);
}