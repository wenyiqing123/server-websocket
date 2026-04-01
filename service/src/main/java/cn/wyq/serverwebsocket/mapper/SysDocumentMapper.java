package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.entity.SysDocument;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysDocumentMapper extends BaseMapper<SysDocument> {
    // 继承 BaseMapper 即可获得基础的 CRUD 能力
}