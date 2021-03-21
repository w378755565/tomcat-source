package cn.myself.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.log.LogFactory;
import cn.myself.util.ServerXMLUtil;

import java.util.List;

/**
 * Service类，内只有一个Engine，与Engine一对一
 */
public class Service {
    private String name;
    private Engine engine;
    private Server server;

    private List<Connector> connectors;

    public Service(Server server) {
        this.server = server;
        // 读取service的name
        this.name = ServerXMLUtil.getServiceName();
        // 直接new一个engine
        this.engine = new Engine(this);
        // 读取所有的Connector
        this.connectors = ServerXMLUtil.getConnectors(this);
    }

    public Engine getEngine() {
        return engine;
    }

    public Server getServer() {
        return server;
    }

    public void start(){
        init();
    }

    private void init(){
        TimeInterval timer = DateUtil.timer();
        // 打印日志
        for (Connector connector : this.connectors) {
            // 启动日志
            connector.init();
        }
        LogFactory.get().info("Initialization processed in {} ms", timer.intervalMs());
        // 将所有的Connector启动
        for (Connector connector : this.connectors) {
            // 启动
            connector.start();
        }
    }
}
