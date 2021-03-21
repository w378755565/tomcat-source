package cn.myself.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import cn.myself.classloader.WebAppClassLoader;
import cn.myself.exception.WebConfigDuplicatedException;
import cn.myself.http.ApplicationContext;
import cn.myself.http.StandardFilterConfig;
import cn.myself.http.StandardServletConfig;
import cn.myself.util.ContextXMLUtil;
import cn.myself.watcher.ContextFileChangeWatcher;
import org.apache.jasper.JspC;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 表示一个应用的信息，路径、绝对路径
 */
public class Context {
    // 路径，其实就是文件夹的名称
    private String path;
    // 服务器的绝对路径
    private String docBase;
    // 当前应用下的web.xml文件对象
    private File contextWebXmlFile;
    // webAppClassLoader初始化时加载web应用中的lib和类
    private WebAppClassLoader webAppClassLoader;
    // Host对象
    private Host host;
    // 是否为热加载
    private boolean reloadable;
    // 监听对象
    private ContextFileChangeWatcher contextFileChangeWatcher;
    // ApplicationContext用于存储数据
    private ServletContext servletContext;
    // servlet的池，为了满足单例
    private Map<Class<?>, HttpServlet> servletPool;
    // filter的池，为了满足单例
    private Map<String, Filter> filterPool;

    // 监听器列表
    private List<ServletContextListener> listeners;

    // 自启动的servlet类名
    private List<String> loadOnStartUpServletClassNames;

    // servlet的配置信息映射以及参数列表的映射表
    private Map<String, String> url_servletClassName;
    private Map<String, String> url_servletName;
    private Map<String, String> servletName_className;
    private Map<String, String> className_servletName;
    // servlet className类名---参数表
    private Map<String, Map<String, String>> servlet_className_init_params;

    // filter的配置信息映射以及参数列表的映射表
    private Map<String, List<String>> url_filterClassName;
    private Map<String, List<String>> url_filterName;
    private Map<String, String> filterName_className;
    private Map<String, String> className_filterName;
    // filter className类名---参数表
    private Map<String, Map<String, String>> filter_className_init_params;

    public Context(String path, String docBase, Host host, boolean reloadable) {
        this.path = path;
        this.docBase = docBase;
        this.host = host;
        this.reloadable = reloadable;
        // 初始化ApplicationContext
        this.servletContext = new ApplicationContext(this);
        // 初始化servlet池
        this.servletPool = new HashMap<>();
        this.filterPool = new HashMap<>();
        // 初始化其自动类名称列表
        this.loadOnStartUpServletClassNames = new ArrayList<>();

        // 设置web.xml文件对象
        this.contextWebXmlFile = new File(docBase, ContextXMLUtil.getWatchedResource());
        // 初始化webAppClassLoader
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        this.webAppClassLoader = new WebAppClassLoader(docBase, contextClassLoader);

        // 初始化map对象
        this.url_servletClassName = new HashMap<>();
        this.url_servletName = new HashMap<>();
        this.servletName_className = new HashMap<>();
        this.className_servletName = new HashMap<>();
        // 初始化参数列表
        this.servlet_className_init_params = new HashMap<>();

        // 初始化map对象
        this.url_filterClassName = new HashMap<>();
        this.url_filterName = new HashMap<>();
        this.filterName_className = new HashMap<>();
        this.className_filterName = new HashMap<>();
        // 初始化参数列表
        this.filter_className_init_params = new HashMap<>();

        // 初始化监听器列表
        this.listeners = new ArrayList<>();

        this.deploy();
    }

    private void deploy() {
        // 加载所有的监听器
        loadListeners();
        TimeInterval timer = DateUtil.timer();
        LogFactory.get().info("Deploying web application directory {}", this.docBase);
        // 初始化servlet映射表
        this.init();
        // 如果是热部署，则设置监听器
        if (this.reloadable) {
            this.contextFileChangeWatcher = new ContextFileChangeWatcher(this);
            this.contextFileChangeWatcher.start();
        }
        JspC c = new JspC();
        new JspRuntimeContext(servletContext, c);
        LogFactory.get().info("Deploying of web application directory {} has finished in {} ms", this.docBase, timer.interval());
    }
    /**
     * 初始化，如果有web.xml就读取servlet配置
     */
    private void init() {
        // 初始化时调用初始化方法
        this.fireEvent("init");
        if (this.contextWebXmlFile.exists()) {
            try {
                checkDuplicated();
            } catch (WebConfigDuplicatedException e) {
                e.printStackTrace();
            }
            // 读取web.xml文件
            String xml = FileUtil.readUtf8String(this.contextWebXmlFile);
            Document document = Jsoup.parse(xml);
            // 初始化servlet四个Map对象
            this.parseServletMapping(document);
            // 初始化servlet配置参数列表
            this.parseServletInitParams(document);
            // 初始化Filter四个Map对象
            this.parseFilterMapping(document);
            // 初始化Filter配置参数列表
            this.parseFilterInitParams(document);
            // 设置自启动类
            this.parseServletOnStartUp(document);
            this.handleServletOnStartUp();
            // 启动filter
            this.initFilter();
        }
    }

