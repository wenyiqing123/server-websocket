package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.vo.AccountLogVO;
import cn.wyq.serverwebsocket.pojo.vo.LogQueryDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * TransactionLogMapper：MyBatis 数据访问接口，负责数据库查询与持久化操作。
 */
@Mapper
public interface TransactionLogMapper {
    List<AccountLogVO> selectLogList(LogQueryDTO queryDTO);
}