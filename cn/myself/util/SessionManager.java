package cn.myself.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import cn.myself.catalina.Context;
import cn.myself.http.Request;
import cn.myself.http.Response;
import cn.myself.http.StandardSession;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import sun.plugin2.util.SystemUtil;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * session的管理器，对session一级session过期时间等信息的管理
 */
public class SessionManager {

    // session列表
    private static Map<String, StandardSession> sessionMap = new HashMap<>();

    // 超时时间
    private static int defaultTime = getTimeout();

    static {
        // 开始便启动检测session是否过期的线程
        startSessionOutDateCheckThread();
    }

    /**
     * 开启线程检查session是否失效
     */
    private static void startSessionOutDateCheckThread(){
        new Thread(){
            @Override
            public void run() {
                while (true){
                    checkOutDateSession();
                    ThreadUtil.sleep(1000 * 30);
                }
            }
        }.start();
    }

    /**
     * 循环所有的session，判断是否有过期线程，每30秒调用一次
     */
    private static void checkOutDateSession(){
        List<String> outDateJessionIds = new ArrayList<>();
        // 循环遍历所有的session
        for (Map.Entry<String, StandardSession> entry : sessionMap.entrySet()) {
            StandardSession session = entry.getValue();
            // 时间差
            long interval = System.currentTimeMillis() - session.getLastAccessedTime();
            // 超时
            if(interval > session.getMaxInactiveInterval() * 1000){
                outDateJessionIds.add(entry.getKey());
            }
        }
        // 移除
        for (String jessionId : outDateJessionIds) {
            sessionMap.remove(jessionId);
        }
    }

    /**
     * 创建session的id
     *
     * @return 随机session的id
     */
    private static synchronized String generateSessionId(){
        String result = null;
        byte[] bytes = RandomUtil.randomBytes(16);
        result = new String(bytes);
        result = SecureUtil.md5(result);
        return result.toUpperCase();
    }

    /**
     * 获取session对象
     *
     * @param jsessionid session唯一id
     * @param request 请求对象
     * @param response 响应对象
     * @return session实例
     */
    public static HttpSession getSession(String jsessionid, Request request, Response response){
        if(jsessionid == null){
            return newSession(request, response);
        }else{
            // 从map获取
            StandardSession session = sessionMap.get(jsessionid);
            // 有可能session过期了
            if(null == session){
                return newSession(request, response);
            }else{
                // 未过期，设置最后访问时间
                session.setLastAccessTime(System.currentTimeMillis());
                // 创建cookie
                createCookieBySession(session, request, response);
                return session;
            }
        }
    }

    /**
     * 创建一个session
     *
     * @param request 请求对象
     * @param response 响应对象
     * @return session实例
     */
    private static HttpSession newSession(Request request, Response response){
        ServletContext context = request.getServletContext();
        String sessionId = generateSessionId();
        StandardSession session = new StandardSession(sessionId, context);
        session.setMaxInactiveInterval(defaultTime);
        sessionMap.put(sessionId, session);
        createCookieBySession(session, request, response);
        return session;
    }

    /**
     * 将sessionid写入到response中
     *
     * @param session 即将写入的session
     * @param request 请求
     * @param response 响应
     */
    private static void createCookieBySession(HttpSession session, Request request, Response response){
        Cookie cookie = new Cookie("JSESSIONID", session.getId());
        cookie.setMaxAge(session.getMaxInactiveInterval());
        cookie.setPath(request.getContext().getPath());
        response.addCookie(cookie);
    }

    /**
     * 获取超时时间
     *
     * @return 默认为30，或者web.xml中读取的超时时间
     */
    private static int getTimeout(){
        // 默认过期时间
        int defaultResult = 30;
        try {
            Document document = Jsoup.parse(Constant.webXMLFile, "utf-8");
            Elements elements = document.select("session-config session-timeout");
            // 设置了超时时间
            if(!elements.isEmpty()){
                return Convert.toInt(elements.get(0).text());
            }
            return defaultResult;
        }catch (IOException e){
            return defaultResult;
        }
    }

}
