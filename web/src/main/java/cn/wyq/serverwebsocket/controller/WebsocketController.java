//package cn.wyq.serverwebsocket.controller;
//
//import cn.wyq.serverwebsocket.common.Result;
//import cn.wyq.serverwebsocket.socket.WebSocket;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/websocket")
//@Slf4j
//public class WebsocketController {
//    @PostMapping("/sendToTarget")
//    public Result sendSystemBroadcast(@RequestParam String targetUsername, @RequestParam String content) {
//        WebSocket.sendSystemBroadcast(targetUsername, content);
//        return Result.success();
//    }
//
//}
