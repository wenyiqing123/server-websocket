package cn.wyq.serverwebsocket.controller;

import cn.wyq.serverwebsocket.annotation.CurrentUser;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.exception.ServiceException;
import cn.wyq.serverwebsocket.mapper.UserMapper;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;
import cn.wyq.serverwebsocket.pojo.vo.LoginAuditQueryDTO;
import cn.wyq.serverwebsocket.service.LoginAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理端用户登录审计日志接口。 */
@RestController
@RequestMapping("/login-audit")
@RequiredArgsConstructor
public class LoginAuditController {
    private final LoginAuditService loginAuditService;
    private final UserMapper userMapper;

    /** 分页查询登录审计日志。 */
    @GetMapping("/page")
    public Result page(@ModelAttribute LoginAuditQueryDTO query, @CurrentUser UserEntity currentUser) {
        assertAdmin(currentUser);
        return Result.success(loginAuditService.page(query));
    }

    /** 查询登录审计统计卡片。 */
    @GetMapping("/stats")
    public Result stats(@ModelAttribute LoginAuditQueryDTO query, @CurrentUser UserEntity currentUser) {
        assertAdmin(currentUser);
        return Result.success(loginAuditService.stats(query));
    }

    private void assertAdmin(UserEntity currentUser) {
        if (currentUser == null || currentUser.getId() <= 0) {
            throw new ServiceException("请先登录", 401);
        }
        var persisted = userMapper.getCurrentUserById(currentUser.getId());
        if (persisted == null || !Integer.valueOf(1).equals(persisted.getStatus())
                || (persisted.getRole() != 0 && persisted.getRole() != 1)) {
            throw new ServiceException("仅管理员可以查看登录审计日志", 403);
        }
    }
}
