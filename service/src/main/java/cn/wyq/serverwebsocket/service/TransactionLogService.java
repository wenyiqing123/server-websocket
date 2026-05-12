package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.pojo.vo.AccountLogVO;
import cn.wyq.serverwebsocket.pojo.vo.LogQueryDTO;

import java.util.List;

public interface TransactionLogService {
    PageResult<List<AccountLogVO>> getLogPage(LogQueryDTO queryDTO);
}