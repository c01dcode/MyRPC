package cn.edu.ustc.server.service;

import java.util.HashMap;
import java.util.Map;

//临时充当注册中心
public class RegisterCenter {
    private static Map<String,Class<?>> map = new HashMap<String,Class<?>>();
    static {
        map.put("cn.edu.ustc.server.service.HelloService",HelloServiceImpl.class);
    }
    public static void register(String serviceName, Class<?> service) {
        map.put(serviceName, service);
    }
    public static Class<?> getService(String serviceName) {
        return map.get(serviceName);
    }
}
