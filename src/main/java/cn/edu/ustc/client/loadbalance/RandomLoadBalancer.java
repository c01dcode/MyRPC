package cn.edu.ustc.client.loadbalance;

import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.List;
import java.util.Random;

public class RandomLoadBalancer implements LoadBalancer {
    @Override
    public Instance select(List<Instance> instances) {
        int n = instances.size();
        return instances.get(new Random().nextInt(n));
    }
}
