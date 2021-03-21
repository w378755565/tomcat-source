package cn.myself;

import cn.myself.classloader.CommonClassLoader;

import java.lang.reflect.Method;

public class Bootstrap {

    public static void main(String[] args) throws Exception {
        // 创建classloader
        CommonClassLoader classLoader = new CommonClassLoader();
        // 设置为当前线程的类加载器
        Thread.currentThread().setContextClassLoader(classLoader);
        // 得到Server对象
        String serverClassName = "cn.myself.catalina.Server";
        Class<?> serverClazz = classLoader.loadClass(serverClassName);
        Object serverObject = serverClazz.newInstance();
        Method startMethod = serverClazz.getMethod("start");
        // 调用start方法
        startMethod.invoke(serverObject);
        System.out.println(serverClazz);
    }
}
