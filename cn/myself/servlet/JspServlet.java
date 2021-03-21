package cn.myself.servlet;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.myself.catalina.Context;
import cn.myself.classloader.JspClassLoader;
import cn.myself.http.Request;
import cn.myself.http.Response;
import cn.myself.util.Constant;
import cn.myself.util.JspUtil;
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
public class JspServlet extends HttpServlet {

    // 单例模式
    private static JspServlet instance = new JspServlet();
    private JspServlet(){}
    public static JspServlet getInstance(){
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
        String path = context.getPath();
        // 如果访问的是根路径，则表示访问文本，否则访问文件
        if ("/".equals(uri)) {
            uri = WebXMLUtil.getWelcomeFile(request.getContext());
        }
        // 访问文件的文件名.扩展名
        String fileName = StrUtil.removePrefix(uri, "/");
        // 创建文件对象
        File jspFile = new File(request.getRealPath(fileName));

        // 将jsp文件编译生成一下class文件
        // 访问根目录，则为_,其他目录正常
        String subFolder = "/".equals(path) ? "_" : StrUtil.subAfter(path, "/", false);
        // 获取编译的servlet最终的路径work下面的
        String servletClassPath = JspUtil.getServletClassPath(uri, subFolder);
        File jspServletClassFile = new File(servletClassPath);
        // 如果servlet的class文件不存在，表示未编译过，或者servlet编译了但是jsp文件进行了修改，则进行编译
        if(!jspServletClassFile.exists()){
            JspUtil.compileJsp(context, jspFile);
        }else if(jspServletClassFile.lastModified() < jspFile.lastModified()){
            // 重新生成了class
            JspUtil.compileJsp(context, jspFile);
            // 将之前的jsp和class解除关联
            JspClassLoader.invalidJspClassLoader(uri, context);
        }

        if (jspFile.exists()) {
            // 获取文件的扩展名
            String extName = FileUtil.extName(jspFile);
            // 获取mimeType
            String mimeType = WebXMLUtil.getMimeType(extName);
            // 设置
            response.setContentType(mimeType);
            // 使用jsp的类加载器加载jsp的servlet类，加载后运行service方法
            JspClassLoader jspClassLoader = JspClassLoader.getJspClassLoader(uri, context);
            String jspServletClassName = JspUtil.getJspServletClassName(uri, subFolder);
            // 类加载器加载类
            try {
                Class<?> jspServletClass = jspClassLoader.loadClass(jspServletClassName);
                // 进行单例处理
                HttpServlet servlet = context.getServlet(jspServletClass);
                servlet.service(request, response);
            } catch (ClassNotFoundException e) {
                response.setStatus(Constant.CODE_500);
            }
            if(response.getRedirectPath() != null){
                response.setStatus(Constant.CODE_302);
            }else{
                response.setStatus(Constant.CODE_200);
            }
        }else{
            response.setStatus(Constant.CODE_404);
        }
    }

}
