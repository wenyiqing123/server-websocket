package cn.wqk.serverwebsocket.controller;

import cn.wqk.serverwebsocket.framework.common.AjaxResult;
import cn.wqk.serverwebsocket.framework.common.PageResult;
import cn.wqk.serverwebsocket.framework.common.Result;
import cn.wqk.serverwebsocket.pojo.dto.MessageQueryDTO;
import cn.wqk.serverwebsocket.pojo.entity.Message;
import cn.wqk.serverwebsocket.service.MessageService;
import cn.wqk.serverwebsocket.socket.pojo.MessageFull;
import cn.wqk.serverwebsocket.socket.pojo.MessageInfo;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/message")
@CrossOrigin
@Validated
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
    public Result recallMessage(@PathVariable int id) {
        messageService.recallMessage(id);
        return Result.success();
    }


    @GetMapping("/list")
    public PageResult<List<Message>> list(@ParameterObject MessageQueryDTO messageQueryDTO) {
        PageResult<List<Message>> pageResult = messageService.pageQuery(messageQueryDTO);
        return pageResult;
    }

    @DeleteMapping("/{id}")
    public AjaxResult<Void> deleteMessage(@PathVariable Integer id) {
        messageService.deleteMessage(id);
        return AjaxResult.success();
    }
}
