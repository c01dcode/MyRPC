package cn.edu.ustc.protocol;

import com.google.gson.*;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 提高不同的序列化、反序列化算法
 * 0 Jdk 1 Json 2 Protobuf ...
 */
public interface Serializer {

    // 反序列化方法
    <T> T deserialize(Class<T> clazz, byte[] bytes);

    // 序列化方法
    <T> byte[] serialize(T object);

    enum Algorithm implements Serializer {
        //0
        Jdk {
            @Override
            public <T> T deserialize(Class<T> clazz, byte[] bytes) {
                try {
                    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
                    return (T) ois.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException("反序列化失败", e);
                }
            }

            @Override
            public <T> byte[] serialize(T object) {
                try {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(bos);
                    oos.writeObject(object);
                    return bos.toByteArray();
                } catch (IOException e) {
                    throw new RuntimeException("序列化失败", e);
                }
            }
        },
        //1
        Json {
            @Override
            public <T> T deserialize(Class<T> clazz, byte[] bytes) {
                Gson gson = new GsonBuilder().registerTypeAdapter(Class.class, new ClassCodec()).create();
                String json = new String(bytes, StandardCharsets.UTF_8);
                return gson.fromJson(json, clazz);
            }

            @Override
            public <T> byte[] serialize(T object) {
                Gson gson = new GsonBuilder().registerTypeAdapter(Class.class, new ClassCodec()).create();
                String json = gson.toJson(object);
                return json.getBytes(StandardCharsets.UTF_8);
            }

            //gson默认不支持对Class的序列化，可以通过自定义转换器来解决
            class ClassCodec implements JsonSerializer<Class<?>>, JsonDeserializer<Class<?>> {

                @Override
                public JsonElement serialize(Class<?> src, Type typeOfSrc, JsonSerializationContext context) {
                    // class -> json
                    // 只需要传输全类名
                    return new JsonPrimitive(src.getName());
                }

                //根据类名获取Class
                @Override
                public Class<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                    try {
                        String str = json.getAsString();
                        return Class.forName(str);
                    } catch (ClassNotFoundException e) {
                        throw new JsonParseException(e);
                    }
                }
            }
        },
        //2
        Protobuf {
            //预分配LinkedBuffer，用于在序列化过程中存储数据，序列化后clear清空缓存以便下次使用
            private LinkedBuffer linkedBuffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
            //缓存类对应的Schema
            private Map<Class<?>, Schema<?>> schemaCache = new ConcurrentHashMap<>();

            //根据类获取Schema，使用缓存优化过程
            private Schema getSchema(Class<?> clazz) {
                Schema schema = schemaCache.get(clazz);
                if (schema == null) {
                    //该方法是线程安全的
                    schema = RuntimeSchema.getSchema(clazz);
                }
                return schema;
            }

            @Override
            public <T> T deserialize(Class<T> clazz, byte[] bytes) {
                Schema schema = getSchema(clazz);
                Object obj = schema.newMessage();
                ProtobufIOUtil.mergeFrom(bytes, obj, schema);
                return (T) obj;
            }

            @Override
            public <T> byte[] serialize(T object) {
                Class<?> clazz = object.getClass();
                Schema schema = getSchema(clazz);
                byte[] bytes;
                try {
                    bytes = ProtobufIOUtil.toByteArray(object, schema, linkedBuffer);
                } finally {
                    //保证清除缓存
                    linkedBuffer.clear();
                }
                return bytes;
            }
        }
    }
}