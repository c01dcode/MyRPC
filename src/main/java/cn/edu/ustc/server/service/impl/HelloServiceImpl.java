package cn.edu.ustc.server.service.impl;

import cn.edu.ustc.server.annotation.RPCService;
import cn.edu.ustc.server.service.HelloService;

@RPCService
public class HelloServiceImpl implements HelloService {
    @Override
    public String sayHello(String name) {
        return "Hello " + name;
    }
}
