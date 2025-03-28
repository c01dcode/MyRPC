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
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
public class RPCClient {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        RPCClient rpcClient = new RPCClient();
        //基于动态代理的同步调用
//        HelloService helloService = rpcClient.getProxyService(HelloService.class);
//        System.out.println(helloService.sayHello("zhangsan"));
//        System.out.println(helloService.sayHello("lisi"));

        // 获取异步代理
        AsyncProxy<HelloService> asyncProxy = rpcClient.getAsyncProxy(HelloService.class);
        
        // 异步调用方法
        CompletableFuture<Object> future1 = asyncProxy.invoke("sayHello", "user1");
        CompletableFuture<Object> future2 = asyncProxy.invoke("sayHello", "user2");
        
        // 组合多个异步调用
        CompletableFuture.allOf(future1, future2)
                .thenRun(() -> log.debug("所有调用完成"));
    }

    //RPC客户端参数
    private String ip;
    private int port;
    private LoadBalancer loadBalancer;

    public RPCClient() {
        //spi读取负载均衡实现类
        ServiceLoader<LoadBalancer> loadBalancers = ServiceLoader.load(LoadBalancer.class);
        for (LoadBalancer loadBalancer : loadBalancers) {
            this.loadBalancer = loadBalancer;
        }
        if (this.loadBalancer == null) {
            //默认采用随机负载均衡
            this.loadBalancer = new RandomLoadBalancer();
        }
    }

    //获取单例channel
    private volatile Channel channel = null;
    private Object lock = new Object();
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

    //默认基于动态代理的同步调用
    private <T> T getProxyService(Class<T> serviceClass) {
        //服务发现
        findService(serviceClass.getCanonicalName());
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
                    CompletableFuture<Object> resquestFuture = new CompletableFuture<>();
                    RPCResponseHandler.map.put(request.getId(), resquestFuture);
                    getChannel().writeAndFlush(request);
                    //同步调用模式等待返回结果
                    try {
                        return resquestFuture.get();
                    } catch (ExecutionException e) {
                        throw new RuntimeException("远程调用失败");
                    }
                }
        );

        return (T) proxyInstance;
    }

    // 获取异步代理对象
    public <T> AsyncProxy<T> getAsyncProxy(Class<T> serviceClass) {
        return new AsyncProxy<>(serviceClass, this);
    }
    
    // 新增 AsyncProxy 内部类
    public class AsyncProxy<T> {
        private final Class<T> serviceClass;
        private final RPCClient rpcClient;

        public AsyncProxy(Class<T> serviceClass, RPCClient rpcClient) {
            this.serviceClass = serviceClass;
            this.rpcClient = rpcClient;
        }

        public CompletableFuture<Object> invoke(String methodName, Object... args) throws InterruptedException {
            // 通过反射获取方法参数类型
            Class<?>[] parameterTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                parameterTypes[i] = args[i].getClass();
            }
            
            // 查找对应方法获取返回值类型
            Class<?> returnType;
            try {
                returnType = serviceClass.getMethod(methodName, parameterTypes).getReturnType();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("方法不存在", e);
            }
            
            RPCRequest rpcRequest = new RPCRequest(
                            serviceClass.getName(),
                            methodName,
                            returnType,
                            parameterTypes,
                            args
                        );
            findService(rpcRequest.getInterfaceName());
            CompletableFuture<Object> resquestFuture = new CompletableFuture<>();
            RPCResponseHandler.map.put(rpcRequest.getId(), resquestFuture);
            getChannel().writeAndFlush(rpcRequest);
            //异步调用直接返回future
            return resquestFuture;
        }
    }

    //服务发现
    private <T> void findService(String serviceName) {
        List<Instance> instances;
        try {
            instances = NacosUtil.getInstances(serviceName);
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
        if (instances == null)
            throw new RuntimeException("未找到服务");
        Instance instance = loadBalancer.select(instances);
        ip = instance.getIp();
        port = instance.getPort();
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
