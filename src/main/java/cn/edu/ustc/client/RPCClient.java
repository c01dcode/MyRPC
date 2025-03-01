package cn.edu.ustc.client;


import cn.edu.ustc.client.handler.RPCResponseHandler;
import cn.edu.ustc.client.loadbalance.LoadBalancer;
import cn.edu.ustc.client.loadbalance.RandomLoadBalancer;
import cn.edu.ustc.protocol.MessageCodec;
import cn.edu.ustc.protocol.MyProtocolFrameDecoder;
import cn.edu.ustc.protocol.RPCRequest;
import cn.edu.ustc.registry.NacosUtil;
import cn.edu.ustc.server.service.HelloService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.pojo.Instance;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultPromise;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Proxy;
import java.util.List;

@Slf4j
public class RPCClient {
    public static void main(String[] args) throws InterruptedException {
        RPCClient rpcClient = new RPCClient();
        HelloService helloService = rpcClient.getProxyService(HelloService.class);
        System.out.println(helloService.sayHello("zhangsan"));
        System.out.println(helloService.sayHello("lisi"));
    }

    private String ip;
    private int port;
    private LoadBalancer loadBalancer;

    public RPCClient() {
        this.loadBalancer = new RandomLoadBalancer();
    }

    public RPCClient(LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    private volatile Channel channel = null;
    private Object lock = new Object();
    //获取单例channel
    public Channel getChannel() throws InterruptedException {
        if(channel == null){
            synchronized (lock){
                if(channel == null){
                    initChannel();
                }
            }
        }
        return channel;
    }

    private <T> T getProxyService(Class<T> serviceClass) {
        //服务发现
        List<Instance> instances;
        try {
            instances = NacosUtil.getInstances(serviceClass.getCanonicalName());
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
        if (instances == null)
            throw new RuntimeException("未找到服务");
        Instance instance = loadBalancer.select(instances);
        ip = instance.getIp();
        port = instance.getPort();
        //动态代理
        ClassLoader classLoader = serviceClass.getClassLoader();
        Object proxyInstance = Proxy.newProxyInstance(
                classLoader,
                new Class[]{serviceClass},

                (proxy, method, args) -> {
                    //构造请求对象
                    RPCRequest request = new RPCRequest(serviceClass.getName(),
                            method.getName(),
                            method.getReturnType(),
                            method.getParameterTypes(),
                            args);
                    DefaultPromise promise = new DefaultPromise(getChannel().eventLoop());
                    RPCResponseHandler.map.put(request.getId(), promise);
                    getChannel().writeAndFlush(request);
                    promise.await();
                    if (promise.isSuccess()) {
                        return promise.getNow();
                    } else {
                        throw new RuntimeException("远程调用失败");
                    }
                }
        );
        return (T) proxyInstance;
    }

    // 获取channel
    private void initChannel() throws InterruptedException {
        NioEventLoopGroup group = new NioEventLoopGroup();
        LoggingHandler loggingHandler = new LoggingHandler();
        MessageCodec messageCodec = new MessageCodec();
        RPCResponseHandler rpcResponseHandler = new RPCResponseHandler();
        Channel channelInit = new Bootstrap().group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        //自定义协议 结合 基于长度字段的帧解码器 解决粘包半包
                        ch.pipeline().addLast(new MyProtocolFrameDecoder());
                        ch.pipeline().addLast(loggingHandler);
                        ch.pipeline().addLast(messageCodec);
                        ch.pipeline().addLast(rpcResponseHandler);
                    }
                })
                .connect(ip, port)
                .sync()
                .channel();
        channelInit.closeFuture().addListener(o -> group.shutdownGracefully());
        channel = channelInit;
    }
}
