package cn.myself.http;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 过滤器参数类
 */
public class StandardFilterConfig implements FilterConfig {

    // servlet的上下文ApplicationContext，内部就有context，context中有servlet
    private ServletContext servletContext;
    // 初始化参数
    private Map<String,String> initParameters;
    // 过滤器名称
    private String filterName;

    public StandardFilterConfig(ServletContext servletContext, Map<String, String> initParameters, String filterName) {
        this.servletContext = servletContext;
        this.initParameters = initParameters;
        this.filterName = filterName;
        if(null == this.initParameters){
            this.initParameters = new HashMap<>();
        }
    }

    @Override
    public String getFilterName() {
        return this.filterName;
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
