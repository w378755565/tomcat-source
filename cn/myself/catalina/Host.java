package cn.myself.catalina;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import cn.myself.util.Constant;
import cn.myself.util.ServerXMLUtil;
import cn.myself.watcher.WarFileWatcher;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Host {

    private String name;
    // Context映射表
    private Map<String, Context> contextMap;
    private Engine engine;

    public Host(String name, Engine engine) {
        this.contextMap = new HashMap<>();
        // 获取Host的Name
        this.name = name;
        this.engine = engine;

        // 加载contextMap
        scanContextsOnWebAppsFolder();
        // 读取XML中的Context
        loadServerXMLContexts();
        // 扫描所有的war文件
        scanWarOnWebAppsFolder();
        // 开启监听器进行监听
        new WarFileWatcher(this).start();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    /**
     * 根据Server.xml获取Context对象
     */
    private void loadServerXMLContexts() {
        List<Context> contexts = ServerXMLUtil.getContexts(this);
        for (Context context : contexts) {
            this.contextMap.put(context.getPath(), context);
        }
    }

    /**
     * 读取所有webapps下面的应用文件放入到Map中
     */
    private void scanContextsOnWebAppsFolder() {
        // 获取webapps下面所有的文件和文件夹
        File[] folders = Constant.webappsFolder.listFiles();
        // 进行遍历
        for (File folder : folders) {
            if(folder.isDirectory()){
                // 如果是文件夹则加载Context
                load(folder);
            }
        }
    }

    /**
     * 将文件夹加载成为context
     *
     * @param folder
     */
    private void load(File folder){
        String path = folder.getName();
        path = "ROOT".equals(path) ? "/" : "/" + path;
        String docBase = folder.getAbsolutePath();
        Context context = new Context(path, docBase, this, true);
        contextMap.put(context.getPath(), context);
    }

    /**
     * 加载war文件
     *
     * @param warFile 文件名
     */
    public void loadWar(File warFile){
        String fileName = warFile.getName();
        String folderName = StrUtil.subBefore(fileName, ".", true);
        Context context = getContext("/" + folderName);
        if(null != context){
            return;
        }
        File folder = new File(Constant.webappsFolder, folderName);
        if(folder.exists())
            return;
        File tempWarFile = FileUtil.file(Constant.webappsFolder, folderName, fileName);
        File contextFolder = tempWarFile.getParentFile();
        contextFolder.mkdir();
        FileUtil.copyFile(warFile, tempWarFile);
        String command = "jar xvf " + fileName;
        Process p = RuntimeUtil.exec(null, contextFolder, command);
        try{
            p.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        tempWarFile.delete();
        load(contextFolder);
    }

    /**
     * 扫描web应用下的所有war文件
     */
    private void scanWarOnWebAppsFolder(){
        File folder = FileUtil.file(Constant.webappsFolder);
        File[] files = folder.listFiles();
        for (File file : files) {
            if(file.getName().toLowerCase().endsWith("war")){
                loadWar(file);
            }
        }
    }

    public Context getContext(String path){
        return contextMap.get(path);
    }

    /**
     * 重载context
     *
     * @param context 需要重新获取的context
     */
    public void reload(Context context) {
        LogFactory.get().info("Reloading Context with name [{}] has started", context.getPath());
        String path = context.getPath();
        String docBase = context.getDocBase();
        boolean reloadable = context.isReloadable();
        // 停止context
        context.stop();
        // 移除
        this.contextMap.remove(path);
        // 创建新的context
        Context newContext = new Context(path, docBase, this, reloadable);
        this.contextMap.put(path, newContext);
        // 打印日志
        LogFactory.get().info("Reloading Context with name [{}] has completed", context.getPath());
    }
}
