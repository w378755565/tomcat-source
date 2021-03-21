package cn.myself.http;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.myself.catalina.Connector;
import cn.myself.catalina.Context;
import cn.myself.catalina.Engine;
import cn.myself.catalina.Service;
import cn.myself.util.MiniBrowser;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.*;

/**
 * 请求对象
 */
public class Request extends BaseRequest{
    // 请求的uri，资源的最终路径
    private String uri;
    // 请求的串
    private String requestString;
    // 连接
    private Socket socket;
    // Context
    private Context context;
    // engine对象
    private Service service;
    // 请求方法
    private String method;
    // cookies
    private Cookie[] cookies;
    // session
    private HttpSession session;

    private Connector connector;

    // 是否已经转发
    private boolean forwarded;

    // 请求字符串 name=xxx&age=xxx
    private String queryString;
    // 参数列表
    private Map<String, String[]> parameterMap;
    // 头信息映射表
    private Map<String, String> headerMap;
    // 参数列表
    private Map<String, Object> attributesMap;

    public Request(Socket socket, Connector connector) throws IOException {
        this.socket = socket;
        this.connector = connector;
        this.service = connector.getService();
        this.parameterMap = new HashMap<>();
        this.headerMap = new HashMap<>();
        this.attributesMap = new HashMap<>();
        // 解析请求串
        parseHttpRequest();
        // 如果解析不成功则结束
        if (StrUtil.isEmpty(this.requestString)) {
            return;
        }
        // 解析Uri
        parseUri();
        // 解析Context
        parseContext();
        // 解析method
        parseMethod();
        // 解析参数
        parseParameters();
        // 解析头信息
        parseHeaders();
        // 解析cookie
        parseCookies();
        if (!"/".equals(context.getPath())) {
            uri = StrUtil.removePrefix(uri, context.getPath());
            if (StrUtil.isEmpty(uri)) {
                uri = "/";
            }
        }
    }

    @Override
    public void removeAttribute(String name) {
        this.attributesMap.remove(name);
    }

    @Override
    public void setAttribute(String name, Object o) {
        this.attributesMap.put(name, o);
    }

    @Override
    public Object getAttribute(String name) {
        return this.attributesMap.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        Set<String> set = this.attributesMap.keySet();
        return Collections.enumeration(set);
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String uri) {
        return new ApplicationRequestDispatcher(uri);
    }

    public Connector getConnector() {
        return connector;
    }

    public Socket getSocket() {
        return socket;
    }

    @Override
    public HttpSession getSession() {
        return session;
    }

    public void setSession(HttpSession session) {
        this.session = session;
    }

