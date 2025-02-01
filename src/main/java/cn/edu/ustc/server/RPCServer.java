package cn.edu.ustc.server;

import cn.edu.ustc.protocol.MessageCodec;
import cn.edu.ustc.protocol.MyProtocolFrameDecoder;
import cn.edu.ustc.server.handler.RPCRequestHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.logging.LoggingHandler;

public class RPCServer {
    public static void main(String[] args) {
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
