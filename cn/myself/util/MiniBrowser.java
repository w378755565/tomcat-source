package cn.myself.util;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 进行测试的小型浏览器
 * 能够获取响应头、响应体
 */
public class MiniBrowser {

    public static void main(String[] args) {
        String url = "https://www.baidu.com/";
        // 获取响应信息
        String contentString = getContentString(url, false);
        System.out.println(contentString);
        String httpString = getHttpString(url, false);
        System.out.println(httpString);
    }

    public static byte[] getContentBytes(String url){
        return getContentBytes(url, false);
    }

    public static String getContentString(String url){
        return getContentString(url, false);
    }

    /**
     * 将响应的消息获取成为字符串
     *
     * @param url 访问的路径
     * @param gzip
     * @return
     */
    public static String getContentString(String url, boolean gzip) {
        byte[] result = getContentBytes(url, gzip);
        if(null == result){
            return null;
        }
        try {
            return new String(result, "utf-8").trim();
        }catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    /**
     * 获取响应体的byte数组
     *
     * @param url
     * @param gzip
     * @return
     */
    public static byte[] getContentBytes(String url, boolean gzip) {
        byte[] response = getHttpBytes(url, gzip);
        byte[] doubleReturn = "\r\n\r\n".getBytes();
        int pos = -1;
        for (int i = 0; i < response.length - doubleReturn.length; i++) {
            byte[] temp = Arrays.copyOfRange(response, i, i + doubleReturn.length);
            if(Arrays.equals(temp, doubleReturn)){
                pos = i;
                break;
            }
        }
        if(-1 == pos){
            return null;
        }
        pos += doubleReturn.length;
        byte[] result = Arrays.copyOfRange(response, pos, response.length);
        return result;
    }

    public static String getHttpString(String url, boolean gzip) {
        byte[] bytes = getHttpBytes(url, gzip);
        return new String(bytes).trim();
    }

    public static String getHttpString(String url){
        return getHttpString(url, false);
    }


    /**
     * 获取响应的信息
     *
     * @param url 访问地址
     * @param gzip
     * @return 响应头 + 响应体
     */
    public static byte[] getHttpBytes(String url, boolean gzip){
        byte[] result = null;
        try {
            URL u = new URL(url);
            Socket client = new Socket();
            int port = u.getPort();
            if(-1 == port){
                port = 80;
            }
            InetSocketAddress inetSocketAddress = new InetSocketAddress(u.getHost(), port);
            client.connect(inetSocketAddress, 1000);
            Map<String, String> requestHeaders = new HashMap<>();

            requestHeaders.put("Host", u.getHost() + ":" + port);
            requestHeaders.put("Accept", "text/html");
            requestHeaders.put("Connection", "close");
            requestHeaders.put("User-Agent", "myself mini brower / java1.8");

            if(gzip){
                requestHeaders.put("Accept-Encoding", "gzip");
            }
            String path = u.getPath();
            if(path.length() == 0){
                path = "/";
            }
            String firstLine = "GET " + path + " HTTP/1.1\r\n";

            StringBuffer httpRequestString = new StringBuffer();
            httpRequestString.append(firstLine);
            Set<String> headers = requestHeaders.keySet();
            for (String header : headers) {
                String headerLine = header + ":" + requestHeaders.get(header) + "\r\n";
                httpRequestString.append(headerLine);
            }
            PrintWriter printWriter = new PrintWriter(client.getOutputStream(), true);
            printWriter.println(httpRequestString);
            InputStream is = client.getInputStream();
            result = readBytes(is, true);
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                result = e.toString().getBytes("utf-8");
            } catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 从输入流中读取结果到byte数组
     *
     * @param is 输入流
     * @return 数据的byte数组
     * @throws IOException IO异常
     */
    public static byte[] readBytes(InputStream is, boolean fully)throws IOException{
        int buffer_size = 1024;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte buffer[] = new byte[buffer_size];
        while(true) {
            int length = is.read(buffer);
            if(-1 == length){
                break;
            }
            baos.write(buffer, 0, length);
            if(!fully && length != buffer_size){
                break;
            }
        }
        return baos.toByteArray();
    }

}
