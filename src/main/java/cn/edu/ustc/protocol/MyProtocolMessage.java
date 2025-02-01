package cn.edu.ustc.protocol;

import cn.edu.ustc.config.Config;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/*
    自定义传输协议
    魔数   序列化算法 消息类型 正文长度 正文
    4B    4B       4B      4B
*/
@Data
public abstract class MyProtocolMessage implements Serializable {
    private int serializerType;
    private UUID id;
    public MyProtocolMessage() {
        serializerType = Config.getSerializerAlgorithm().ordinal();
    }
    public abstract int getMessageType();

    protected static final int RPCRequest = 0;
    protected static final int RPCReponse = 1;
}
