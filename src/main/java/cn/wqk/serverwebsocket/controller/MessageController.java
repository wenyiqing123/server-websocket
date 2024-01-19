package cn.wqk.serverwebsocket.controller;

import cn.wqk.serverwebsocket.common.Result;
import cn.wqk.serverwebsocket.service.MessageService;
import cn.wqk.serverwebsocket.socket.pojo.MessageInfo;
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

    @GetMapping
    public Result findAll(MessageInfo messageInfo) {
        List<MessageInfo> messageInfoList = messageService.findAll(messageInfo);
        return Result.success(messageInfoList);
    }
}
