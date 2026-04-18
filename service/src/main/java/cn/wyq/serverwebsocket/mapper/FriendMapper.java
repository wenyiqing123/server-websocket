package cn.wyq.serverwebsocket.mapper;


import cn.wyq.serverwebsocket.pojo.entity.Friend;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FriendMapper extends BaseMapper<Friend> {
    /**
     * 删除单向好友记录
     */
    int deleteFriendRecord(@Param("userName") String userName, @Param("friendName") String friendName);

    @Delete("delete from friend where user_name=#{userName} or friend_name=#{userName};")
    void deleteAllRelationsByUserName(String userName);

    @Select("select friend_name from friend where user_name=#{userName};")
    List<String> getMyFriendsNameByUsername(String userName);

//    List<String> findFriendsByUserName(String userName);
}