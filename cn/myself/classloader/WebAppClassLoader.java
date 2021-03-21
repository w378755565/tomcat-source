package cn.myself.classloader;

import cn.hutool.core.io.FileUtil;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * 加载web应用中的类的类加载器
 */
public class WebAppClassLoader extends URLClassLoader {

    /**
     * 加载web应用下面的WEB-INF的classes和lib下面的类
     *
     * @param docBase 应用的绝对路径
     * @param commonClassLoader 公共类的加载器
     */
    public WebAppClassLoader(String docBase, ClassLoader commonClassLoader) {
        super(new URL[]{}, commonClassLoader);
        try{
            // WEB-INF文件夹
            File webInfFolder = new File(docBase, "WEB-INF");
            // classes文件夹
            File classesFolder = new File(webInfFolder, "classes");
            // lib文件夹
            File libFolder = new File(webInfFolder, "lib");
            // classes文件夹的URL
            URL url;
            url = new URL("file:" + classesFolder.getAbsolutePath() + "/");
            // 添加url
            this.addURL(url);
            // 对lib下面的jar包进行处理
            List<File> jarFiles = FileUtil.loopFiles(libFolder);
            for (File jarFile : jarFiles) {
                url = new URL("file:" + jarFile.getAbsolutePath() + "/");
                this.addURL(url);
            }


        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
