package cn.myself.catalina;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.log.LogFactory;
import cn.myself.http.Request;
import cn.myself.http.Response;
import cn.myself.servlet.DefaultServlet;
import cn.myself.servlet.InvokerServlet;
import cn.myself.servlet.JspServlet;
import cn.myself.util.ApplicationFilterChain;
import cn.myself.util.Constant;
import cn.myself.util.SessionManager;
import cn.myself.util.WebXMLUtil;
import cn.myself.webappservlet.HelloServlet;

import javax.servlet.Filter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

/**
 * 处理请求的类
 */
public class HttpProcessor {

    /**
     * 处理请求
     *
     * @param socket   用户连接socket
     * @param request  请求对象
     * @param response 返回对象
     */
    public void execute(Socket socket, Request request, Response response) {
        try {
            this.prepareSession(request,response);
            // 处理响应
            String uri = request.getUri();
            Context context = request.getContext();
            if (null == uri) {
                return;
            }
            // 从context中获取servlet，如果获取不到，则以普通文本处理，否则调用servlet中的doGet方法
            String servletClassName = context.getServletClassName(uri);

            // 执行的servlet
            HttpServlet workingServlet;
            if (null != servletClassName) {
                workingServlet = InvokerServlet.getInstance();
            } else if(uri.endsWith(".jsp")){
                workingServlet = JspServlet.getInstance();
            }else {
                workingServlet = DefaultServlet.getInstance();
            }
            // 获取过滤器执行链
            List<Filter> filters = request.getContext().getMatchedFilters(uri);
            ApplicationFilterChain filterChain = new ApplicationFilterChain(filters, workingServlet);
            filterChain.doFilter(request, response);

            // 已经转发了，则不需要继续处理了
            if(request.isForwarded()){
                return;
            }

            if(Constant.CODE_200 == response.getStatus()){
                this.handle200(socket,request, response);
            } else if(Constant.CODE_302 == response.getStatus()){
                this.handle302(socket, response);
            } else if(Constant.CODE_404 == response.getStatus()){
                this.handle404(socket, uri);
            }
        } catch (Exception e) {
            LogFactory.get().error(e);
            // 响应500
            try {
                this.handle500(socket, e);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handle302(Socket socket, Response response) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        String redirectPath = response.getRedirectPath();
        String head_text = Constant.RESPONSE_HEAD_302;
        String header = StrUtil.format(head_text, redirectPath);
        byte[] responseBytes = header.getBytes("utf-8");
        outputStream.write(responseBytes);
    }

    private boolean isGzip(Request request, byte[] body, String mimeType){
        String acceptEncoding = request.getHeader("Accept-Encoding");
        if(!StrUtil.containsAny(acceptEncoding, "gzip")){
            return false;
        }
        Connector connector = request.getConnector();
        if(mimeType.contains(";")){
            mimeType = StrUtil.subBefore(mimeType, ";", false);
        }
        // 不是开启状态，则不进行压缩
        if(!"on".equals(connector.getCompression())){
            return false;
        }
        // 规格没到需要压缩的大小不进行压缩
        if(body.length < connector.getCompressionMinSize()){
            return false;
        }
        // 获取配置的不进行压缩的浏览器
        String userAgents = connector.getNoCompressionUserAgents();
        String[] eachUserAgents = userAgents.split(",");
        for (String eachUserAgent : eachUserAgents) {
            // 获取头信息，判断是否包含
            eachUserAgent = eachUserAgent.trim();
            String userAgent = request.getHeader("User-Agent");
            if(StrUtil.containsAny(userAgent, eachUserAgent))
                return false;
        }
        // 判断mimeType是否为可以压缩的类型
        String mimeTypes = connector.getCompressableMimeType();
        String[] eachMimeTypes = mimeTypes.split(",");
        for (String eachMimeType : eachMimeTypes) {
            if(mimeType.equals(eachMimeType)){
                return true;
            }
        }
        return false;
    }


    /**
     * 准备一个session
     *
     * @param request 请求
     * @param response 响应
     */
    private void prepareSession(Request request, Response response){
        String id = request.getSessionIdFromCookie();
        HttpSession session = SessionManager.getSession(id, request, response);
        request.setSession(session);
    }

    /**
     * 向客户端响应数据,200成功
     *
     * @param socket   连接socket
     * @param response 响应对象
     */
    private void handle200(Socket socket,Request request, Response response) throws IOException {
        // 设置contentType
        String contentType = response.getContentType();
        // 获取头信息中的cookie，将cookie信息写入到头信息中
        String cookiesHeader = response.getCookiesHeader();
        // 响应体
        byte[] body = response.getBody();
        // 查看数据是否需要压缩
        boolean gzip = isGzip(request, body, contentType);
        System.out.println("gzip::::::::::::" + gzip);
        // 根据是否压缩进行设置头信息
        String headText = gzip ? Constant.RESPONSE_HEAD_200_GZIP : Constant.RESPONSE_HEAD_200;
        // 根据格式获取头信息
        headText = StrUtil.format(headText, contentType, cookiesHeader);
        // 响应头
        byte[] head = headText.getBytes();
        // 进行压缩
        body = gzip ? ZipUtil.gzip(body) : body;
        // 进行复制
        byte[] responseBody = new byte[head.length + body.length];
        ArrayUtil.copy(head, 0, responseBody, 0, head.length);
        ArrayUtil.copy(body, 0, responseBody, head.length, body.length);
        // 写入到输出流
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(responseBody);
    }

    /**
     * 404操作
     *
     * @param socket 连接socket
     * @param uri    响应的路径
     * @throws IOException IO异常
     */
    protected void handle404(Socket socket, String uri) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        String responseText = StrUtil.format(Constant.TEXTFORMAT_404, uri, uri);
        responseText = Constant.RESPONSE_HEAD_404 + responseText;
        // 转化为byte数组写入
        byte[] responseByte = responseText.getBytes("utf-8");
        outputStream.write(responseByte);
    }

    /**
     * 500响应操作
     *
     * @param socket 连接socket
     * @param e      异常对象
     * @throws IOException IO异常
     */
    protected void handle500(Socket socket, Exception e) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        // 异常栈
        StackTraceElement[] stackTrace = e.getStackTrace();
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(e.toString());
        stringBuffer.append("\r\n");
        // 加入各个异常信息
        for (StackTraceElement element : stackTrace) {
            stringBuffer.append("\t");
            stringBuffer.append(element.toString());
            stringBuffer.append("\r\n");
        }
        // 异常消息
        String message = e.getMessage();
        if (null != message && message.length() > 20) {
            message = message.substring(0, 19);
        }
        // 将消息格式化得到响应体，最后写入输出流
        String responseText = StrUtil.format(Constant.TEXTFORMAT_500, message, e.toString(), stringBuffer.toString());
        responseText = Constant.RESPONSE_HEAD_500 + responseText;
        // 转化为byte数组写入
        byte[] responseByte = responseText.getBytes("utf-8");
        outputStream.write(responseByte);
    }
}
