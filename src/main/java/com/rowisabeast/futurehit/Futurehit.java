package com.rowisabeast.futurehit;

import com.google.gson.stream.JsonReader;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.rowisabeast.futurehit.RandomString.RandomString;
import org.bson.UuidRepresentation;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Futurehit extends JavaPlugin {

    public FileConfiguration serverData;
    public File data;

    private criticalSMP critical;

    public static MongoDatabase database;

    @Override
    public void onEnable() {
        // Plugin startup logic

        getLogger().info("Plugin enabled!");
        // Create config
        createConfig();
        if(serverData.getInt("ID")==0){
            serverData.set("ID", new RandomString(5));
        }

        // Connect to Database
        //((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("org.mongodb.driver").setLevel(Level.ERROR); // only show ERROR in mongoDB // https://stackoverflow.com/questions/30137564/how-to-disable-mongodb-java-driver-logging
        //LogManager.getLogManager().getLogger("com.mongodb").setLevel(Level.SEVERE);

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
        serverData = new YamlConfiguration();
        try {
            serverData.load(data);
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

/*


LogManager.getLogManager().getLogger("org.mongodb.driver.connection").setLevel(Level.OFF);
        LogManager.getLogManager().getLogger("org.mongodb.driver.management").setLevel(Level.OFF);
        LogManager.getLogManager().getLogger("org.mongodb.driver.cluster").setLevel(Level.OFF);
        LogManager.getLogManager().getLogger("org.mongodb.driver.protocol.insert").setLevel(Level.OFF);
        LogManager.getLogManager().getLogger("org.mongodb.driver.protocol.query").setLevel(Level.OFF);
        LogManager.getLogManager().getLogger("org.mongodb.driver.protocol.update").setLevel(Level.OFF);
 */