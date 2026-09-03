package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.pojo.vo.AccountLogVO;
import cn.wyq.serverwebsocket.pojo.vo.LogQueryDTO;

import java.util.List;

/**
 * TransactionLogService：业务服务接口，定义对应领域的业务操作契约。
 */
public interface TransactionLogService {
    PageResult<List<AccountLogVO>> getLogPage(LogQueryDTO queryDTO);
}