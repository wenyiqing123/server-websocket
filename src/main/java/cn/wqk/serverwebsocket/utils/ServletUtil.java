package cn.wqk.serverwebsocket.utils;


import com.alibaba.fastjson.JSONObject;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;

public class ServletUtil {
    /**
     * 将字符串渲染到客户端
     *
     * @param response 渲染对象
     * @param msg
     * @param code
     * @return
     */
    public static String renderString(HttpServletResponse response, String msg, int code) {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("code", code);
        hashMap.put("success", false);
        hashMap.put("msg", msg);

        String jsonString = JSONObject.toJSONString(hashMap);
        try {
            response.setStatus(200);
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            response.getWriter().print(jsonString);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
