package cn.wyq.serverwebsocket.socket;

import cn.wyq.serverwebsocket.framework.common.ApplicationHelper;
import cn.wyq.serverwebsocket.framework.config.WebSocketConfig;
import cn.wyq.serverwebsocket.pojo.User;
import cn.wyq.serverwebsocket.service.MessageService;
import cn.wyq.serverwebsocket.service.UserService;
import cn.wyq.serverwebsocket.socket.pojo.MessageFull;
import cn.wyq.serverwebsocket.socket.pojo.MessageInfo;
import cn.wyq.serverwebsocket.utils.MessageUtils;
import com.alibaba.fastjson.JSON;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created with IntelliJ IDEA.
 *
 * @Auther: wyq
 * @Date: 2025/9/28
 * @Description:
 */
@Component
@Slf4j
@ServerEndpoint(value = "/websocket/{username}", configurator = WebSocketConfig.class) //暴露的ws应用的路径
public class WebSocket {
    /**
     *
     */
    private static final Map<String, Session> onlineUsers = new ConcurrentHashMap<>();
    /**
     * 当前在线客户端数量（线程安全的）
     */
    private static AtomicInteger onlineClientNumber = new AtomicInteger(0);
    /**
     * 当前在线客户端集合（线程安全的）：以键值对方式存储，key是连接的编号，value是连接的对象
     */
    private static Map<String, Session> onlineClientMap = new ConcurrentHashMap<>();

    /**
     * 静态注入
     */
//    private static MessageService messageService;
//
//    @Autowired
//    public void setINoticeService(MessageService messageService) {
//        WebSocket.messageService = messageService;
//    }
    private UserService userService = ApplicationHelper.getBean(UserService.class);
    /**
     * 通过上下文获取实例
     */
    private MessageService messageService = (MessageService) ApplicationHelper.getBean("messageService");
    private HttpSession httpSession;

    /**
     * 客户端与服务端连接成功
     *
     * @param session
     * @param username
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("username") String username, EndpointConfig config) {
        /*
            与当前客户端连接成功时
         */
        //0.获取token
        List<String> protocols = session.getRequestParameterMap().get("Sec-WebSocket-Protocol");
        if (protocols != null && !protocols.isEmpty()) {
            String token = protocols.get(0);

            // 校验 token，决定是否允许连接
        }
        //1，将session进行保存
        this.httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        onlineUsers.put(username, session);
        //2，广播消息。需要将登陆的所有的用户推送给所有的用户
        String message = MessageUtils.getMessage(true, null, getFriends());


        broadcastAllUsers(message);
        onlineClientNumber.incrementAndGet();//在线数+1
        onlineClientMap.put(session.getId(), session);//添加当前连接的session
        log.info("时间[{}]：与用户[{}]的连接成功，当前连接编号[{}]，当前连接总数[{}]", new Date().toLocaleString(), username, session.getId(), onlineClientNumber);
    }

    /**
     * 客户端与服务端连接关闭
     *
     * @param session
     * @param username
     */
    @OnClose
    public void onClose(Session session, @PathParam("username") String username) {
        /*
            do something for onClose
            与当前客户端连接关闭时
         */
        //1,从onlineUsers中剔除当前用户的session对象
//        String user = (String) this.httpSession.getAttribute("user");
        onlineUsers.remove(username);
        //2,通知其他所有的用户，当前用户下线了
        String message = MessageUtils.getMessage(true, null, getFriends());
        broadcastAllUsers(message);
        onlineClientNumber.decrementAndGet();//在线数-1
        onlineClientMap.remove(session.getId());//移除当前连接的session
        log.info("时间[{}]：与用户[{}]的连接关闭，当前连接编号[{}]，当前连接总数[{}]", new Date().toLocaleString(), username, session.getId(), onlineClientNumber);
    }

    /**
     * 客户端与服务端连接异常
     *
     * @param error
     * @param session
     * @param username
     */
    @OnError
    public void onError(Throwable error, Session session, @PathParam("username") String username) {
        /*
            do something for onError
            与当前客户端连接异常时
         */
        error.printStackTrace();
    }

    /**
     * 客户端向服务端发送消息
     *
     * @param message
     * @param username
     * @throws IOException
     */
    @OnMessage
    public void onMsg(Session session, String message, @PathParam("username") String username) throws IOException {
        /*
            do something for onMessage
            收到来自当前客户端的消息时
         */

        MessageInfo messageInfo = JSON.parseObject(message, MessageInfo.class);
        Integer id = messageService.addMessage(messageInfo);
        MessageFull messageFull = JSON.parseObject(message, MessageFull.class);
        messageFull.setId(id);
        String strmessageFull = JSON.toJSONString(messageFull);
        log.info("时间[{}]：来自连接编号为[{}]的消息：[{}]", new Date().toLocaleString(), session.getId(), strmessageFull);
        sendAllMessage(strmessageFull);
    }

    //向所有客户端发送消息（广播）
    private void sendAllMessage(String message) {

        Set<String> sessionIdSet = onlineClientMap.keySet(); //获得Map的Key的集合
        for (String sessionId : sessionIdSet) { //迭代Key集合
            Session session = onlineClientMap.get(sessionId); //根据Key得到value
            session.getAsyncRemote().sendText(message); //发送消息给客户端
        }
    }

    //只向当前客户端发送消息
    private void sendOneMessage(String message) {

    }

    public Set getFriends() {
        Set<String> keys = onlineUsers.keySet();
        HashSet<User> users = new HashSet<>();
        for (String key : keys) {
            String path = userService.getPath(key);
            users.add(new User().builder().userName(key).path(path).build());

        }

        return users;
    }


    private void broadcastAllUsers(String message) {
        try {
            //遍历map集合
            Set<Map.Entry<String, Session>> entries = onlineUsers.entrySet();
            for (Map.Entry<String, Session> entry : entries) {
                //获取到所有用户对应的session对象
                Session session = entry.getValue();
                //发送消息
                session.getBasicRemote().sendText(message);
            }
        } catch (Exception e) {
            //记录日志
        }
    }
}
