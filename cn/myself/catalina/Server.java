package cn.myself.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.log.LogFactory;
import cn.hutool.system.SystemUtil;

import java.util.LinkedHashMap;
import java.util.Map;

public class Server {
    private Service service;

    public Server() {
        this.service = new Service(this);
    }

    // 服务启动
    public void start() {
        TimeInterval timer = DateUtil.timer();
        this.logJVM();
        this.service.start();
        LogFactory.get().info("Server startup in {} ms", timer.intervalMs());
    }

    private void logJVM() {
        Map<String, String> infos = new LinkedHashMap<>();
        infos.put("Server version", "My Tomcat/1.0.1");
        infos.put("Server built", "2021-1-12 19:02:23");
        infos.put("Server number", "1.0.1");
        infos.put("OS Name\t", SystemUtil.get("os.name"));
        infos.put("OS version", SystemUtil.get("os.version"));
        infos.put("Architecture", SystemUtil.get("os.arch"));
        infos.put("Java Home", SystemUtil.get("java.home"));
        infos.put("JVM version", SystemUtil.get("java.runtime.version"));
        infos.put("JVM Vendor", SystemUtil.get("java.vm.specification.vendor"));

        for (Map.Entry<String, String> entry : infos.entrySet()) {
            LogFactory.get().info(entry.getKey() + ":\t\t" + entry.getValue());
        }

    }
}
