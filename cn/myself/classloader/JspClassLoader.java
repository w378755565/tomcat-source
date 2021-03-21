package cn.myself.classloader;

import cn.hutool.core.util.StrUtil;
import cn.myself.catalina.Context;
import cn.myself.util.Constant;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * 加载jsp的servlet类的加载器
 */
public class JspClassLoader extends URLClassLoader {

    // 映射表 jsp文件和JspClassLoader的映射
    private static Map<String, JspClassLoader> map = new HashMap<>();

    public JspClassLoader(Context context){
        super(new URL[]{}, context.getWebAppClassLoader());
        try{
            // 将context应用中的work的jsp的class全部加载到类加载器中
            String path = context.getPath();
            String subFolder = "/".equals(path) ? "_" : StrUtil.subAfter(path, "/", false);
            File classesFolder = new File(Constant.workFolder, subFolder);
            URL url = new URL("file:" + classesFolder.getAbsolutePath() + "/");
            this.addURL(url);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 将jsp和类加载器脱离关系invalidJspClassLoader
     *
     * @param uri
     * @param context
     */
    public static void invalidJspClassLoader(String uri, Context context){
        String key = context.getPath() + "/" + uri;
        map.remove(key);
    }

    /**
     * 获取类加载器
     *
     * @param uri
     * @param context
     */
    public static JspClassLoader getJspClassLoader(String uri, Context context){
        String key = context.getPath() + "/" + uri;
        JspClassLoader loader = map.get(key);
        if(null == loader){
            loader = new JspClassLoader(context);
            map.put(key, loader);
        }
        return loader;
    }

}
