package cn.wyq.serverwebsocket.pojo.dto;

import lombok.Data;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

/**
 * UpdateEmailDto：数据传输对象，用于承载接口请求参数。
 */
@Data
public class UpdateEmailDto {

    @NotBlank(message = "用户名不能为空")
    private String userName; // 用于定位当前操作的用户

    @NotBlank(message = "密码不能为空")
    private String password; // 安全校验：防止别人拿你的电脑乱改

    @NotBlank(message = "新邮箱不能为空")
    private String newEmail; // 要绑定的新邮箱

    @NotBlank(message = "验证码不能为空")
    private String code;     // 发送到新邮箱的验证码
}