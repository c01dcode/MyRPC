package cn.edu.ustc.client.handler;

import cn.edu.ustc.protocol.RPCResponse;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@ChannelHandler.Sharable
@Slf4j
public class RPCResponseHandler extends SimpleChannelInboundHandler<RPCResponse> {
    public static final Map<UUID, CompletableFuture<Object>> map = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RPCResponse rpcResponse) throws Exception {
        CompletableFuture<Object> completableFuture = map.remove(rpcResponse.getId());
        if(completableFuture != null) {
            Exception exceptionValue = rpcResponse.getExceptionValue();
            if(exceptionValue != null) {
                completableFuture.completeExceptionally(exceptionValue);
            }else{
                completableFuture.complete(rpcResponse.getReturnValue());
            }
        }
    }
}
