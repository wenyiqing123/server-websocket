package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.dto.WalletQueryDTO;
import cn.wyq.serverwebsocket.pojo.vo.UserWalletVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * UserWalletMapper：MyBatis 数据访问接口，负责数据库查询与持久化操作。
 */
@Mapper
public interface UserWalletMapper {
    // 列表查询
    List<UserWalletVO> selectWalletList(WalletQueryDTO queryDTO);

    // 获取余额并加排他锁
    BigDecimal getBalanceForUpdate(@Param("userId") Integer userId);

    // 更新余额 (可以直接加负数)
    int updateBalance(@Param("userId") Integer userId, @Param("amount") BigDecimal amount);

    // 插入流水
    int insertAccountLog(
            @Param("userId") Integer userId,
            @Param("type") Integer type,
            @Param("amount") BigDecimal amount,
            @Param("balanceAfter") BigDecimal balanceAfter,
            @Param("orderNo") String orderNo,
            @Param("remark") String remark
    );
}