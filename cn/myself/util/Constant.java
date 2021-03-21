package cn.myself.util;


import cn.hutool.system.SystemUtil;

import java.io.File;

/**
 * 常量类
 */
public class Constant {

    public final static int CODE_200 = 200;
    public final static int CODE_302 = 302;
    public final static int CODE_404 = 404;
    public final static int CODE_500 = 500;

    // 请求成功头信息
    public final static String RESPONSE_HEAD_200 =
            "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: {}{}\r\n\r\n";

    // 请求成功带压缩的头信息
    public final static String RESPONSE_HEAD_200_GZIP =
            "HTTP/1.1 200 OK\r\n" +
                "Content-Type: {}{}\r\n" +
                "Content-Encoding:gzip\r\n\r\n";

    // 302请求头信息
    public final static String RESPONSE_HEAD_302 =
            "HTTP/1.1 302 Found\r\nLocation:{}\r\n\r\n";

    // 404信息头
    public final static String RESPONSE_HEAD_404 = "HTTP/1.1 404 Not Found\r\nContent-Type: text/html\r\n\r\n";

    // 404页面格式
    public static final String TEXTFORMAT_404 =
            "<!doctype html><html lang=\"en\"><head><title>HTTP Status 404 - Not Found</title>" +
                    "<style type=\"text/css\">h1 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:22px;} " +
                    "h2 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:16px;} h3 {font-family:Tahoma," +
                    "Arial,sans-serif;color:white;background-color:#525D76;font-size:14px;} body {font-family:Tahoma,Arial,sans-serif;color:" +
                    "black;background-color:white;} b {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;} p {font-family:" +
                    "Tahoma,Arial,sans-serif;background:white;color:black;font-size:12px;} a {color:black;} a.name {color:black;} .line {height:1px;" +
                    "background-color:#525D76;border:none;}</style></head><body><h1>HTTP Status 404 - Not Found {}</h1><hr class=\"line\" /><p><b>Type</b>" +
                    " Status Report</p><p><b>Message</b> &#47;{}</p><p><b>Description</b> The origin server did not find a current representation for" +
                    " the target resource or is not willing to disclose that one exists.</p><hr class=\"line\" /><h3>MyTomcat 1.0.1</h3></body></html>";

    // 500信息头
    public final static String RESPONSE_HEAD_500 = "HTTP/1.1 500 Internal Server Error\r\nContent-Type: text/html\r\n\r\n";

    // 500错误页面格式
    public final static String TEXTFORMAT_500 =
            "<!doctype html><html lang=\"en\"><head><title>HTTP Status 500 - Internal Server Error</title>" +
                    "<style type=\"text/css\">h1 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:22px;} " +
                    "h2 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:16px;} h3 {font-family:Tahoma," +
                    "Arial,sans-serif;color:white;background-color:#525D76;font-size:14px;} body {font-family:Tahoma,Arial,sans-serif;color:" +
                    "black;background-color:white;} b {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;} p {font-family:" +
                    "Tahoma,Arial,sans-serif;background:white;color:black;font-size:12px;} a {color:black;} a.name {color:black;} .line {height:1px;" +
                    "background-color:#525D76;border:none;}</style></head><body><h1>HTTP Status 500 - An exception occurred processing {}</h1><hr class=\"line\" /><p><b>Type</b>" +
                    " Exception Report</p><p><b>Message</b> &#47;<u>An exception occurred processing{}</></p><p><b>Description</b> " +
                    " <u>The server encountered an internal error that prevented it from fulfilling this request.</u></p>" +
                    "<p>Stacktrace:</p><pre>{}</pre><hr class=\"line\" /><h3>MyTomcat 1.0.1</h3></body></html>";

    // webapp的文件夹路径常量
    public final static File webappsFolder = new File(SystemUtil.get("user.dir"), "webapps");
    // ROOT文件夹对象
    public final static File rootFolder = new File(webappsFolder, "ROOT");

    // 配置文件所在的目录
    public final static File confFolder = new File(SystemUtil.get("user.dir"), "conf");
    // server.xml文件
    public final static File serverXMLFile = new File(confFolder, "server.xml");
    // web.xml文件
    public final static File webXMLFile = new File(confFolder, "web.xml");
    // context.xml文件
    public final static File contextXMLFile = new File(confFolder, "context.xml");
    // 工作的路径work
    public final static File workFolder = new File(SystemUtil.get("user.dir") + File.separator + "work");
}
