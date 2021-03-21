package cn.myself.util;

import cn.hutool.core.io.FileUtil;
import cn.myself.catalina.Context;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class WebXMLUtil {

    // mimeType的映射表
    private static Map<String, String> mimeTypeMapping = new HashMap<>();

    /**
     * 根据扩展名获取mimeType
     *
     * @param extendName 文件扩展名
     * @return 返回mimeType
     */
    public static synchronized String getMimeType(String extendName){
        if(mimeTypeMapping.isEmpty()){
            initMimeType();
        }
        String mimeType = mimeTypeMapping.get(extendName);
        if(null == mimeType){
            return "text/html";
        }
        return mimeType;
    }

    /**
     * 初始化映射表，从xml里面读取
     */
    private static void initMimeType() {
        String xml = FileUtil.readUtf8String(Constant.webXMLFile);
        Document document = Jsoup.parse(xml);

        Elements elements = document.select("mime-mapping");
        for (Element element : elements) {
            String extension = element.select("extension").first().text();
            String mimeType = element.select("mime-type").first().text();
            mimeTypeMapping.put(extension, mimeType);
        }
    }

    /**
     * 根据Context获取下面的welcome文件路径，根据Context的绝对路径下面的welcome是否存在判断
     *
     * @param context 需要获取欢迎文件的context对象
     * @return welcome文件名index.xxx
     */
    public static String getWelcomeFile(Context context){
        String xml = FileUtil.readUtf8String(Constant.webXMLFile);
        Document document = Jsoup.parse(xml);
        Elements elements = document.select("welcome-file");
        for (Element element : elements) {
            File file = new File(context.getDocBase(), element.text());
            if(file.exists()){
                return file.getName();
            }
        }
        // 默认为index.html
        return "index.html";
    }



}
