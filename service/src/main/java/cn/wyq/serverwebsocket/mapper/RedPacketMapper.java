package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.entity.RedPacket;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface RedPacketMapper extends BaseMapper<RedPacket> {

    /**
     * 原子减库存：在 MySQL 层面保证红包不会被抢超
     * 只有当剩余个数 > 0 时才会更新成功
     */
    @Update("UPDATE u_red_packet SET remaining_amount = remaining_amount - #{amount}, " +
            "remaining_count = remaining_count - 1 " +
            "WHERE id = #{id} AND remaining_count > 0")
    int decreaseRedPacket(@Param("id") Long id, @Param("amount") BigDecimal amount);
}