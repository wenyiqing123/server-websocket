package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.pojo.dto.SendTransferDTO;
import cn.wyq.serverwebsocket.service.TransferService;
import cn.wyq.serverwebsocket.utils.BaseContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * TransferController：Web 层控制器，负责接收并处理 HTTP 请求。
 */
@RestController
@RequestMapping("/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping("/send")
    public Result send(@RequestBody SendTransferDTO dto) {
        try {
            return Result.success(transferService.sendTransfer(
                    BaseContext.getCurrentId().intValue(), dto.getReceiverName(), dto.getAmount(), dto.getRemark()));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/accept")
    public Result accept(@RequestParam String transferNo) {
        try {
            return Result.success(transferService.acceptTransfer(
                    BaseContext.getCurrentId().intValue(), transferNo));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/detail")
    public Result detail(@RequestParam String transferNo) {
        return Result.success(transferService.getTransfer(transferNo));
    }
}
