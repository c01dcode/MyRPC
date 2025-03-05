package cn.edu.ustc.server.handler;

import cn.edu.ustc.protocol.RPCRequest;
import cn.edu.ustc.protocol.RPCResponse;
import cn.edu.ustc.server.serviceprovider.ServiceProvider;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ChannelHandler.Sharable
@Slf4j
public class RPCRequestHandler extends SimpleChannelInboundHandler<RPCRequest> {
    //具体业务处理线程池
    private static final ExecutorService LOCAL_BUSINESS_HANDLER = Executors.newFixedThreadPool(2);

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RPCRequest rpcRequest) throws Exception {
        LOCAL_BUSINESS_HANDLER.submit(() -> {
            RPCResponse rpcResponse = new RPCResponse(rpcRequest);
            rpcResponse.setId(rpcRequest.getId());
            try {
                String interfaceName = rpcRequest.getInterfaceName();
                String methodName = rpcRequest.getMethodName();
                Class<?>[] parameterTypes = rpcRequest.getParameterTypes();
                Object[] parameterValue = rpcRequest.getParameterValue();
                Class<?> service = ServiceProvider.getService(interfaceName);
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
        });
    }
}
