package cn.edu.ustc.protocol;


import cn.edu.ustc.config.Config;
import cn.edu.ustc.registry.NacosUtil;
import com.alibaba.nacos.api.exception.NacosException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.logging.LoggingHandler;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SerializerTest {
    @Test
    public void testJSONSerializer() throws ClassNotFoundException {
        byte[] bytes = Serializer.Algorithm.Json.serialize(String.class);
        Class<?> clazz = Class.forName(Serializer.Algorithm.Json.deserialize(String.class, bytes));
        System.out.println(clazz);
    }

    @Test
    public void testCodec() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel(new LoggingHandler(),new MyProtocolFrameDecoder(),new MessageCodec());

        RPCRequest sayHelloRequest = new RPCRequest("cn.edu.ustc.server.service.HelloService",
                "sayHello",
                String.class,
                new Class[]{String.class},
                new Object[]{"zhangsan"});
//        channel.writeOutbound(sayHelloRequest);
        ByteBuf buffer = channel.alloc().buffer();
        //魔数
        buffer.writeInt(0xCAFEBABE);
        //序列化算法
        Serializer.Algorithm serializerAlgorithm = Config.getSerializerAlgorithm();
        System.out.println(serializerAlgorithm);
        buffer.writeInt(serializerAlgorithm.ordinal());
        //消息类型
        buffer.writeInt(sayHelloRequest.getMessageType());
        //对象转为字节数组
        byte[] bytes = serializerAlgorithm.serialize(sayHelloRequest);
        //长度
        buffer.writeInt(bytes.length);
        //正文
        buffer.writeBytes(bytes);

        channel.writeInbound(buffer);

    }

    @Test
    public void testProtostuff() throws ClassNotFoundException {
        Serializer.Algorithm protobufSerializer = Serializer.Algorithm.Protobuf;
        RPCRequest sayHelloRequest = new RPCRequest("cn.edu.ustc.server.service.HelloService",
                "sayHello",
                String.class,
                new Class[]{String.class},
                new Object[]{"zhangsan"});
        byte[] bytes = protobufSerializer.serialize(sayHelloRequest);
        System.out.println(protobufSerializer);
        System.out.println(protobufSerializer.deserialize(RPCRequest.class, bytes));
    }

    @Test
    public void testNacosRegistry() throws NacosException, IOException {
        NacosUtil.registerService("testNacosRegistry", "127.0.0.1", 8080);
        System.in.read();
    }

}