    /**
     * 进行匹配路径
     *
     * @param pattern 匹配路径
     * @param uri 访问的uri
     * @return 是否匹配成功
     */
    private boolean match(String pattern,String uri){
        // 完成匹配
        if(StrUtil.equals(pattern, uri)){
            return true;
        }
        // /*模式，匹配，如果匹配的路径为/*则直接返回true
        if(StrUtil.equals(pattern, "/*")){
            return true;
        }
        // 后缀名 /*.jsp
        if(StrUtil.equals(pattern, "/*.")){
            // 匹配的后缀
            String patternExtName = StrUtil.subAfter(pattern, ".", false);
            // 请求的后缀
            String uriExtName = StrUtil.subAfter(uri, ".", false);
            // 如果相等，则true
            if(StrUtil.equals(patternExtName, uriExtName)){
                return true;
            }
        }
        return false;
    }
    public void addListener(ServletContextListener listener){
        this.listeners.add(listener);
    }

    // 初始化listener列表
    private void loadListeners(){
        try{
            if(!this.contextWebXmlFile.exists()){
                return;
            }
            String xml = FileUtil.readUtf8String(this.contextWebXmlFile);
            Document document = Jsoup.parse(xml);
            Elements listenerElements = document.select("listener listener-class");
            for (Element listenerElement : listenerElements) {
                String listenerClassName = listenerElement.text();
                // 加载对象
                Class<?> aClass = this.webAppClassLoader.loadClass(listenerClassName);
                ServletContextListener listener = (ServletContextListener) aClass.newInstance();
                this.addListener(listener);
            }
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
    }

    private void fireEvent(String type){
        ServletContextEvent event = new ServletContextEvent(this.servletContext);
        // 对事件进行处理
        for (ServletContextListener listener : this.listeners) {
            if("init".equals(type)){
                listener.contextInitialized(event);
            }
            if("destroy".equals(type)){
                listener.contextDestroyed(event);
            }
        }
    }

    /**
     * 匹配得到需要进行执行的过滤器列表
     *
     * @param uri 匹配的路径
     * @return 过滤器列表
     */
    public List<Filter> getMatchedFilters(String uri){
        List<Filter> filters = new ArrayList<>();
        // 匹配的串
        Set<String> patterns = this.url_filterClassName.keySet();
        Set<String> matchedPatterns = new HashSet<>();
        // 对串进行循环
        for (String pattern : patterns) {
            // 如果匹配上，则添加
            if(match(pattern, uri)){
                matchedPatterns.add(pattern);
            }
        }
        Set<String> matchedFilterClassNames = new HashSet<>();
        // 所有的匹配信息循环
        for (String pattern : matchedPatterns) {
            List<String> filterClassName = this.url_filterClassName.get(pattern);
            matchedFilterClassNames.addAll(filterClassName);
        }
        // 对匹配的类进行循环
        for (String filterClassName : matchedFilterClassNames) {
            System.out.println("filterClassName:::::::::" + filterClassName);
            Filter filter = this.filterPool.get(filterClassName);
            filters.add(filter);
        }
        return filters;
    }

    /**
     * 初始化过滤器列表，过滤器自动启动，初始化就直接加载，servlet只有初始化的类才会自动加载，其他的需要get
     *
     */
    private void initFilter() {
        try {
            for (Map.Entry<String, String> entry : className_filterName.entrySet()) {
                String className = entry.getKey();
                String filterName = entry.getValue();

                Class<?> clazz = this.webAppClassLoader.loadClass(className);
                Map<String, String> paramMap = filter_className_init_params.get(className);

                StandardFilterConfig filterConfig = new StandardFilterConfig(this.servletContext, paramMap, filterName);
                Filter filter = this.filterPool.get(filterName);
                if(null == filter){
                    // 反射创建对象
                    filter = (Filter) ReflectUtil.newInstance(clazz);
                    // 初始化
                    filter.init(filterConfig);
                    this.filterPool.put(className, filter);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * 读取filter配置到映射表中
     *
     * @param document web.xml文件的文档对象
     */
    private void parseFilterMapping(Document document) {
        // 得到filter-mapping中的url-pattern
        Elements mappingUrlElements = document.select("filter-mapping url-pattern");
        // 设置url_filterName
        for (Element mappingUrlElement : mappingUrlElements) {
            String urlPattern = mappingUrlElement.text();
            String filterName = mappingUrlElement.parent().select("filter-name").first().text();
            List<String> filterNames = this.url_filterName.get(urlPattern);
            if(null == filterNames){
                filterNames = new ArrayList<>();
                this.url_filterName.put(urlPattern, filterNames);
            }
            filterNames.add(filterName);
        }
        // 设置filterName_className className_filterName
        Elements filterNameElements = document.select("filter filter-name");
        for (Element filterNameElement : filterNameElements) {
            String filterName = filterNameElement.text();
            String filterClass = filterNameElement.parent().select("filter-class").first().text();
            this.filterName_className.put(filterName, filterClass);
            this.className_filterName.put(filterClass, filterName);
        }
        //设置url_filterClassName
        for (Map.Entry<String, List<String>> entry : this.url_filterName.entrySet()) {
            String url = entry.getKey();
            List<String> filterNames = entry.getValue();
            if(null == filterNames){
                filterNames = new ArrayList<>();
                this.url_filterName.put(url, filterNames);
            }
            for (String filterName : filterNames) {
                String filterClassName = this.filterName_className.get(filterName);
                List<String> filterClassNames = this.url_filterClassName.get(url);
                if(null == filterClassNames){
                    filterClassNames = new ArrayList<>();
                    this.url_filterClassName.put(url, filterClassNames);
                }
                filterClassNames.add(filterClassName);
            }
        }
    }

    /**
     * 解析filter初始化参数
     *
     * @param document 文档对象
     */
    private void parseFilterInitParams(Document document) {
        Elements filterClassElements = document.select("filter-class");
        for (Element filterClassElement : filterClassElements) {
            // filter的类名
            String filterClassName = filterClassElement.text();
            // 得到parent下面的参数
            Elements initParamsElements = filterClassElement.parent().select("init-param");
            if (!initParamsElements.isEmpty()) {
                // 如果有参数则读取
                Map<String, String> initParams = new HashMap<>();
                // 获取参数
                for (Element initParamsElement : initParamsElements) {
                    String paramName = initParamsElement.select("param-name").get(0).text();
                    String paramValue = initParamsElement.select("param-value").get(0).text();
                    initParams.put(paramName, paramValue);
                }
                this.filter_className_init_params.put(filterClassName, initParams);
            }
        }
    }


    /**
     * 解析自启动的servlet
     *
     * @param document 文档对象
     */
    private void parseServletOnStartUp(Document document) {
        Elements loadOnStartUpElements = document.select("load-on-startup");
        for (Element loadOnStartUpElement : loadOnStartUpElements) {
            // className
            String servletClassName = loadOnStartUpElement.parent().select("servlet-class").text();
            // 添加类名
            this.loadOnStartUpServletClassNames.add(servletClassName);
        }
    }
    /**
     * 启动实例化自启动的servlet
     */
    private void handleServletOnStartUp() {
        try {
            // 遍历所有的自启动类名
            for (String loadOnStartUpServletClassName : this.loadOnStartUpServletClassNames) {
                // 使用类加载器获取类对象
                Class<?> startUpClass = this.webAppClassLoader.loadClass(loadOnStartUpServletClassName);
                // 将servlet的对象加入map映射表
               this.getServlet(startUpClass);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 解析servlet初始化参数
     *
     * @param document 文档对象
     */
    private void parseServletInitParams(Document document) {
        Elements servletClassElements = document.select("servlet-class");
        for (Element servletClassElement : servletClassElements) {
            // servlet的类名
            String servletClassName = servletClassElement.text();
            // 得到parent下面的参数
            Elements initParamsElements = servletClassElement.parent().select("init-param");
            if (!initParamsElements.isEmpty()) {
                // 如果有参数则读取
                Map<String, String> initParams = new HashMap<>();
                // 获取参数
                for (Element initParamsElement : initParamsElements) {
                    String paramName = initParamsElement.select("param-name").get(0).text();
                    String paramValue = initParamsElement.select("param-value").get(0).text();
                    initParams.put(paramName, paramValue);
                }
                this.servlet_className_init_params.put(servletClassName, initParams);
            }
        }
    }


    public synchronized HttpServlet getServlet(Class<?> clazz) throws ServletException {
        // 尝试获取servlet对象
        HttpServlet httpServlet = this.servletPool.get(clazz);
        System.out.println("httpServlet ==============" + httpServlet);
        if (null == httpServlet) {
            // 如果不存在，则实例化并放入池中
            httpServlet = (HttpServlet) ReflectUtil.newInstance(clazz);
            // 获取类名
            String className = clazz.getName();
            String servletName = this.className_servletName.get(className);
            // 根据类名获取初始化参数
            Map<String, String> initParams = this.servlet_className_init_params.get(className);
            // 创建servletConfig
            StandardServletConfig servletConfig = new StandardServletConfig(this.servletContext, initParams, servletName);
            // 初始化servlet
            httpServlet.init(servletConfig);
            this.servletPool.put(clazz, httpServlet);
        }
        return httpServlet;
    }

    /**
     * 通过uri获取servlet
     *
     * @param uri uri
     * @return servlet的类名
     */
    public String getServletClassName(String uri) {
        return this.url_servletClassName.get(uri);
    }



    /**
     * 读取servlet配置到映射表中
     *
     * @param document web.xml文件的文档对象
     */
    private void parseServletMapping(Document document) {
        // 得到servlet-mapping中的url-pattern
        Elements mappingUrlElements = document.select("servlet-mapping url-pattern");
        // 设置url_servletName
        for (Element mappingUrlElement : mappingUrlElements) {
            String urlPattern = mappingUrlElement.text();
            String servletName = mappingUrlElement.parent().select("servlet-name").first().text();
            this.url_servletName.put(urlPattern, servletName);
        }
        // 设置servletName_className className_servletName
        Elements servletNameElements = document.select("servlet servlet-name");
        for (Element servletNameElement : servletNameElements) {
            String servletName = servletNameElement.text();
            String servletClass = servletNameElement.parent().select("servlet-class").first().text();
            this.servletName_className.put(servletName, servletClass);
            this.className_servletName.put(servletClass, servletName);
        }
        //设置url_servletClassName
        for (Map.Entry<String, String> entry : this.url_servletName.entrySet()) {
            // url
            String url = entry.getKey();
            String servletName = entry.getValue();
            // 根据servletName获取servletClassName
            String servletClassName = this.servletName_className.get(servletName);
            // 设置到url_servletClassName中
            this.url_servletClassName.put(url, servletClassName);
        }
    }

    /**
     * 检测web.xml中的servlet配置信息是否正确，即是否有重复的servlet配置
     *
     * @throws WebConfigDuplicatedException 配置错误的异常
     */
    private void checkDuplicated() throws WebConfigDuplicatedException {
        // 读取web.xml文件
        String xml = FileUtil.readUtf8String(this.contextWebXmlFile);
        Document document = Jsoup.parse(xml);

        // 分别检测servlet的name、class、url-pattern是否有重复的
        checkDuplicated(document, "servlet-mapping url-pattern", "servlet url 重复，请保持其唯一性：{}");
        checkDuplicated(document, "servlet servlet-name", "servlet 名称重复，请保持其唯一性：{}");
        checkDuplicated(document, "servlet servlet-class", "servlet 类名重复，请保持其唯一性：{}");
    }

    /**
     * 具体查看某个映射的配置是否有相同的，对所有的配置信息进行排序，如果有两个相邻的相同，则表示有相同的
     *
     * @param document web.xml读取的文档对象
     * @param mapping  需要检测的节点
     * @param desc     错误信息
     * @throws WebConfigDuplicatedException 有相同配置，报错
     */
    private void checkDuplicated(Document document, String mapping, String desc) throws WebConfigDuplicatedException {
        Elements elements = document.select(mapping);
        // 逻辑放入集合，对集合排序，查看相邻的是否相同
        List<String> contents = new ArrayList<>();
        for (Element element : elements) {
            contents.add(element.text());
        }
        Collections.sort(contents);
        for (int i = 0; i < contents.size() - 1; i++) {
            String contentPre = contents.get(i);
            String contentNext = contents.get(i + 1);
            if (contentPre.equals(contentNext)) {
                throw new WebConfigDuplicatedException(StrUtil.format(desc, contentPre));
            }
        }
    }


    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDocBase() {
        return docBase;
    }

    public void setDocBase(String docBase) {
        this.docBase = docBase;
    }

    public WebAppClassLoader getWebAppClassLoader() {
        return this.webAppClassLoader;
    }

    /**
     * 销毁所有的servlet
     */
    private void destroyServlets() {
        Collection<HttpServlet> servlets = servletPool.values();
        for (HttpServlet servlet : servlets) {
            servlet.destroy();
        }
    }

    public void stop() {
        this.fireEvent("destroy");
        try {
            this.webAppClassLoader.close();
            this.contextFileChangeWatcher.stop();
            // 销毁所有的servlet
            this.destroyServlets();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 重载自己
     */
    public void reload() {
        this.host.reload(this);
    }

    public boolean isReloadable() {
        return reloadable;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }
}
