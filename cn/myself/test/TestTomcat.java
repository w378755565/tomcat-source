package cn.myself.test;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.util.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.myself.util.MiniBrowser;
import com.sun.corba.se.spi.ior.ObjectKey;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestTomcat {

    private static int port = 18080;

    private static String ip = "127.0.0.1";

    @BeforeClass
    public static void beforeClass(){
        // 所有测试开始前，保证tomcat已经启动
        if(NetUtil.isUsableLocalPort(port)){
            System.err.println("请先启动端口为：" + port + "的tomcat");
            System.exit(1);
        }else{
            System.out.println("检测到已经启动服务器，开始测试");
        }
    }

    @Test
    public void testHelloTomcat(){
        String html = getContentString("/");
        Assert.assertEquals(html, "Hello My Tomcat");
    }

    @Test
    public void testFileTomcat(){
        String html = getContentString("/file.html");
        Assert.assertEquals(html, "this is a html file,it have been accessed!!");
    }

    @Test
    public void testTimeConsumeHtml() throws InterruptedException {
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(20, 20, 60, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(10));
        TimeInterval timer = DateUtil.timer();
        for (int i = 0; i < 3; i++) {
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    getContentString("/TimeConsume.html");
                }
            });
        }
        threadPool.shutdown();
        threadPool.awaitTermination(1, TimeUnit.HOURS);
        long duration = timer.interval();
        System.out.println(duration);
        Assert.assertTrue(duration < 3000);
    }

    @Test
    public void testServerXMLContext(){
        String html = getContentString("/b/1.html");
        Assert.assertEquals(html, "1dqwf b下面的文件!!!");
    }

    @Test
    public void test404(){
        String response = getHttpString("/not_exist.html");
        containAssert(response, "HTTP/1.1 404 Not Found");
    }

    @Test
    public void testaHtml(){
        String response = getHttpString("/a");
        containAssert(response, "a下面的index.html");
    }

    @Test
    public void testbHtml(){
        String response = getHttpString("/b");
        containAssert(response, "b下面的index.html");
    }

    @Test
    public void testaTxt(){
        String response = getHttpString("/a.txt");
        containAssert(response, "Content-Type: text/plain");
    }

    @Test
    public void testHelloServlet(){
        String html = getContentString("/j2ee/hello");
        Assert.assertEquals(html, "Hello My Tomcat Servlet");
    }

    @Test
    public void testJavaWebServlet(){
        String html = getContentString("/javaweb/hello");
        containAssert(html, "Hello My Tomcat Servlet @javaWeb HelloServlet");
    }

    @Test
    public void testJavaWebServletSingleton(){
        String html1 = getContentString("/javaweb/hello");
        String html2 = getContentString("/javaweb/hello");
        Assert.assertEquals(html1, html2);
    }

    @Test
    public void testsetCookie(){
        String html = getHttpString("/javaweb/setCookie");
        System.out.println(html);
        containAssert(html, "Set-Cookie: name=TEST(Cookie);Expires=");
    }


    private String getContentString(String uri){
        String url = StrUtil.format("http://{}:{}{}", ip, port, uri);
        return MiniBrowser.getContentString(url);
    }

    private String getHttpString(String uri){
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        String http = MiniBrowser.getHttpString(url);
        return http;
    }

    private void containAssert(String html, String string) {
        boolean match = StrUtil.containsAny(html, string);
        Assert.assertTrue(match);
    }



}
