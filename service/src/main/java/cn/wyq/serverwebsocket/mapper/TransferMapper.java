package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.entity.Transfer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * TransferMapper：MyBatis 数据访问接口，负责数据库查询与持久化操作。
 */
@Mapper
public interface TransferMapper extends BaseMapper<Transfer> {

    @Select("SELECT * FROM u_transfer WHERE transfer_no = #{transferNo} FOR UPDATE")
    Transfer selectByTransferNoForUpdate(@Param("transferNo") String transferNo);

    @Select("SELECT * FROM u_transfer WHERE transfer_no = #{transferNo}")
    Transfer selectByTransferNo(@Param("transferNo") String transferNo);

    @Select("SELECT * FROM u_transfer WHERE id = #{id} FOR UPDATE")
    Transfer selectByIdForUpdate(@Param("id") Long id);

    @Select("SELECT id FROM u_transfer WHERE status = 0 " +
            "AND create_time <= DATE_SUB(NOW(), INTERVAL #{expireHours} HOUR)")
    List<Long> selectExpiredTransferIds(@Param("expireHours") int expireHours);

    @Update("UPDATE u_transfer SET status = 2, return_time = NOW() " +
            "WHERE id = #{id} AND status = 0 " +
            "AND create_time <= DATE_SUB(NOW(), INTERVAL #{expireHours} HOUR)")
    int markTransferReturned(@Param("id") Long id, @Param("expireHours") int expireHours);

    @Update("UPDATE u_transfer SET status = 1, accept_time = NOW() " +
            "WHERE id = #{id} AND status = 0")
    int markTransferAccepted(@Param("id") Long id);
}
