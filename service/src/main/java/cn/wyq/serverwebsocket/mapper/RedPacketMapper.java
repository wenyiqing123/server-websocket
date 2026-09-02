package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.entity.RedPacket;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface RedPacketMapper extends BaseMapper<RedPacket> {

    @Select("SELECT * FROM u_red_packet WHERE id = #{id} FOR UPDATE")
    RedPacket selectByIdForUpdate(@Param("id") Long id);

    /**
     * 原子减库存：在 MySQL 层面保证红包不会被抢超
     * 只有当剩余个数 > 0 时才会更新成功
     */
    @Update("UPDATE u_red_packet SET remaining_amount = remaining_amount - #{amount}, " +
            "remaining_count = remaining_count - 1 " +
            "WHERE id = #{id} AND status = 0 AND remaining_count > 0 " +
            "AND remaining_amount >= #{amount}")
    int decreaseRedPacket(@Param("id") Long id, @Param("amount") BigDecimal amount);

    @Update("UPDATE u_red_packet SET status = 1, finish_time = NOW() " +
            "WHERE id = #{id} AND status = 0 AND remaining_count = 0")
    int markRedPacketFinished(@Param("id") Long id);

    @Select("SELECT id FROM u_red_packet " +
            "WHERE status = 0 AND remaining_amount > 0 " +
            "AND create_time <= DATE_SUB(NOW(), INTERVAL 1 DAY)")
    List<Long> selectExpiredRedPacketIds();

    @Update("UPDATE u_red_packet SET status = 2, remaining_amount = 0, " +
            "remaining_count = 0, finish_time = NOW() " +
            "WHERE id = #{id} AND status = 0 AND remaining_amount > 0 " +
            "AND create_time <= DATE_SUB(NOW(), INTERVAL 1 DAY)")
    int markRedPacketExpired(@Param("id") Long id);
}
