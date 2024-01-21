package cn.wqk.serverwebsocket.utils;


import cn.wqk.serverwebsocket.pojo.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.util.Date;

public class JWTUtil {
    public static final String SECRET = "secret_for_jwt2";

    public static String generateToken(User user) {
        return JWT.create()
                .withSubject(String.valueOf(user.getId()))
                .withAudience(user.getPhone(), user.getUserName(), user.getPassword())
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .sign(Algorithm.HMAC256(SECRET));
    }

    public static int decodeToken(String token) {
        String userId = JWT.decode(token).getSubject();
        return Integer.parseInt(userId);

    }

    public static boolean verifyToken(String token) {
        try {
            JWT.require(Algorithm.HMAC256(SECRET)).build().verify(token);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
