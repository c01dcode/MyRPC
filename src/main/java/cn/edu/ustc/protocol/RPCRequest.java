package cn.edu.ustc.protocol;

import lombok.Data;

import java.util.UUID;

@Data
public class RPCRequest extends MyProtocolMessage{
    //请求生成唯一id，响应应保持同一id
    public RPCRequest(String interfaceName, String methodName, Class<?> returnType, Class[] parameterTypes, Object[] parameterValue) {
        super.setId(UUID.randomUUID());
        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
        this.parameterValue = parameterValue;
    }

    private String interfaceName;
    /**
     * 调用接口中的方法名
     */
    private String methodName;
    /**
     * 方法返回类型
     */
    private Class<?> returnType;
    /**
     * 方法参数类型数组
     */
    private Class[] parameterTypes;
    /**
     * 方法参数值数组
     */
    private Object[] parameterValue;

    @Override
    public int getMessageType() {
        return RPCRequest;
    }
}
