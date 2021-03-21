package cn.myself.http;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 响应对象
 */
public class Response extends BaseResponse {
    // 字符串的缓存
    private StringWriter stringWriter;

    // Writer，为了完成response.getWriter().println()风格
    private PrintWriter writer;

    // mimi格式，默认为text/html
    private String contentType;

    // 保存二进制文件内容的byte数组
    private byte[] body;

    // 处理后的响应码
    private int status;

    // Cookie列表
    private List<Cookie> cookies;

    // 重定向的路径
    private String redirectPath;

    /**
     * 构造方法，关联writer和String字符串缓存
     */
    public Response() {
        this.stringWriter = new StringWriter();
        this.writer = new PrintWriter(stringWriter);
        this.cookies = new ArrayList<>();
        this.contentType = "text/html";
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public void addCookie(Cookie cookie) {
        this.cookies.add(cookie);
    }

    public String getRedirectPath() {
        return redirectPath;
    }

    public void sendRedirect(String redirectPath) {
        this.redirectPath = redirectPath;
    }

    /**
     * 获取头信息中的cookie信息
     *
     * @return cookie信息
     */
    public String getCookiesHeader(){
        if(null == this.cookies)
            return "";
        String pattern = "EEE, d MMM yyyy HH:mm:ss 'GMT'";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.ENGLISH);
        StringBuffer sb = new StringBuffer();
        for (Cookie cookie : this.cookies) {
            sb.append("\r\n");
            sb.append("Set-Cookie: ");
            System.out.println(cookie.getName() + "=" + cookie.getValue() + ";");
            sb.append(cookie.getName() + "=" + cookie.getValue() + ";");
            if(-1 != cookie.getMaxAge()){
                // -1表示永久
                sb.append("Expires=");
                Date now = new Date();
                Date expire = DateUtil.offset(now, DateField.MINUTE, cookie.getMaxAge());
                sb.append(sdf.format(expire));
                sb.append(";");
            }
            if(null != cookie.getPath()){
                sb.append("Path=" + cookie.getPath());
            }
        }
        return sb.toString();
    }

    /**
     * 获取PrintWriter对象，写入响应体到String缓冲区
     *
     * @return PrintWriter对象
     */
    public PrintWriter getWriter() {
        return writer;
    }

    /**
     * 获取消息体byte数组
     *
     * @return 返回byte数组
     * @throws IOException IO异常，读取错误
     */
    public byte[] getBody()throws IOException {
        if (this.body == null) {
            String content = this.stringWriter.toString();
            this.body = content.getBytes();
        }
        return this.body;
    }

    /**
     * 设置消息体
     *
     * @param body 二进制文件内容
     */
    public void setBody(byte[] body) {
        this.body = body;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void setStatus(int status) {
        this.status = status;
    }
}
