package cn.edu.ustc.server.handler;

import cn.edu.ustc.protocol.RPCRequest;
import cn.edu.ustc.protocol.RPCResponse;
import cn.edu.ustc.server.service.RegisterCenter;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@ChannelHandler.Sharable
@Slf4j
public class RPCRequestHandler extends SimpleChannelInboundHandler<RPCRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RPCRequest rpcRequest) throws Exception {
        RPCResponse rpcResponse = new RPCResponse(rpcRequest);
        rpcResponse.setId(rpcRequest.getId());
        try {
            String interfaceName = rpcRequest.getInterfaceName();
            String methodName = rpcRequest.getMethodName();
            Class<?>[] parameterTypes = rpcRequest.getParameterTypes();
            Object[] parameterValue = rpcRequest.getParameterValue();
            //TODO 通过注册中心Nacos获取
            Class<?> service = RegisterCenter.getService(interfaceName);
            Method method = service.getMethod(methodName, parameterTypes);
            Object result = method.invoke(service.newInstance(), parameterValue);
            rpcResponse.setReturnValue(result);
            log.debug("远程调用{}的{}方法成功返回{}", interfaceName, methodName, result);
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getCause().getMessage();
            //ExceptionValue不为null时表示远程调用失败
            rpcResponse.setExceptionValue(new Exception("远程调用出错" + msg));
            log.debug("远程调用出错");
        }
        channelHandlerContext.writeAndFlush(rpcResponse);
    }
}
