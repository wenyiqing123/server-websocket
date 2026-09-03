package cn.wyq.serverwebsocket.common;

import cn.wyq.serverwebsocket.constant.ResultConstant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PageResult：公共基础组件，为各模块提供可复用的基础能力。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PageResult<T> {
    private Integer code;
    private String msg;
    private long total;
    private T data;

    public static <T> PageResult<T> success(long total, T data) {
        return new PageResult<>(ResultConstant.SUCCESS, ResultConstant.SUCCESS_MSG, total, data);
    }

    public static <T> PageResult<T> error(String msg) {
        return new PageResult<>(ResultConstant.DefaultEroor, ResultConstant.DefaultEroor_MSG, 0L, null);
    }
}
