package cn.myself.servlet;

import cn.hutool.core.util.ReflectUtil;
import cn.myself.catalina.Context;
import cn.myself.classloader.WebAppClassLoader;
import cn.myself.http.Request;
import cn.myself.http.Response;
import cn.myself.util.Constant;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 用来处理所有的servlet请求
 */
public class InvokerServlet extends HttpServlet {

    // 单例模式
    private static InvokerServlet instance = new InvokerServlet();

    private InvokerServlet() {
    }

    public static InvokerServlet getInstance() {
        return instance;
    }

    /**
     * 处理servlet请求
     *
     * @param httpServletRequest  请求参数
     * @param httpServletResponse 响应数据
     */
    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        Request request = (Request) httpServletRequest;
        Response response = (Response) httpServletResponse;
        Context context = request.getContext();
        // 根据uri获取servlet名称
        String servletClassName = context.getServletClassName(request.getUri());
        // 使用类加载器进行类的实例化
        try{
            // 根据servlet名称获取类对象
            Class<?> servletClass = context.getWebAppClassLoader().loadClass(servletClassName);
            // 使用类对象获取servlet对象
            Object servletObject = context.getServlet(servletClass);
            // 调用servlet中的service方法，service通过getMethod方法判断请求的方法进行相应的处理
            ReflectUtil.invoke(servletObject, "service", request, response);
            if(response.getRedirectPath() != null){
                response.setStatus(Constant.CODE_302);
            }else{
                response.setStatus(Constant.CODE_200);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (ServletException e) {
            e.printStackTrace();
        }
    }
}