    /**
     * 从cookie中获取sessionid
     */
    public String getSessionIdFromCookie(){
        if(cookies != null) {
            for (Cookie cookie : cookies) {
                if ("JSESSIONID".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public boolean isForwarded() {
        return forwarded;
    }

    public void setForwarded(boolean forwarded) {
        this.forwarded = forwarded;
    }

    /**
     * 解析cookies
     */
    private void parseCookies(){
        List<Cookie> cookieList = new ArrayList<>();
        String cookieStr = headerMap.get("cookie");
        if(cookieStr != null){
            String[] pairs = StrUtil.split(cookieStr, ";");
            for (String pair : pairs) {
                if(!StrUtil.isBlank(pair)){
                    String[] segs = StrUtil.split(pair, "=");
                    String name = segs[0].trim();
                    String value = segs[1].trim();
                    Cookie cookie = new Cookie(name, value);
                    cookieList.add(cookie);
                }
            }
        }
        this.cookies = ArrayUtil.toArray(cookieList, Cookie.class);
    }

    /**
     * 解析头信息
     */
    private void parseHeaders(){
        StringReader stringReader = new StringReader(this.requestString);
        List<String> lines = new ArrayList<>();
        // 读取行到List集合
        IoUtil.readLines(stringReader, lines);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if(0 == line.length())
                break;
            String[] segs = line.split(":");
            if(segs.length <= 1){
                continue;
            }
            String headerName = segs[0].toLowerCase();
            String headerValue = segs[1];
            this.headerMap.put(headerName,headerValue);
        }

    }

    @Override
    public Cookie[] getCookies() {
        return this.cookies;
    }

    @Override
    public String getHeader(String name) {
        if(name == null){
            return null;
        }
        name = name.toLowerCase();
        return this.headerMap.get(name);
    }

    @Override
    public int getIntHeader(String name) {
        String value = headerMap.get(name);
        return Convert.toInt(value, 0);
    }

    public Map<String, String> getHeaderMap() {
        return headerMap;
    }


    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> keySet = this.headerMap.keySet();
        return Collections.enumeration(keySet);
    }

    private void parseParameters(){
        if("GET".equals(this.method)){
            // 获取到url
            String url = StrUtil.subBetween(this.requestString, " ", " ");
            // 进行参数的获取
            if(StrUtil.contains(url, '?')){
                // 设置请求参数
                this.queryString = StrUtil.subAfter(url, '?', false);
            }
        }
        if("POST".equals(this.method)){
            this.queryString = StrUtil.subAfter(this.requestString, "\r\n\r\n", false);
        }
        System.out.println(this.queryString);
        if(null != queryString){
            // 解析参数
            this.queryString = URLUtil.decode(queryString);
            String[] parameterValues = this.queryString.split("&");
            if(parameterValues != null){
                for (String parameterValue : parameterValues) {
                    String[] nameValues = parameterValue.split("=");
                    String name = nameValues[0];
                    String value = nameValues[1];
                    String values[] = this.parameterMap.get(name);
                    values = null == values ? new String[]{ value } : ArrayUtil.append(values, value);
                    this.parameterMap.put(name, values);
                }
            }
        }
    }

    @Override
    public String getParameter(String name) {
        String values[] = this.parameterMap.get(name);
        if(null != values && 0 != values.length){
            return values[0];
        }
        return null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameterMap;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(this.parameterMap.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return this.parameterMap.get(name);
    }

    private void parseMethod() {
        this.method = StrUtil.subBefore(this.requestString, " ", false);
    }

    private void parseContext() {
        Engine engine = service.getEngine();
        // 先根据uri直接获取context
        context = engine.getDefaultHost().getContext(uri);
        // 如果没有获取到
        if(null == context){
            // 取uri开始的/xxx/xxxx,中的xxx，再获取path为/xxx
            String path = StrUtil.subBetween(uri, "/", "/");
            path = path == null ? "/" : "/" + path;
            // 再根据path获取一次context
            context = engine.getDefaultHost().getContext(path);
            // 如果获取的还是空，则直接使用ROOT
            if(null == context)
                context = engine.getDefaultHost().getContext("/");
        }
    }

    /**
     * 根据requestString解析uri，如果没有问号，则从空格开始到第二个空格，否则到问号前
     */
    private void parseUri() {
        if(!StrUtil.isEmpty(this.requestString)){
            // 进行解析
            String temp = StrUtil.subBetween(this.requestString, " ", " ");
            // 如果有问号，则取问号之前的串
            temp = StrUtil.contains(temp, '?') ? StrUtil.subBefore(temp, "?", false) : temp;
            // 设置uri
            this.uri = temp;
        }
    }

    /**
     * 从连接socket中解析requestString
     *
     * @throws IOException IO异常
     * */
    private void parseHttpRequest() throws IOException{
        // 连接的输入流
        InputStream inputStream = this.socket.getInputStream();
        // 从输入流读取请求体
        byte[] bytes = MiniBrowser.readBytes(inputStream, false);
        // 转化为String，编码为utf-8
        this.requestString = new String(bytes, "utf-8");
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    public String getRequestString() {
        return requestString;
    }

    public Context getContext() {
        return context;
    }

    public String getMethod() {
        return this.method;
    }

    @Override
    public String getRealPath(String path) {
        return this.context.getServletContext().getRealPath(path);
    }

    @Override
    public ServletContext getServletContext() {
        return this.context.getServletContext();
    }

    @Override
    public String getLocalAddr() {
        return socket.getLocalAddress().getHostAddress();
    }

    @Override
    public String getLocalName() {
        return socket.getLocalAddress().getHostName();
    }

    @Override
    public int getLocalPort() {
        return socket.getLocalPort();
    }

    @Override
    public String getRemoteAddr() {
        InetSocketAddress isa = (InetSocketAddress)socket.getRemoteSocketAddress();
        String temp = isa.getAddress().toString();
        return StrUtil.subAfter(temp, "/", false);
    }
    @Override
    public String getRemoteHost() {
        InetSocketAddress isa = (InetSocketAddress)socket.getRemoteSocketAddress();
        return isa.getHostName();
    }
    @Override
    public int getRemotePort() {
        return socket.getPort();
    }

    @Override
    public String getScheme() {
        return "http";
    }

    @Override
    public String getServerName() {
        return this.getHeader("host").trim();
    }

    @Override
    public int getServerPort() {
        return this.getLocalPort();
    }

    @Override
    public String getContextPath() {
        String path = this.context.getPath();
        if("/".equals(path)){
            return "";
        }
        return path;
    }

    @Override
    public String getRequestURI() {
        return this.uri;
    }

    @Override
    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer();
        String scheme = getScheme();
        int port = getServerPort();
        if(port < 0){
            port = 80;
        }
        url.append(scheme);
        url.append("://");
        url.append(getServerName());
        if((scheme.equals("http") && (port !=80)) || (scheme.equals("https") && (port!=443))){
            url.append(".");
            url.append(port);
        }
        url.append(getRequestURI());
        return url;
    }

    @Override
    public String getServletPath() {
        return uri;
    }
}
