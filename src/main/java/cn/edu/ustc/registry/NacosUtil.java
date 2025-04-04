package cn.edu.ustc.registry;

import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.pojo.Instance;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
public class NacosUtil {
    private static final String SERVER_ADDR = "192.168.191.128:8848";
    private static NamingService namingService;
    static {
        try {
            namingService = NamingFactory.createNamingService(SERVER_ADDR);
        } catch (NacosException e) {
            log.error("{}", e.getMessage());
            throw new RuntimeException("无法连接Nacos");
        }
    }

    //服务注册
    public static void registerService(String serviceName, String ip, int port) throws NacosException {
        Instance instance = new Instance();
        instance.setIp(ip);
        instance.setPort(port);
        namingService.registerInstance(serviceName, instance);
    }


    //获取实例
    public static List<Instance> getInstances(String serviceName) throws NacosException {
        return namingService.getAllInstances(serviceName);
    }


}
