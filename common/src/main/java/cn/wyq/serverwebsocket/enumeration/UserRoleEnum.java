package cn.wyq.serverwebsocket.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserRoleEnum {
    SUPER_ADMIN(0, "超级管理员"),
    ADMIN(1, "管理员"),
    USER(2, "普通用户");

    private final Integer code;
    private final String desc;

    public static String getDescByCode(Integer code) {
        if (code == null) return "未知";
        for (UserRoleEnum e : values()) {
            if (e.code.equals(code)) {
                return e.desc;
            }
        }
        return "未知";
    }
}