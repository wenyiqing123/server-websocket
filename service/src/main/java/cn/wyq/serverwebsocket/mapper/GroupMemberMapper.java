package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.entity.GroupMember;
import cn.wyq.serverwebsocket.pojo.vo.GroupMemberQueryVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * GroupMemberMapper：MyBatis 数据访问接口，负责数据库查询与持久化操作。
 */
@Mapper
public interface GroupMemberMapper extends BaseMapper<GroupMember> {
    List<GroupMemberQueryVo> queryGroupMembers(String groupId);
}