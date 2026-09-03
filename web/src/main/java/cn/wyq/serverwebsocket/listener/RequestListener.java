package cn.wyq.serverwebsocket.listener;


import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * 监听器类:主要任务是用ServletRequest将我们的HttpSession携带过去
 */
/**
 * RequestListener：事件监听组件，负责接收并处理应用事件。
 */
@Component
public class RequestListener implements ServletRequestListener {
    @Override
    public void requestInitialized(ServletRequestEvent sre)  {

        //将所有request请求都携带上httpSession
        ((HttpServletRequest) sre.getServletRequest()).getSession();
    }
    public RequestListener() {}

    @Override
    public void requestDestroyed(ServletRequestEvent arg0)  {}
}

