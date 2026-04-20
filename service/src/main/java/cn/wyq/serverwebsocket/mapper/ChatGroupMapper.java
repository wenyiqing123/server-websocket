package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.dto.GroupQueryDTO;
import cn.wyq.serverwebsocket.pojo.entity.ChatGroup;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatGroupMapper extends BaseMapper<ChatGroup> {
    @Select("select id from chat_group where  owner_name=#{userName}")
    List<String> findGroupIdsByOwner(String userName);

    @Delete("delete from group_member where group_id=#{groupId}")
    void deleteAllMembersByGroupId(String groupId);

    @Select("select group_id from group_member where user_name=#{userName};")
    List<String> findGroupIdsByMember(String userName);

    @Delete("delete from chat_group where id=#{groupId}")
    void deleteGroupById(String groupId);

    @Delete("delete from group_member where group_id=#{groupId} and user_name=#{userName};")
    void deleteGroupMember(String groupId, String userName);

    @Select("select count(*) from group_member where group_id=#{groupId}")
    int countGroupMembers(String groupId);

    Page<ChatGroup> selectGroupList(GroupQueryDTO queryDTO);
}