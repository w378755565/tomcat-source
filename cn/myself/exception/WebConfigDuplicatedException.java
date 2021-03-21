package cn.myself.exception;

/**
 * 配置错误异常类
 */
public class WebConfigDuplicatedException extends Exception {
    public WebConfigDuplicatedException(String message){
        super(message);
    }
}
