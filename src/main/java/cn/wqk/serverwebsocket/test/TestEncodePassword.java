package cn.wqk.serverwebsocket.test;

import cn.wqk.serverwebsocket.pojo.User;
import cn.wqk.serverwebsocket.utils.JWTUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


public class TestEncodePassword {

    public static void main(String[] args) {
        User user = User
                .builder()
                .id(1)
                .userName("wyq")
                .password("123")
                .build();

        String token = JWTUtil.generateToken(user);
        System.out.println("token = " + token);
        int userId = JWTUtil.getUserId(token);
        System.out.println("userId = " + userId);

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        String encodedPassword = bCryptPasswordEncoder.encode(user.getPassword());
        System.out.println("encodedPassword = " + encodedPassword);

    }
}
