package cn.edu.ustc.server;

import cn.edu.ustc.config.Config;
import cn.edu.ustc.protocol.MessageCodec;
import cn.edu.ustc.protocol.MyProtocolFrameDecoder;
import cn.edu.ustc.registry.NacosUtil;
import cn.edu.ustc.server.annotation.RPCService;
import cn.edu.ustc.server.handler.RPCRequestHandler;
import cn.edu.ustc.server.service.HelloService;
import cn.edu.ustc.server.serviceprovider.ServiceProvider;
import com.alibaba.nacos.api.exception.NacosException;
import io.netty.bootstrap.ServerBootstrap;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import org.reflections.Reflections;

import java.util.Set;

public class RPCServer {
    public static void main(String[] args){
        //扫描指定目录下的@RPCService并注册服务
        ServiceProvider.scanAndRegisterServices("cn.edu.ustc.server.service");

        LoggingHandler loggingHandler = new LoggingHandler();
        MessageCodec messageCodec = new MessageCodec();
        RPCRequestHandler rpcRequestHandler = new RPCRequestHandler();
        new ServerBootstrap().group(new NioEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new MyProtocolFrameDecoder());
                        ch.pipeline().addLast(loggingHandler);
                        ch.pipeline().addLast(messageCodec);
                        ch.pipeline().addLast(rpcRequestHandler);
                    }
                })
                .bind(8080);
    }
}
