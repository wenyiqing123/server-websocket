package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.mapper.TransactionLogMapper;
import cn.wyq.serverwebsocket.pojo.vo.AccountLogVO;
import cn.wyq.serverwebsocket.pojo.vo.LogQueryDTO;
import cn.wyq.serverwebsocket.service.TransactionLogService;
import cn.wyq.serverwebsocket.utils.CacheClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionLogServiceImpl implements TransactionLogService {

    @Autowired
    private TransactionLogMapper logMapper;
    @Autowired
    private CacheClient cacheClient;

    @Override
    public PageResult<List<AccountLogVO>> getLogPage(LogQueryDTO queryDTO) {
        LogQueryDTO query = queryDTO == null ? new LogQueryDTO() : queryDTO;
        String key = "page=" + query.getPageNum() + ",size=" + query.getPageSize()
                + ",user=" + query.getUserId() + ",type=" + query.getType()
                + ",start=" + query.getStartDate() + ",end=" + query.getEndDate();
        return cacheClient.queryWithMutex("finance:transaction-logs", key,
                new TypeReference<PageResult<List<AccountLogVO>>>() {}, ignored -> {
                    PageHelper.startPage(query.getPageNum(), query.getPageSize());
                    List<AccountLogVO> list = logMapper.selectLogList(query);
                    PageInfo<AccountLogVO> pageInfo = new PageInfo<>(list);
                    return PageResult.success(pageInfo.getTotal(), pageInfo.getList());
                }, 2, java.util.concurrent.TimeUnit.MINUTES);
    }
}
