package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.framework.common.AjaxResult;
import cn.wyq.serverwebsocket.framework.common.PageResult;
import cn.wyq.serverwebsocket.framework.common.Result;
import cn.wyq.serverwebsocket.pojo.dto.MessageQueryDTO;
import cn.wyq.serverwebsocket.pojo.entity.Message;
import cn.wyq.serverwebsocket.service.MessageService;
import cn.wyq.serverwebsocket.socket.pojo.MessageFull;
import cn.wyq.serverwebsocket.socket.pojo.MessageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/message")
@CrossOrigin
@Validated
@Tag(name = "消息相关", description = "消息相关接口")
public class MessageController {
    @Autowired
    private MessageService messageService;

    /**
     * 查询所有消息
     *
     * @param messageInfo
     * @return
     */
    @GetMapping
    @Operation(summary = "查询所有消息",
            description = "查询所有消息，返回消息列表")
    public Result findAll(MessageInfo messageInfo) {
        List<MessageFull> messageFullListList = messageService.findAll(messageInfo);
        return Result.success(messageFullListList);
    }

    /**
     * 撤回消息
     *
     * @param id
     * @return
     */
    @PatchMapping("/{id}")
    @Operation(summary = "撤回消息",
            description = "根据消息ID撤回消息")
    public Result recallMessage(@PathVariable int id) {
        messageService.recallMessage(id);
        return Result.success();
    }


    @GetMapping("/list")
    @Operation(summary = "分页查询消息",
            description = "根据分页查询条件分页查询消息，返回消息列表")
    public PageResult<List<Message>> list(@ParameterObject MessageQueryDTO messageQueryDTO) {
        return messageService.pageQuery(messageQueryDTO);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除消息",
            description = "根据消息ID删除消息")
    public AjaxResult<Void> deleteMessage(@PathVariable Integer id) {
        messageService.deleteMessage(id);
        return AjaxResult.success();
    }
}
