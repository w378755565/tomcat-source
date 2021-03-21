package cn.myself.catalina;

import cn.myself.util.ServerXMLUtil;

import java.util.List;

public class Engine {
    private String defaultHost;
    // engine下的host列表
    private List<Host> hosts;

    private Service service;

    public Engine(Service service) {
        // 读取默认的defaultHost
        this.defaultHost = ServerXMLUtil.getEngineDefaultHost();
        // 获取所有的host
        this.hosts = ServerXMLUtil.getHosts(this);
        this.service = service;
        checkDefault();
    }

    /**
     * 验证默认host是否存在
     */
    private void checkDefault() {
        if(null == getDefaultHost()){
            throw new RuntimeException("this defaultHost " + defaultHost +" does not exist");
        }
    }

    public Host getDefaultHost() {
        for (Host host : this.hosts) {
            if(host.getName().equals(this.defaultHost)){
                return host;
            }
        }
        return null;
    }

    public List<Host> getHosts() {
        return hosts;
    }

}
