package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.entity.GroupMember;
import cn.wyq.serverwebsocket.pojo.vo.GroupMemberQueryVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GroupMemberMapper extends BaseMapper<GroupMember> {
    List<GroupMemberQueryVo> queryGroupMembers(String groupId);
}