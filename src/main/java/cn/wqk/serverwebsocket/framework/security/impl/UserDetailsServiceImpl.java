//package cn.wqk.serverwebsocket.framework.security.impl;
//
//
//import cn.wqk.serverwebsocket.framework.exception.ServiceException;
//import cn.wqk.serverwebsocket.mapper.UserMapper;
//import cn.wqk.serverwebsocket.pojo.User;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//
//@Service
//public class UserDetailsServiceImpl implements UserDetailsService {
//    @Autowired
//    private UserMapper userMapper;
//
//    @Override
//    //此处username意指用户的主键，不强制使用用户名
//    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//        User user = userMapper.findByUsername(username);
//        if (user == null) {
//            throw new ServiceException("用户名或密码错误", 401);
//        }
//        return new LoginUser(user);
//    }
//}
