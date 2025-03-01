package cn.edu.ustc.client.loadbalance;

import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalancer implements LoadBalancer {
    private AtomicInteger index = new AtomicInteger(0);
    @Override
    public Instance select(List<Instance> instances) {
        return instances.get(index.updateAndGet(i -> i == instances.size() - 1 ? 0 : i + 1));
    }
}
