package com.tcveinminer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ClientConfigManager {

    // Định dạng JSON cho dễ đọc
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Đường dẫn file: .minecraft/config/tc_veinglow_client.json
    private static final File CONFIG_FILE = new File(
            FabricLoader.getInstance().getConfigDir().toFile(),
            "tc_veinglow_client.json"
    );

    // Biến lưu trữ cấu hình hiện tại đang chạy trong game
    public static ClientConfig instance = new ClientConfig();

    // Hàm gọi khi game khởi động
    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                instance = GSON.fromJson(reader, ClientConfig.class);
                if (instance == null) {
                    instance = new ClientConfig();
                }
                instance.postLoad();
                save();
            } catch (Exception e) {
                System.err.println("Lỗi đọc file client config, dùng mặc định: " + e.getMessage());
                instance = new ClientConfig();
                instance.postLoad();
            }
        } else {
            // Nếu chưa có file thì tạo mới với thông số mặc định
            instance.postLoad();
            save();
        }
    }

    // Hàm gọi khi người chơi bấm "Lưu" trên giao diện
    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            System.err.println("Lỗi lưu file client config: " + e.getMessage());
        }
    }
}
