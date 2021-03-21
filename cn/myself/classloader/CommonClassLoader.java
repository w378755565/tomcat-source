package cn.myself.classloader;

import cn.hutool.system.SystemUtil;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * 公共类加载器，加载lib下的jar、catalina中的类（后面做）
 */
public class CommonClassLoader extends URLClassLoader {

    public CommonClassLoader() {
        // 父类构造方法m
        super(new URL[]{});

        try {
            // 加载lib下面的所有jar
            File libFolder = new File(System.getProperty("user.dir"), "lib");
            File[] jars = libFolder.listFiles();
            // 遍历所有的jar文件，加入URL中
            for (File jar : jars) {
                if (jar.getName().endsWith("jar")) {
                    URL url = new URL("file:" + jar.getAbsolutePath());
                    this.addURL(url);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }
}
