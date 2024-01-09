package org.zhaobo.core;

import lombok.extern.slf4j.Slf4j;
import org.zhaobo.common.utils.PropertiesUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * @Auther: bo
 * @Date: 2023/12/2 10:54
 * @Description:
 */
@Slf4j
public class ConfigLoader {
    private static final String CONFIG_FILE = "gateway.properties";
    private static final String ENV_PREFIX = "gateway_";
    private static final String JVM_PREFIX = "gateway.";
    private static final ConfigLoader INSTANCE = new ConfigLoader();

    private Config config;

    private ConfigLoader(){}

    public static ConfigLoader getInstance(){
        return INSTANCE;
    }

    public Config getConfig(){
        return INSTANCE.config;
    }

    /**
     *  优先级高的会覆盖低的，优先级由高到低：
     *  运行参数 > jvm参数 > 环境变量 > 配置文件 > 配置类默认值
     * @param args
     * @return
     */
    public Config load(String[] args){
        // 配置对象默认值
        this.config = new Config();
        // 配置文件
        loadFromConfigFile();
        // 环境变量
        loadFromEnv();
        // jvm参数
        loadFromJvm();
        // 运行参数
        loadFromArgs(args);
        return config;
    }

    private void loadFromArgs(String[] args) {
        // --port=8081
        if (args != null && args.length > 0){
            Properties properties = new Properties();
            for (String arg : args) {
                if (arg.startsWith("--") && arg.contains("=")){
                    properties.put(arg.substring(2, arg.indexOf("=")),
                            arg.substring(arg.indexOf("=") + 1));
                }
            }
            PropertiesUtils.properties2Object(properties, config);
        }
    }

    private void loadFromJvm() {
        Properties properties = System.getProperties();
        PropertiesUtils.properties2Object(properties, config, JVM_PREFIX);
    }

    private void loadFromEnv() {
        Map<String,String> env = System.getenv();
        Properties properties = new Properties();
        properties.putAll(env);
        PropertiesUtils.properties2Object(properties, config, ENV_PREFIX);
    }

    private void loadFromConfigFile() {
        InputStream inputStream = ConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
        Properties properties = new Properties();
        if (inputStream != null) {
            try {
                properties.load(inputStream);
                PropertiesUtils.properties2Object(properties, config);
            } catch (IOException e) {
                log.warn("load config file {} occur error", CONFIG_FILE, e);
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    //
                }
            }
        }


    }


}
