package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.framework.common.AjaxResult;
import cn.wyq.serverwebsocket.pojo.vo.ActiveUsersAndMessageCount;
import cn.wyq.serverwebsocket.pojo.vo.RencetSevenDayUsersAndMessageCount;
import cn.wyq.serverwebsocket.pojo.vo.UsersAndMessages;
import cn.wyq.serverwebsocket.service.MessageStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController

@RequestMapping("/message-stats")
@CrossOrigin
@Tag(name = "消息统计控制器", description = "提供消息统计相关的接口")
/**
 * 消息统计控制器
 *
 * @author wyq
 * @date 2023/12/20
 */
public class MessageStatsController {
    @Autowired
    private MessageStatsService messageStatsService;

    /**
     * 查询某时间段内发送消息不为0的用户和其发送消息数量
     *
     * @param sendAtStart
     * @param sendAtEnd
     * @return
     */
    @GetMapping("/today-active")
    @Operation(summary = "查询某时间段内发送消息不为0的用户和其发送消息数量",
            description = "根据指定时间范围查询发送消息不为0的用户和其发送消息数量，返回用户列表")
    public AjaxResult<List<ActiveUsersAndMessageCount>> getTodayActiveUsersAndMessageCount(
            @RequestParam LocalDate sendAtStart,
            @RequestParam LocalDate sendAtEnd) {
        List<ActiveUsersAndMessageCount> activeUsersAndMessageCountList = messageStatsService.getTodayActiveUsersAndMessageCount(sendAtStart, sendAtEnd);
        return AjaxResult.success(activeUsersAndMessageCountList);
    }

    /**
     * 查询近一段时间内发送消息用户及其发送消息数量
     *
     * @return
     */
    @GetMapping("/recent-active")
    @Operation(summary = "查询近一段时间内发送消息用户及其发送消息数量",
            description = "查询近7天内发送消息用户及其发送消息数量，返回用户列表")
    public AjaxResult<List<RencetSevenDayUsersAndMessageCount>> getRencetSevenDayUsersAndMessageCount() {
        List<RencetSevenDayUsersAndMessageCount> rencetSevenDayUsersAndMessageCountList = messageStatsService.getRencetSevenDayUsersAndMessageCount();
        System.out.println("rencetSevenDayUsersAndMessageCountList = " + rencetSevenDayUsersAndMessageCountList);
        return AjaxResult.success(rencetSevenDayUsersAndMessageCountList);
    }

    @GetMapping("/users-messages")
    public AjaxResult<List<UsersAndMessages>> getActiveUsersAndMessages() {
        List<UsersAndMessages> usersAndMessagesList = messageStatsService.getActiveUsersAndMessages();
        return AjaxResult.success(usersAndMessagesList);
    }
}
