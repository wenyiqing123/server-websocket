package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.entity.SysDocument;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * SysDocumentMapper：MyBatis 数据访问接口，负责数据库查询与持久化操作。
 */
@Mapper
public interface SysDocumentMapper extends BaseMapper<SysDocument> {
    // 继承 BaseMapper 即可获得基础的 CRUD 能力
}