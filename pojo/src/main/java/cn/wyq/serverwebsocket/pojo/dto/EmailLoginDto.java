package cn.wyq.serverwebsocket.pojo.dto;

import lombok.Data;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

/**
 * EmailLoginDto：数据传输对象，用于承载接口请求参数。
 */
@Data
public class EmailLoginDto {
    @NotBlank(message = "邮箱不能为空")
    private String email;
    @NotBlank(message = "验证码不能为空")
    private String code;
    private Integer applyRole; // 登录身份
}