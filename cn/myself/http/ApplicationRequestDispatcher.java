package cn.myself.http;

import cn.myself.catalina.HttpProcessor;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * 完成请求转发的功能
 */
public class ApplicationRequestDispatcher implements RequestDispatcher {

    private String uri;

    public ApplicationRequestDispatcher(String uri) {
        this.uri = uri.startsWith("/") ? uri : "/" + uri;
    }

    /**
     * 转发请求，重新执行processer中的execute方法
     *
     * @param servletRequest
     * @param servletResponse
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public void forward(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        Request request = (Request)servletRequest;
        Response response = (Response)servletResponse;

        request.setUri(this.uri);
        HttpProcessor processor = new HttpProcessor();
        processor.execute(request.getSocket(), request, response);
        // 是否已经执行完转发，防止死循环
        request.setForwarded(true);
    }

    @Override
    public void include(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {

    }
}
