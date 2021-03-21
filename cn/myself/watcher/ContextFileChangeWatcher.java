package cn.myself.watcher;

import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import cn.hutool.log.LogFactory;
import cn.myself.catalina.Context;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

/**
 * 监听应用的servlet文件改变的监听器
 */
public class ContextFileChangeWatcher {

    // 监听器
    private WatchMonitor monitor;

    // 是否停止监听
    private boolean stop = false;

    /**
     * 构造函数，对context的应用文件进行监听
     *
     * @param context 应用对象
     */
    public ContextFileChangeWatcher(Context context){
        this.monitor = WatchUtil.createAll(context.getDocBase(), Integer.MAX_VALUE, new Watcher() {
            /**
             * 当文件发生变化时
             *
             * @param event 发生的事件
             */
            private void dealWith(WatchEvent<?> event) {
                synchronized (ContextFileChangeWatcher.class){
                    // 获取文件名
                    String fileName = event.context().toString();
                    // 如果已经处理，则返回
                    if(stop){
                        return;
                    }
                    if(fileName.endsWith("jar") || fileName.endsWith("class") || fileName.endsWith("xml")){
                        stop = true;
                        // 重新加载
                        LogFactory.get().info(ContextFileChangeWatcher.this + "检测到了Web应用下的文件变化{}" + fileName);
                        context.reload();
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
