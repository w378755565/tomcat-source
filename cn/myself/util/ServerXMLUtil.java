package cn.myself.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.myself.catalina.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * 读取Service.xml文件
 */
public class ServerXMLUtil {

    /**
     * 根据service获取Connector列表
     *
     * @param service service对象
     * @return connector列表
     */
    public static List<Connector> getConnectors(Service service){
        List<Connector> result = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.serverXMLFile);
        Document document = Jsoup.parse(xml);
        Elements elements = document.select("Connector");
        for (Element element : elements) {
            Integer port = Convert.toInt(element.attr("port"));
            String compression = element.attr("compression");
            Integer compressionMinSize = Convert.toInt(element.attr("compressionMinSize"), 0);
            String noCompressionUserAgents = element.attr("noCompressionUserAgents");
            String compressableMimeType = element.attr("compressableMimeType");
            Connector connector = new Connector(service);
            connector.setPort(port);
            connector.setCompression(compression);
            connector.setCompressionMinSize(compressionMinSize);
            connector.setNoCompressionUserAgents(noCompressionUserAgents);
            connector.setCompressableMimeType(compressableMimeType);
            result.add(connector);
        }
        return result;
    }


    /**
     * 读取获取context对象
     *
     * @return context对象集合
     */
    public static List<Context> getContexts(Host host){
        List<Context> result = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.serverXMLFile);
        Document document = Jsoup.parse(xml);
        Elements elements = document.select("Context");
        for (Element element : elements) {
            String path = element.attr("path");
            String docBase = element.attr("docBase");
            boolean reloadable = Convert.toBool(element.attr("reloadable"), true);
            Context context = new Context(path, docBase,host , reloadable);
            result.add(context);
        }
        return result;
    }

    /**
     * 获取HostName，第一个Name
     *
     * @return 第一个Host的name
     */
    public static String getHostName(){
        String xml = FileUtil.readUtf8String(Constant.serverXMLFile);
        Document document = Jsoup.parse(xml);

        Element host = document.select("Host").first();
        return host.attr("name");
    }

    /**
     * 获取第一个Engine的defaultHost
     *
     * @return 第一个Engine的defaultHost名称
     */
    public static String getEngineDefaultHost(){
        String xml = FileUtil.readUtf8String(Constant.serverXMLFile);
        Document document = Jsoup.parse(xml);

        Element engine = document.select("Engine").first();
        return engine.attr("defaultHost");
    }

    /**
     * 获取当前engine下面所有的host
     *
     * @param engine engine对象，获取其下面的所有host
     * @return Host列表
     */
    public static List<Host> getHosts(Engine engine){
        List<Host> result = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.serverXMLFile);
        Document document = Jsoup.parse(xml);
        Elements elements = document.select("Host");
        for (Element element : elements) {
            String name = element.attr("name");
            Host host = new Host(name, engine);
            result.add(host);
        }
        return result;
    }

    /**
     * 获取Service的name属性
     *
     * @return Service的name属性
     */
    public static String getServiceName(){
        String xml = FileUtil.readUtf8String(Constant.serverXMLFile);
        Document document = Jsoup.parse(xml);

        Element service = document.select("Service").first();
        return service.attr("name");
    }

}
