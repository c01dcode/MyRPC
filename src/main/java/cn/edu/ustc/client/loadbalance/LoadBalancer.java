package cn.edu.ustc.client.loadbalance;

import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.List;

@FunctionalInterface
public interface LoadBalancer {
    public Instance select(List<Instance> instances);
}
