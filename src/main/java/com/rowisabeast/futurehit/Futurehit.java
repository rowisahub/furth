package com.rowisabeast.futurehit;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.UuidRepresentation;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class Futurehit extends JavaPlugin {

    public FileConfiguration playerData;
    public File data;

    private criticalSMP critical;

    public static MongoDatabase database;

    @Override
    public void onEnable() {
        // Plugin startup logic

        getLogger().info("Plugin enabled!");
        // Create config
        createConfig();

        // Connect to Database
        connectToDataBase();

        // Run criticalSMP
        critical = new criticalSMP(this);
    }

    private void createConfig(){
        data = new File(getDataFolder() + File.separator + "config.yml");
        if(!data.exists()){
            getLogger().info(ChatColor.LIGHT_PURPLE + "Creating file config.yml");
            this.saveResource("config.yml", false);
        }
        playerData = new YamlConfiguration();
        try {
            playerData.load(data);
        } catch (IOException | InvalidConfigurationException e){
            e.printStackTrace();
        }
    }

    private static void connectToDataBase(){
        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .applyConnectionString(new ConnectionString("mongodb+srv://FutureHit:MuphBvTUiccg9G7q@criticalfut.hqri5.mongodb.net/criticalFut?retryWrites=true&w=majority&"))
                .build();
        MongoClient mongoClient = MongoClients.create(clientSettings);
        Futurehit.database = mongoClient.getDatabase("criticalFut");
        // find out how to use database
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
