package cn.edu.ustc.server.serviceprovider;

import cn.edu.ustc.config.Config;
import cn.edu.ustc.registry.NacosUtil;
import cn.edu.ustc.server.annotation.RPCService;
import com.alibaba.nacos.api.exception.NacosException;
import org.reflections.Reflections;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ServiceProvider {
    private static Map<String,Class<?>> map = new HashMap<String,Class<?>>();
    public static void addService(String serviceName, Class<?> service) {
        map.put(serviceName, service);
    }
    public static Class<?> getService(String serviceName) {
        return map.get(serviceName);
    }

    public static void scanAndRegisterServices(String packageName) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(RPCService.class);
        for (Class<?> clazz : classes) {
            Class<?>[] interfaces = clazz.getInterfaces();
            //对所有实现了的接口注册服务
            for (Class<?> anInterface : interfaces) {
                try {
                    NacosUtil.registerService(anInterface.getCanonicalName(), Config.getServerIP(), Config.getServerPort());
                } catch (NacosException e) {
                    throw new RuntimeException(e);
                }
                //保存接口-实现类信息
                ServiceProvider.addService(anInterface.getCanonicalName(), clazz);
            }
        }
    }
}
