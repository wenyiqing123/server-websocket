package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.entity.AccountLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountLogMapper extends BaseMapper<AccountLog> {
    // 使用 MyBatis-Plus 自带的 insert 即可完成流水记录
}