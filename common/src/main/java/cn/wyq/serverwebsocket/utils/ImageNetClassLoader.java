package cn.wyq.serverwebsocket.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ImageNetClassLoader {

    public static String[] loadClassNames() throws IOException {
        InputStream inputStream = ImageNetClassLoader.class.getResourceAsStream("/data/imagenet_classes.txt");
        if (inputStream == null) {
            throw new IOException("无法找到 imagenet_classes.txt 文件");
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        return reader.lines().toArray(String[]::new);
    }
}
