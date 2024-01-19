package cn.wqk.serverwebsocket.controller;

import cn.wqk.serverwebsocket.common.Result;
import cn.wqk.serverwebsocket.pojo.User;
import cn.wqk.serverwebsocket.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = {"http://localhost:8080","https://web-websocket-8kacjl61a-linzeshuis-projects.vercel.app"})
@RequestMapping("/user")
@Validated
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result login(@RequestBody User user, HttpSession session) {
        User login=userService.login(user);
        if (login!=null){
            session.setAttribute("user",user.getUserName());
            return Result.success(login);
        }
        return Result.error();
    }

}
