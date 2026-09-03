package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.common.AjaxResult;
import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.pojo.dto.MessageQueryDTO;
import cn.wyq.serverwebsocket.pojo.entity.Message;
import cn.wyq.serverwebsocket.pojo.socket.MessageFull;
import cn.wyq.serverwebsocket.pojo.socket.MessageInfo;
import cn.wyq.serverwebsocket.service.MessageService;
import cn.wyq.serverwebsocket.socket.WebSocket;
import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * MessageController：Web 层控制器，负责接收并处理 HTTP 请求。
 */
@RestController
@RequestMapping("/message")
@Validated
@Tag(name = "消息相关", description = "消息相关接口")
@Slf4j // 💡 新增：Lombok 日志注解
public class MessageController {

    @Autowired
    private MessageService messageService;

    @DeleteMapping("/clear")
    @Operation(summary = "清空与某人的聊天记录")
    public Result clearHistory(@RequestParam String userName, @RequestParam String targetName) {
        return messageService.clearHistory(userName, targetName);
    }
    @GetMapping("/history/filter")
    @Operation(summary = "高级筛选查询聊天记录")
    public Result getFilteredHistory(
            @RequestParam("userName") String userName,
            @RequestParam("targetName") String targetName,
            @RequestParam(value = "type", defaultValue = "0") Integer type,
            @RequestParam(value = "keyword", required = false) String keyword) {

        if (userName == null || targetName == null) {
            return Result.error("查询参数不完整");
        }
        return messageService.getFilteredHistory(userName, targetName, type, keyword);
    }

    /**
     * 查询所有消息
     *
     * @param messageInfo
     * @return
     */
    @GetMapping
    @Operation(summary = "查询所有消息", description = "查询所有消息，返回消息列表")
    public Result findAll(MessageInfo messageInfo) {
        log.info("请求查询所有消息，查询参数 payload: {}", messageInfo);
        List<MessageFull> messageFullListList = messageService.findAll(messageInfo);
        return Result.success(messageFullListList);
    }

    /**
     * 撤回消息
     *
     * @param id
     * @return
     */
    @PatchMapping("/{id}") // (以你实际的映射路径为准)
    public Result recallMsg(@PathVariable("id") int id) {

        // 1. 调用 Service 执行业务逻辑，拿到组装好的撤回消息对象
        MessageFull recallEvent = messageService.recallMessage(id);

        // 2. 序列化为 JSON
        String jsonRecall = JSON.toJSONString(recallEvent);

        // 🌟 3. 在 Controller 层调用 WebSocket 静态方法，跨网线通知对方！
        WebSocket.sendToTarget(recallEvent.getToName(), jsonRecall, recallEvent.getFromName());

        return Result.success("撤回成功");
    }

    /**
     * 分页查询消息
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询消息", description = "根据分页查询条件分页查询消息，返回消息列表")
    public PageResult<List<Message>> list(@ParameterObject MessageQueryDTO messageQueryDTO) {
        log.info("请求分页查询消息，分页条件 payload: {}", messageQueryDTO);
        return messageService.pageQuery(messageQueryDTO);
    }

    /**
     * 删除消息
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除消息", description = "根据消息ID删除消息")
    public AjaxResult<Void> deleteMessage(@PathVariable Integer id) {
        log.info("请求彻底删除消息，待删除消息ID: {}", id);
        messageService.deleteMessage(id);
        return AjaxResult.success();
    }


    @GetMapping("/export")
    @Operation(summary = "按条件导出消息", description = "根据检索条件导出全量消息记录，不进行分页")
    public void exportMessages(@ParameterObject MessageQueryDTO messageQueryDTO, HttpServletResponse response) throws IOException {
        log.info("请求导出消息记录，导出条件 payload: {}", messageQueryDTO);
        messageService.exportMessages(messageQueryDTO, response);
    }
}