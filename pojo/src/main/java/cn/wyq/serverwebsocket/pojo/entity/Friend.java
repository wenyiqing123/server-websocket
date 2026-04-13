package cn.wyq.serverwebsocket.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("friend")
public class Friend {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String userName;

    private String friendName;

    /**
     * 状态: 0-待通过, 1-已是好友, 2-已拒绝
     */
    private Integer status;

    private Date createTime;
}