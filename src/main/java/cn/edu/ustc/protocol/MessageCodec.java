package cn.edu.ustc.protocol;

import cn.edu.ustc.config.Config;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.List;

//消息编解码器
//ByteBuf ———— MyProtocolMessage
/*
    自定义传输协议
    魔数   序列化算法 消息类型 正文长度 正文
    4B    4B       4B      4B
*/
@Data
@ChannelHandler.Sharable
@Slf4j
public class MessageCodec extends MessageToMessageCodec<ByteBuf, MyProtocolMessage> {
    @Override
    protected void encode(ChannelHandlerContext ctx, MyProtocolMessage msg, List<Object> list) throws Exception {
        ByteBuf buffer = ctx.alloc().buffer();
        //魔数
        buffer.writeInt(0xCAFEBABE);
        //序列化算法
        Serializer.Algorithm serializerAlgorithm = Config.getSerializerAlgorithm();
        buffer.writeInt(serializerAlgorithm.ordinal());
        //消息类型
        buffer.writeInt(msg.getMessageType());
        //对象转为字节数组
        byte[] bytes = serializerAlgorithm.serialize(msg);
        //长度
        buffer.writeInt(bytes.length);
        //正文
        buffer.writeBytes(bytes);
        list.add(buffer);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) throws Exception {
        int magic = byteBuf.readInt();
        if(magic != 0xCAFEBABE)
            throw new RuntimeException("不支持的协议包");
        int serializerType = byteBuf.readInt();
        int messageType = byteBuf.readInt();
        int length = byteBuf.readInt();
        byte[] bytes = new byte[length];
        byteBuf.readBytes(bytes, 0, length);
        //根据数字获取序列化算法
        Serializer.Algorithm serializer = Config.getSerializerAlgorithm().values()[serializerType];
        //根据数字获取实际消息类型0/1分别表示请求、响应
        Class<?> clazz = null;
        if(messageType == 0)
            clazz = RPCRequest.class;
        else if(messageType == 1)
            clazz = RPCResponse.class;
        else
            throw new RuntimeException("没有该类型的的请求");
        MyProtocolMessage msg = (MyProtocolMessage)serializer.deserialize(clazz, bytes);
        list.add(msg);
        log.debug("{} {} {} {}",msg.getSerializerType(),msg.getMessageType(),length,msg.getClass().getName());
    }
}
