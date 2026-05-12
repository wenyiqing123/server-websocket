package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.vo.AccountLogVO;
import cn.wyq.serverwebsocket.pojo.vo.LogQueryDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TransactionLogMapper {
    List<AccountLogVO> selectLogList(LogQueryDTO queryDTO);
}