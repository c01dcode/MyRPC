package cn.edu.ustc.config;

import cn.edu.ustc.protocol.Serializer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public abstract class Config {
    static Properties properties;
    static {
        try (InputStream in = Config.class.getResourceAsStream("/application.properties")) {
            properties = new Properties();
            properties.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    public static String getServerIP() {
        String value = properties.getProperty("server.ip");
        if(value == null) {
            return "127.0.0.1";
        } else {
            return value;
        }
    }
    public static int getServerPort() {
        String value = properties.getProperty("server.port");
        if(value == null) {
            return 8080;
        } else {
            return Integer.parseInt(value);
        }
    }
    public static Serializer.Algorithm getSerializerAlgorithm() {
        String value = properties.getProperty("serializer.algorithm");
        if(value == null) {
            return Serializer.Algorithm.Jdk;
        } else {
            return Serializer.Algorithm.valueOf(value);
        }
    }
}
