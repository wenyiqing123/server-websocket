package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.entity.Message;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * IMessageMapperTool：MyBatis 数据访问接口，负责数据库查询与持久化操作。
 */
@Mapper
public interface IMessageMapperTool extends BaseMapper<Message> {
}
