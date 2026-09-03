package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.pojo.dto.GrabRedPacketDTO;
import cn.wyq.serverwebsocket.pojo.dto.SendRedPacketDTO;
import cn.wyq.serverwebsocket.pojo.vo.RedPacketDetailVO;
import cn.wyq.serverwebsocket.service.RedPacketService;
import cn.wyq.serverwebsocket.utils.BaseContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * RedPacketController：Web 层控制器，负责接收并处理 HTTP 请求。
 */
@Slf4j
@RestController
@RequestMapping("/redpacket")
@Tag(name = "红包模块接口", description = "处理发红包、抢红包等金融级高并发操作")
@CrossOrigin
public class RedPacketController {

    @Autowired
    private RedPacketService redPacketService;

    @PostMapping("/send")
    @Operation(summary = "发红包", description = "扣除余额，生成红包并预热进Redis")
    public Result sendRedPacket(@RequestBody SendRedPacketDTO dto) {
        // 确保 BaseContext.getCurrentId() 返回的是 Integer 或者转为 Integer
        Integer userId = BaseContext.getCurrentId().intValue();
        try {
            Long redPacketId = redPacketService.sendRedPacket(userId, dto.getGroupId(), dto.getTotalAmount(), dto.getTotalCount(), dto.getReceiverName());
            return Result.success(redPacketId); // 直接返回 ID 或者你组装好的字符串
        } catch (RuntimeException e) {
            log.error("发红包失败，用户: {}", userId, e);
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("发红包发生系统异常", e);
            return Result.error("系统繁忙，请稍后再试");
        }
    }


    @PostMapping("/grab")
    @Operation(summary = "抢红包", description = "高并发挡流抢单，异步更新MySQL")
    public Result grabRedPacket(@RequestBody GrabRedPacketDTO dto) {
        Integer userId = BaseContext.getCurrentId().intValue();
        try {
            // 注意这里传入的 groupId 类型
            BigDecimal grabbedAmount = redPacketService.grabRedPacket(userId, dto.getRedPacketId(), dto.getGroupId());
            return Result.success(grabbedAmount);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("抢红包发生系统异常", e);
            return Result.error("系统繁忙，请稍后再试");
        }
    }
    @GetMapping("/detail")
    public Result getDetail(@RequestParam Long redPacketId) {
        Integer userId = BaseContext.getCurrentId().intValue();
        RedPacketDetailVO detail = redPacketService.getRedPacketDetail(redPacketId, userId);
        return Result.success(detail);
    }
}