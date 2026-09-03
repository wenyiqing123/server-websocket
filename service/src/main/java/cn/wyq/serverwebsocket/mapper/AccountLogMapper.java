package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.entity.AccountLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * AccountLogMapper：MyBatis 数据访问接口，负责数据库查询与持久化操作。
 */
@Mapper
public interface AccountLogMapper extends BaseMapper<AccountLog> {
    // 使用 MyBatis-Plus 自带的 insert 即可完成流水记录
}