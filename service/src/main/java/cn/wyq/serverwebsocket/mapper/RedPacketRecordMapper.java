package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.entity.RedPacketRecord;
import cn.wyq.serverwebsocket.pojo.vo.RedPacketDetailVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * RedPacketRecordMapper：MyBatis 数据访问接口，负责数据库查询与持久化操作。
 */
@Mapper
public interface RedPacketRecordMapper extends BaseMapper<RedPacketRecord> {

    /**
     * 联表查询红包明细，连带查出领取人的昵称和头像
     * 注意：这里的 u_user 和 user_name, path 请替换为你数据库里真实的用户表名和字段
     */
    @Select("SELECT r.user_id AS userId, u.user_name AS userName, u.path AS avatar, r.amount, r.create_time AS grabTime " +
            "FROM u_red_packet_record r " +
            "LEFT JOIN user u ON r.user_id = u.id " +
            "WHERE r.red_packet_id = #{redPacketId} " +
            "ORDER BY r.create_time DESC")
    List<RedPacketDetailVO.RecordItem> getRecordListByRedPacketId(@Param("redPacketId") Long redPacketId);
}