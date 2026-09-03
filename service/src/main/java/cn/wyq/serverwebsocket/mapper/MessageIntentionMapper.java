package cn.wyq.serverwebsocket.mapper;


import cn.wyq.serverwebsocket.pojo.entity.MessageIntention;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MessageIntentionMapper：MyBatis 数据访问接口，负责数据库查询与持久化操作。
 */
@Mapper
public interface MessageIntentionMapper {
    void insert(MessageIntention intention);
    List<MessageIntention> selectAllOrderByTimeAsc();

    List<MessageIntention> selectByTagNameAndOriginalPrompt(String currentTag, String originalPrompt);
}
