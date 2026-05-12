package cn.wyq.serverwebsocket.controller;


import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.pojo.vo.AccountLogVO;
import cn.wyq.serverwebsocket.pojo.vo.LogQueryDTO;
import cn.wyq.serverwebsocket.service.TransactionLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/finance/log") // 🌟 无 /api 前缀
public class TransactionLogController {

    @Autowired
    private TransactionLogService logService;

    /**
     * 分页查询资金流水明细
     */
    @GetMapping("/list")
    public Result getLogList(LogQueryDTO queryDTO) {
        PageResult<List<AccountLogVO>> page = logService.getLogPage(queryDTO);
        // 返回的结果结构就是：{ code: 200, data: { total: 100, data: [...] } }
        return Result.success(page);
    }
}