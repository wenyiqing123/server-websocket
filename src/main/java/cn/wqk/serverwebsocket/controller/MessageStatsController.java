package cn.wqk.serverwebsocket.controller;

import cn.wqk.serverwebsocket.framework.common.AjaxResult;
import cn.wqk.serverwebsocket.pojo.vo.ActiveUsersAndMessageCount;
import cn.wqk.serverwebsocket.pojo.vo.RencetSevenDayUsersAndMessageCount;
import cn.wqk.serverwebsocket.pojo.vo.UsersAndMessages;
import cn.wqk.serverwebsocket.service.MessageStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/message-stats")
@CrossOrigin
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
