package cn.myself.http;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Servlet配置类
 */
public class StandardServletConfig implements ServletConfig {

    // servlet的上下文ApplicationContext，内部就有context，context中有servlet
    private ServletContext servletContext;
    // 初始化参数
    private Map<String,String> initParameters;
    // servlet的名称
    private String servletName;

    public StandardServletConfig(ServletContext servletContext, Map<String, String> initParameters, String servletName) {
        this.servletContext = servletContext;
        this.initParameters = initParameters;
        this.servletName = servletName;
        if(null == this.initParameters){
            this.initParameters = new HashMap<>();
        }
    }

    @Override
    public String getServletName() {
        return this.servletName;
    }

    @Override
    public ServletContext getServletContext() {
        return this.servletContext;
    }

    @Override
    public String getInitParameter(String name) {
        return this.initParameters.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(this.initParameters.keySet());
    }
}
