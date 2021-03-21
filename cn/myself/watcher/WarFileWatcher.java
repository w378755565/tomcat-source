package cn.myself.watcher;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import cn.hutool.log.LogFactory;
import cn.myself.catalina.Context;
import cn.myself.catalina.Host;
import cn.myself.util.Constant;
import org.apache.tomcat.util.bcel.Const;

import java.io.File;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;

import static cn.hutool.core.io.watch.WatchMonitor.ENTRY_CREATE;

/**
 * 监听webapp下的war文件
 */
public class WarFileWatcher {

    // 监听器
    private WatchMonitor monitor;

    /**
     * 构造函数，对context的应用文件进行监听
     *
     * @param host host对象
     */
    public WarFileWatcher(Host host){
        this.monitor = WatchUtil.createAll(Constant.webappsFolder, 1, new Watcher() {
            /**
             * 当文件发生变化时
             *
             * @param event 发生的事件
             */
            private void dealWith(WatchEvent<?> event) {
                synchronized (WarFileWatcher.class){
                    String fileName = event.context().toString();
                    if(fileName.endsWith(".war") && ENTRY_CREATE.equals(event.kind())){
                        // 加载war文件
                        File warFile = FileUtil.file(Constant.webappsFolder, fileName);
                        host.loadWar(warFile);
                    }
                }
            }
            // 创建文件
            @Override
            public void onCreate(WatchEvent<?> event, Path path) {
                dealWith(event);
            }
            // 修改文件
            @Override
            public void onModify(WatchEvent<?> event, Path path) {
                dealWith(event);
            }
            // 删除文件
            @Override
            public void onDelete(WatchEvent<?> event, Path path) {
                dealWith(event);
            }
            // 溢出
            @Override
            public void onOverflow(WatchEvent<?> event, Path path) {
                dealWith(event);
            }
        });
    }

    public void start(){
        this.monitor.start();
    }

    public void stop(){
        this.monitor.close();
    }


}
