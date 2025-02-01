package cn.edu.ustc.protocol;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class MyProtocolFrameDecoder extends LengthFieldBasedFrameDecoder {
    //自定义传输协议
    //魔数   序列化算法 消息类型 正文长度 正文
    //4B    4B       4B      4B
    public MyProtocolFrameDecoder() {
        super(1024, 12, 4);
    }
}
