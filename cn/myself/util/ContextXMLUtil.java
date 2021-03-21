package cn.myself.util;

import cn.hutool.core.io.FileUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * 读取context.xml文件
 */
public class ContextXMLUtil {
    public static String getWatchedResource(){
        try {
            String xml = FileUtil.readUtf8String(Constant.contextXMLFile);
            Document document = Jsoup.parse(xml);

            Element watchedResource = document.select("WatchedResource").first();
            return watchedResource.text();
        }catch (Exception e){
            return "WEB-INF/web.xml";
        }

    }
}
