package cn.myself.servlet;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.myself.catalina.Context;
import cn.myself.http.Request;
import cn.myself.http.Response;
import cn.myself.util.Constant;
import cn.myself.util.WebXMLUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * 用来处理静态资源的Servlet
 */
public class DefaultServlet extends HttpServlet {

    // 单例模式
    private static DefaultServlet instance = new DefaultServlet();
    private DefaultServlet(){}
    public static DefaultServlet getInstance(){
        return instance;
    }

    /**
     * 处理servlet请求
     *
     * @param httpServletRequest 请求参数
     * @param httpServletResponse 响应数据
     */
    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)throws IOException, ServletException {
        Request request = (Request)httpServletRequest;
        Response response = (Response)httpServletResponse;
        String uri = request.getUri();
        Context context = request.getContext();
        // 如果访问的是根路径，则表示访问文本，否则访问文件
        if ("/".equals(uri)) {
            uri = WebXMLUtil.getWelcomeFile(request.getContext());
            // 如果uri以jsp结尾则交给jspservlet处理
            if(uri.endsWith("jsp")){
                JspServlet.getInstance().service(request, response);
            }
        }
        // 访问文件的文件名.扩展名
        String fileName = StrUtil.removePrefix(uri, "/");
        // 创建文件对象
        File file = new File(request.getRealPath(fileName));
        if (file.exists()) {
            // 获取文件的扩展名
            String extName = FileUtil.extName(file);
            // 获取mimeType
            String mimeType = WebXMLUtil.getMimeType(extName);
            // 设置
            response.setContentType(mimeType);
            // 读取信息，写入到返回数据中
            // 设置body
            byte[] body = FileUtil.readBytes(file);
            response.setBody(body);
            response.setStatus(Constant.CODE_200);
        }else{
            response.setStatus(Constant.CODE_404);
        }
    }

}
