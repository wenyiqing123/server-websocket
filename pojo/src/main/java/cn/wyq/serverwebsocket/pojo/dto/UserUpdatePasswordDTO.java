package cn.wyq.serverwebsocket.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserUpdatePasswordDTO：数据传输对象，用于承载接口请求参数。
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UserUpdatePasswordDTO {
    @NotBlank(message = "旧密码不能为空")
    @Size(max = 128, message = "旧密码长度不能超过 128 个字符")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(max = 128, message = "新密码长度不能超过 128 个字符")
    private String newPassword;
}
