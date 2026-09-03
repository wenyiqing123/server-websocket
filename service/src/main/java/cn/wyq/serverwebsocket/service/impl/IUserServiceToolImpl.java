package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.mapper.IUserMapperTool;
import cn.wyq.serverwebsocket.pojo.entity.User;
import cn.wyq.serverwebsocket.service.IUserServiceTool;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * IUserServiceToolImpl：业务服务实现类，封装对应领域的业务规则。
 */
@Service
public class IUserServiceToolImpl extends ServiceImpl<IUserMapperTool, User> implements IUserServiceTool {
}
