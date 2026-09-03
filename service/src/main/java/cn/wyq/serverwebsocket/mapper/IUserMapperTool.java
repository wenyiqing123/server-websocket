package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * IUserMapperTool：MyBatis 数据访问接口，负责数据库查询与持久化操作。
 */
@Mapper
public interface IUserMapperTool extends BaseMapper<User> {
}
