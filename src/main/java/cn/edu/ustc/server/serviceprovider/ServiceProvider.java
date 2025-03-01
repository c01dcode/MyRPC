package cn.edu.ustc.server.serviceprovider;

import java.util.HashMap;
import java.util.Map;

//临时充当注册中心
public class ServiceProvider {
    private static Map<String,Class<?>> map = new HashMap<String,Class<?>>();
    public static void addService(String serviceName, Class<?> service) {
        map.put(serviceName, service);
    }
    public static Class<?> getService(String serviceName) {
        return map.get(serviceName);
    }
}
