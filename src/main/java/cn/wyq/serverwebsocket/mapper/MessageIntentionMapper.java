package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.entity.MessageIntention;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MessageIntentionMapper {
    void insert(MessageIntention intention);
    List<MessageIntention> selectAllOrderByTimeAsc();

    List<MessageIntention> selectByTagNameAndOriginalPrompt(String currentTag, String originalPrompt);
}
