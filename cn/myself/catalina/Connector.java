package cn.myself.catalina;

import cn.hutool.log.LogFactory;
import cn.myself.http.Request;
import cn.myself.http.Response;
import cn.myself.util.ThreadPoolUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Connector类，用来实现多个端口的功能，所以实现Runnable接口
 */
public class Connector implements Runnable {

    // 连接的端口号
    private int port;

    // service
    private Service service;

    // 压缩参数
    private String compression;
    private int compressionMinSize;
    private String noCompressionUserAgents;
    private String compressableMimeType;

    public String getCompression() {
        return compression;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public int getCompressionMinSize() {
        return compressionMinSize;
    }

    public void setCompressionMinSize(int compressionMinSize) {
        this.compressionMinSize = compressionMinSize;
    }

    public String getNoCompressionUserAgents() {
        return noCompressionUserAgents;
    }

    public void setNoCompressionUserAgents(String noCompressionUserAgents) {
        this.noCompressionUserAgents = noCompressionUserAgents;
    }

    public String getCompressableMimeType() {
        return compressableMimeType;
    }

    public void setCompressableMimeType(String compressableMimeType) {
        this.compressableMimeType = compressableMimeType;
    }

    public Connector(Service service) {
        this.service = service;
    }

    public Service getService() {
        return service;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try {
            // web服务器的监听对象，监听服务
            ServerSocket ss = new ServerSocket(this.port);
            while (true) {
                // 客户端连接进入
                Socket s = ss.accept();
                // 监听到开个线程放入到线程池进行处理，然后继续监听下一个客户
                ThreadPoolUtil.run(() -> {
                    try {
                        // 请求对象
                        Request request = new Request(s, Connector.this);
                        // 打印结果
                        Response response = new Response();
                        HttpProcessor processor = new HttpProcessor();
                        processor.execute(s, request, response);

                    } catch (Exception e) {
                        LogFactory.get().error(e);
                        e.printStackTrace();
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 打印启动日志
     */
    public void init(){
        LogFactory.get().info("Initializing ProtocolHandler [http-bio-{}]", port);
    }

    /**
     * 开启线程
     */
    public void start(){
        LogFactory.get().info("Starting ProtocolHandler [http-bio-{}]", port);
        new Thread(this).start();
    }
}
