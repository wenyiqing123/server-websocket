package cn.wyq.serverwebsocket.mapper;


import cn.wyq.serverwebsocket.pojo.entity.Account;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * AccountMapper：MyBatis 数据访问接口，负责数据库查询与持久化操作。
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {

    /**
     * 悲观锁查询：锁定该行数据，防止转账或发红包时余额被并发修改
     */
    @Select("SELECT * FROM u_account WHERE user_id = #{userId} FOR UPDATE")
    Account selectByUserIdForUpdate(@Param("userId") Integer userId);
    @Select("SELECT balance FROM u_account WHERE user_id = #{userId}")
    BigDecimal selectBalanceByUserId(@Param("userId") Long userId);
}