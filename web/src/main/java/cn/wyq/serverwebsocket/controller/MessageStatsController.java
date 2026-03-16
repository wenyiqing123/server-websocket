package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.common.AjaxResult;
import cn.wyq.serverwebsocket.pojo.vo.ActiveUsersAndMessageCount;
import cn.wyq.serverwebsocket.pojo.vo.RencetSevenDayUsersAndMessageCount;
import cn.wyq.serverwebsocket.pojo.vo.UsersAndMessages;
import cn.wyq.serverwebsocket.service.MessageStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/message-stats")
@Tag(name = "消息统计控制器", description = "提供消息统计相关的接口")
@Slf4j // 💡 新增：Lombok 日志注解
public class MessageStatsController {

    @Autowired
    private MessageStatsService messageStatsService;

    /**
     * 查询某时间段内发送消息不为0的用户和其发送消息数量
     */
    @GetMapping("/today-active")
    @Operation(summary = "查询某时间段内发送消息不为0的用户和其发送消息数量",
            description = "根据指定时间范围查询发送消息不为0的用户和其发送消息数量，返回用户列表")
    public AjaxResult<List<ActiveUsersAndMessageCount>> getTodayActiveUsersAndMessageCount(
            @RequestParam LocalDate sendAtStart,
            @RequestParam LocalDate sendAtEnd
    ) {
        log.info("请求统计活跃用户：时间范围 [{} ~ {}]", sendAtStart, sendAtEnd);
        List<ActiveUsersAndMessageCount> activeUsersAndMessageCountList =
                messageStatsService.getTodayActiveUsersAndMessageCount(sendAtStart, sendAtEnd);
        return AjaxResult.success(activeUsersAndMessageCountList);
    }

    /**
     * 查询近一段时间内发送消息用户及其发送消息数量
     */
    @GetMapping("/recent-active")
    @Operation(summary = "查询近一段时间内发送消息用户及其发送消息数量",
            description = "查询近一段时间内发送消息用户及其发送消息数量，返回用户列表")
    public AjaxResult<List<RencetSevenDayUsersAndMessageCount>> getRencetSevenDayUsersAndMessageCount(
            @RequestParam LocalDate sendAtStart,
            @RequestParam LocalDate sendAtEnd
    ) {
        log.info("请求统计近段时间发送趋势：时间范围 [{} ~ {}]", sendAtStart, sendAtEnd);
        List<RencetSevenDayUsersAndMessageCount> rencetSevenDayUsersAndMessageCountList =
                messageStatsService.getRencetSevenDayUsersAndMessageCount(sendAtStart, sendAtEnd);
        return AjaxResult.success(rencetSevenDayUsersAndMessageCountList);
    }

    /**
     * 查询固定时间内活跃用户及其发送消息数量
     */
    @GetMapping("/users-messages")
    @Operation(summary = "查询固定时间内活跃用户及其发送消息数量",
            description = "查询固定时间内活跃用户及其发送消息数量")
    public AjaxResult<List<UsersAndMessages>> getActiveUsersAndMessages(
            @RequestParam LocalDate sendAtStart,
            @RequestParam LocalDate sendAtEnd
    ) {
        log.info("请求查询活跃用户及消息详情：时间范围 [{} ~ {}]", sendAtStart, sendAtEnd);
        List<UsersAndMessages> usersAndMessagesList =
                messageStatsService.getActiveUsersAndMessages(sendAtStart, sendAtEnd);
        return AjaxResult.success(usersAndMessagesList);
    }
}