package cn.myself.http;

import cn.myself.catalina.Context;

import java.io.File;
import java.util.*;

/**
 * ApplicationContext，最大的容器，一个应用有一个ApplicationContext
 */
public class ApplicationContext extends BaseServletContext {

    // 用来存储数据
    private Map<String, Object> attributesMap;

    // 使用的应用
    private Context context;

    public ApplicationContext(Context context){
        this.attributesMap = new HashMap<>();
        this.context = context;
    }

    @Override
    public void removeAttribute(String s) {
        this.attributesMap.remove(s);
    }

    @Override
    public void setAttribute(String s, Object o) {
        this.attributesMap.put(s, o);
    }

    @Override
    public Object getAttribute(String s) {
        return this.attributesMap.get(s);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        Set<String> keySet = this.attributesMap.keySet();
        return Collections.enumeration(keySet);
    }

    @Override
    public String getRealPath(String path) {
        return new File(this.context.getDocBase(), path).getAbsolutePath();
    }
}
