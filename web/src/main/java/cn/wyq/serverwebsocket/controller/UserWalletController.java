package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.pojo.dto.AdjustBalanceDTO;
import cn.wyq.serverwebsocket.pojo.dto.WalletQueryDTO;
import cn.wyq.serverwebsocket.pojo.vo.UserWalletVO;
import cn.wyq.serverwebsocket.service.UserWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * UserWalletController：Web 层控制器，负责接收并处理 HTTP 请求。
 */
@RestController
@RequestMapping("/finance/wallet") // 🌟 严格遵守不要 /api 的约定
public class UserWalletController {

    @Autowired
    private UserWalletService walletService;

    /**
     * 分页查询用户钱包列表
     */
    @GetMapping("/list")
    public Result getWalletList(WalletQueryDTO queryDTO) {
        PageResult<List<UserWalletVO>> page = walletService.getWalletPage(queryDTO);
        return Result.success(page);
    }

    /**
     * 人工调账核心接口
     */
    @PostMapping("/adjust")
    public Result adjustBalance(@RequestBody AdjustBalanceDTO dto) {
        if (dto.getAmount() == null || dto.getAmount().doubleValue() <= 0) {
            return Result.error("调账金额必须大于0");
        }
        walletService.manualAdjustBalance(dto);
        return Result.success("调账成功");
    }
}