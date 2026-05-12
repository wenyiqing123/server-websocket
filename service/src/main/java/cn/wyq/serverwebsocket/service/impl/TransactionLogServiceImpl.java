package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.mapper.TransactionLogMapper;
import cn.wyq.serverwebsocket.pojo.vo.AccountLogVO;
import cn.wyq.serverwebsocket.pojo.vo.LogQueryDTO;
import cn.wyq.serverwebsocket.service.TransactionLogService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionLogServiceImpl implements TransactionLogService {

    @Autowired
    private TransactionLogMapper logMapper;

    @Override
    public PageResult<List<AccountLogVO>> getLogPage(LogQueryDTO queryDTO) {
        // 开启 PageHelper 分页
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        // 执行连表查询
        List<AccountLogVO> list = logMapper.selectLogList(queryDTO);
        PageInfo<AccountLogVO> pageInfo = new PageInfo<>(list);

        // 封装返回结构，满足 data 和 total 的命名要求
        return PageResult.success(pageInfo.getTotal(), pageInfo.getList());
    }
}