package cn.wyq.serverwebsocket.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * FriendPathVO：视图对象，用于组织接口返回和展示数据。
 */
@AllArgsConstructor
@Data
@Builder
@NoArgsConstructor

public class FriendPathVO {
    private Integer id;

    private String userName;

    private String friendName;

    private String path;
    /**
     * 状态: 0-待通过, 1-已是好友, 2-已拒绝
     */
    private Integer status;

    private Date createTime;
}
