package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.entity.SysConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * SysConfigMapper：MyBatis 数据访问接口，负责数据库查询与持久化操作。
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {
}
