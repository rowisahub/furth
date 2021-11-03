package com.rowisabeast.futurehit;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Pose;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.*;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.io.IOException;
import java.util.*;

public class criticalSMP implements Listener, CommandExecutor {

    private static Futurehit plugin;

    MongoCollection<Document> players;
    MongoCollection<Document> serverDatabase;

    ArrayList<UUID> deadPlsSpawnBody = new ArrayList<>();

    ItemStack lifeShard;

    public criticalSMP(Futurehit plugin){
        criticalSMP.plugin = plugin;

        Bukkit.getPluginManager().registerEvents(this, plugin);

        players = Futurehit.database.getCollection("players");
        serverDatabase = Futurehit.database.getCollection("serverdb");
        getServerDB();
        plugin.getLogger().info("Database Ready!");

        // Add custom items
        lifeShard = getLifeShardINIT();
        makeNewRecipes();

        //Get player body
        getDeadPlayers();

        plugin.getCommand("link").setExecutor(this);

    }

    // Make custom recipes
    public void makeNewRecipes(){
        // Make god apple recipe
        ItemStack god_apple = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE);
        NamespacedKey key = new NamespacedKey(plugin, "god_apple");
        ShapedRecipe recipe_god_apple = new ShapedRecipe(key, god_apple);
        recipe_god_apple.shape("%%%", "%*%", "%%%")
                .setIngredient('%', Material.GOLD_BLOCK)
                .setIngredient('*', Material.APPLE);

        // Make LIFE

        ItemStack LIFE = new ItemStack(Material.FIRE_CHARGE);
        ItemMeta lifeMeta = LIFE.getItemMeta();
        lifeMeta.setDisplayName("Life");
        lifeMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS);
        lifeMeta.setUnbreakable(true);
        lifeMeta.setCustomModelData(14123);
        LIFE.setItemMeta(lifeMeta);
        LIFE.addEnchantment(Enchantment.QUICK_CHARGE, Integer.MAX_VALUE);
        LIFE.addEnchantment(Enchantment.OXYGEN, Integer.MAX_VALUE);
        LIFE.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, Integer.MAX_VALUE);
        LIFE.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, Integer.MAX_VALUE);
        LIFE.addEnchantment(Enchantment.PROTECTION_FIRE, Integer.MAX_VALUE);
        LIFE.addEnchantment(Enchantment.DURABILITY, Integer.MAX_VALUE);
        NamespacedKey lifeKey = new NamespacedKey(plugin, "life");
        ShapedRecipe lifeRecipe = new ShapedRecipe(lifeKey, LIFE);


        //                     1   2   3   / Rows
        //                    123 123 123  / setup

        //        3                 4              1                    1
        // `%`=LIFE  `$`=diamond_blocks  `#`=totem  `*`=netherrite_block
        lifeRecipe.shape("$%$", "%#%", "$ $")
                .setIngredient('$', Material.DIAMOND_BLOCK)
                .setIngredient('#', Material.TOTEM_OF_UNDYING)
                .setIngredient('*', Material.NETHERITE_BLOCK)
                .setIngredient('%', lifeShard);

        Bukkit.addRecipe(recipe_god_apple);
        Bukkit.addRecipe(lifeRecipe);

    }
    public ItemStack getLifeShardINIT(){
        ItemStack lifeShard = new ItemStack(Material.AMETHYST_SHARD);
        lifeShard.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, Integer.MAX_VALUE);
        lifeShard.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, Integer.MAX_VALUE);
        lifeShard.addEnchantment(Enchantment.PROTECTION_FIRE, Integer.MAX_VALUE);
        ItemMeta lifeShardMeta = lifeShard.getItemMeta();
        lifeShardMeta.setUnbreakable(true);
        lifeShardMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS);
        lifeShardMeta.setDisplayName("Life Shard");
        lifeShardMeta.setCustomModelData(14123);
        lifeShard.setItemMeta(lifeShardMeta);
        return lifeShard;
    }
    /*
        Item recipe shape

        1      2      3   (1)
        1      2      3   (2)
        1      2      3   (3)
    */
    // On player join event
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        // PLayer joined
        Player player = event.getPlayer();
        //plugin.getLogger().info(getPLayerLives(player.getUniqueId()).toString());
        Integer playerLives = getPlayerLives(player.getUniqueId());//works

        dbPlayerEdit(event.getPlayer(), "isOnline", true);

        plugin.getLogger().info("Player `"+player.getName()+"`has `"+playerLives.toString()+"` live(s)");

        createBoardList(player); // Sidebar
        setPlayerTabNameWithLives(player); // Tablist

        //Spawn all dead body's for player
        for(UUID p : deadPlsSpawnBody){
            spawnCorpseForPlayer(player, p);
        }

        //If player join with 0 lives, put them into spectator
        if(playerLives==0){
            player.setGameMode(GameMode.SPECTATOR);
        }

        GameProfile gameProfile = ((CraftPlayer) player).getHandle().getGameProfile();
        Property property = (Property) gameProfile.getProperties().get("textures").toArray()[0];
        String texture = property.getValue();
        String signature = property.getSignature();


        // SkinText
        dbPlayerEdit(player, "SkinTexture", texture);
        dbPlayerEdit(player, "SkinSignature", signature);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        dbPlayerEdit(event.getPlayer(), "isOnline", false);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e){
        Player dplayer = e.getEntity();
        if(isPlayerBounty(dplayer.getUniqueId())){
            if(dplayer.getKiller()!=null){
                Player Killer = e.getEntity().getKiller();
                dbPlayerEdit(dplayer, "lives", getPlayerLives(e.getEntity().getUniqueId())-1);
                dbPlayerEdit(Killer, "numberOfTimesPlayerHasKilledBounty", (int) dbPlayerGet(Killer.getUniqueId(), "numberOfTimesPlayerHasKilledBounty")+1);
                dbPlayerEdit(dplayer, "numberOfTImesPlayerHasDiedAsBounty", (int) dbPlayerGet(dplayer.getUniqueId(),"numberOfTImesPlayerHasDiedAsBounty")+1);

                if(getPlayerLives(e.getEntity().getUniqueId())==0){
                    // Player has no lives, THEY ARE DEAD HAHAHA
                    // add players dead body, from codys stream, IN CHECKMARK DM

                    // Adding dead body here
                    //((CraftPlayer) player).getHandle();

                    deadPlsSpawnBody.add(dplayer.getUniqueId());

                    // HEHE, sorry kody
                    spawnCorpseForAll(dplayer);
                    dbPlayerEdit(dplayer,"deadBodyLocation", new Location(dplayer.getWorld(), dplayer.getLocation().getX(), dplayer.getLocation().getY(), dplayer.getLocation().getZ()));

                    // Giver Killer a LifeShard
                    e.getDrops().add(lifeShard);
                }
            }
        }else{
            // Player isn't bounty
            if(dplayer.getKiller()!=null){
                Player Killer = e.getEntity().getKiller();
                if(isPlayerBounty(Killer.getUniqueId())){
                    // The Killer is the bounty
                    dbPlayerEdit(dplayer, "ifKilledByBounty", true);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e){
        if(e.getEntity() instanceof Player && e.getDamager() instanceof Player){
            Player player =  Bukkit.getPlayer(e.getEntity().getUniqueId());
            Player damager = Bukkit.getPlayer(e.getDamager().getUniqueId());
            if((Boolean) dbPlayerGet(damager.getUniqueId(), "ifKilledByBounty")){
                e.setCancelled(true);
                damager.sendMessage(ChatColor.BOLD+ChatColor.RED.toString()+player.customName()+ ChatColor.DARK_RED +" is a Bounty, and has killed you. You can't hurt them!");
            }
        }
    }

    public void createBoardList(Player p){
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();
        Objective obj = board.registerNewObjective("CriticalSMP", "dummy", "CriticalSMP", RenderType.INTEGER);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.getScore("┌────────────────────────").setScore(10);
        obj.getScore("│ Lives: "+getPlayerLives(p.getUniqueId())).setScore(9); // Client
        obj.getScore("│").setScore(8);
        obj.getScore("│").setScore(7);
        obj.getScore("│ Current Bounty: INIT").setScore(6); // Server
        obj.getScore("│ Next Bounty: INIT").setScore(5); // Server
        obj.getScore("│").setScore(4);
        obj.getScore("│ Bounty kills: "+getPlayerBKills(p.getUniqueId())).setScore(3); // Client
        obj.getScore("│ Bounty deaths: "+getPlayerBDeaths(p.getUniqueId())).setScore(2); // Client
        obj.getScore("└────────────────────────").setScore(1);
        p.setScoreboard(board);
    }



    public void setPlayerTabNameWithLives(Player p){
        Integer pl = getPlayerLives(p.getUniqueId());
        StringBuilder ph = new StringBuilder();
        for(int n = 0; n < pl; n++){
            ph.append("§c♥");
        }
        p.setPlayerListName(p.getName()+" "+ph.toString());
    }

    public Boolean isPlayerBounty(UUID uuid){
        Document Db = getPLayerFromDB(uuid);
        return Boolean.parseBoolean(Db.get("isCurrentBounty").toString());
    }

    public Integer getPlayerBKills(UUID uuid){
        Document Db = getPLayerFromDB(uuid);
        return (int) Db.get("numberOfTimesPlayerHasKilledBounty");
    }

    public Object dbPlayerGet(UUID uuid, String key){
        Document Db = getPLayerFromDB(uuid);
        return Db.get(key);
    }

    public Integer getPlayerBDeaths(UUID uuid){
        Document Db = getPLayerFromDB(uuid);
        return (int) Db.get("numberOfTImesPlayerHasDiedAsBounty");
    }

    public void dbPlayerEdit(Player pl, String key, Object valueToUpdate){
        UUID uuid = pl.getUniqueId();
        players.updateOne(Filters.eq("_id", uuid), Updates.set(key, valueToUpdate));
    }

    public void dpPlayerUUIDEdit(UUID uuid, String key, Object valueToUpdate){
        players.updateOne(Filters.eq("_id", uuid), Updates.set(key, valueToUpdate));
    }

    public void dbServerEdit(String key, Object valueToUpdate){
        serverDatabase.updateOne(Filters.eq("_id", "CriticalSMP"), Updates.set(key, valueToUpdate));
    }

    public Integer getPlayerLives(UUID uuid){
        Player pl = Bukkit.getPlayer(uuid);
        Document Db = getPLayerFromDB(uuid);
        if(Db==null || Db.size()==0){
            players.insertOne(new Document()
                    .append("_id", uuid)
                    .append("uuid", uuid)
                    .append("username", pl.getName())
                    .append("lives", 5)
                    .append("numberOfTimesPlayerHasKilledBounty", 0)
                    .append("numberOfTImesPlayerHasDiedAsBounty", 0)
                    .append("ifKilledByBounty", false)
                    .append("connectionInformationHost", pl.getAddress().getHostString())
                    .append("connectionInformationPort", pl.getAddress().getPort())
                    .append("isOnline", true)
                    .append("isBounty", false)
                    .append("isNextBounty", false)
                    .append("deadBodyLocation", new Location(null, 0.0, 0.0, 0.0))
                    .append("SkinTexture", "")
                    .append("SkinSignature", ""));
            return 3;
        }else{
            return (int) Db.get("lives");
        }
    }

    public Document getPLayerFromDB(UUID uuid){ // WORKS
        return players.find(new Document("_id", uuid)).first();
    }

    //Set and get Server Database
    private void getServerDB(){
        Document ts = serverDatabase.find(new Document("_id", "CriticalSMP")).first();
        if(ts==null || ts.size()==0){
            serverDatabase.insertOne(new Document()
                    .append("_id", "CriticalSMP")
                    .append("nextBountyTimeRemaining", 0)
                    .append("defaultNextBountyTimeTick", 360000)
                    .append("isBountyTimerUp", false)
                    .append("name", "CriticalSMP")
                    .append("currentBountyUUID", "")
                    .append("currentBountyName", "")
                    .append("isBountyDead", false)
                    .append("wasBountyKilledByPlayer", false)
                    .append("isBountyInGame", false)
                    .append("isServerReadyForNextBounty", false)
                    .append("nextBountyUUID", "")
                    .append("nextBountyName", ""));//
        }
        // serverDatabase was already created
    }
    private Integer getCurrentBountyTimeRemaining(){
        Document Db = serverDatabase.find(new Document("_id", "CriticalSMP")).first();
        return (int) Db.get("nextBountyTimeRemaining");
    }
    private Object dbServerGet(String key){
        Document Db = serverDatabase.find(new Document("_id", "CriticalSMP")).first();
        return Db.get(key);
    }

    private void setServerDBStartNexBounty(){
        ArrayList<Bson> ValUpdate = new ArrayList<>();
        ValUpdate.add(Updates.set("nextBountyTimeRemaining", dbServerGet("defaultNextBountyTimeTick")));
        ValUpdate.add(Updates.set("isBountyTimerUp", false));
        ValUpdate.add(Updates.set("isBountyDead", false));
        ValUpdate.add(Updates.set("wasBountyKilledByPlayer", false));
        ValUpdate.add(Updates.set("isServerReadyForNextBounty", false));
        serverDatabase.updateMany(Filters.eq("_id", "CriticalSMP"), ValUpdate);
        for(UUID uuid : getAllServerPlayers()){
            dpPlayerUUIDEdit(uuid, "isBounty", false);
            dpPlayerUUIDEdit(uuid, "isNextBounty", false);
            dpPlayerUUIDEdit(uuid, "ifKilledByBounty", false);
        }
    }

    // Set player bounty
    // Set time remaining for bounty
    // if bounty leaves, wait around 5 min, then if the player is back, continue count down, if not find new player

    // run async task every 5 hours, not repeat, as we might need to stop the loop from running for a bit
    private void startNextBounty(){
        setServerDBStartNexBounty();
        runNextBounty();
    }
    private @NotNull Runnable runNextBounty(){
        Integer bt = getCurrentBountyTimeRemaining();
        if(bt>0){
            everyTick();
        }else if(bt==0){
            // Check to see if the server is ready for next bounty
            if((Boolean) dbServerGet("isBountyDead") && (Boolean) dbServerGet("wasBountyKilledByPlayer")){
                startNextBounty();
            }else{
                Bukkit.getScheduler().runTaskTimerAsynchronously(
                        plugin,
                        runNextBounty(),
                        1,
                        0
                );
            }
        }
        return null;
    }
    private void pauseCountDown(){
        tempStopTick = true;
    }
    private void resumeCountDown(){
        tempStopTick = false;
        everyTick();
    }
    Boolean tempStopTick = false;
    private Runnable everyTick(){
        if(tempStopTick){
            // No need to run this now
        }else {
            Integer bt = getCurrentBountyTimeRemaining();

            if(bt==0){
                tempStopTick = true;
                dbServerEdit("isBountyTimerUp", true);
            }

            Bukkit.getScheduler().runTaskTimerAsynchronously(
                    plugin,
                    everyTick(),
                    1,
                    0
            );
            dbServerEdit("nextBountyTimeRemaining", bt-1);
        }
        return null;
    }
    private void spawnCorpseForAll(Player deadPerson){
        CraftPlayer craftPlayer = (CraftPlayer) deadPerson;
        MinecraftServer server = craftPlayer.getHandle().getServer();
        ServerLevel level = craftPlayer.getHandle().getLevel();

        ServerPlayer npc = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), ChatColor.stripColor(deadPerson.getDisplayName())));

        npc.setPos(deadPerson.getLocation().getX(), deadPerson.getLocation().getWorld().getHighestBlockAt(deadPerson.getLocation()).getY()+1, deadPerson.getLocation().getZ());
        npc.setPose(Pose.SLEEPING);
        npc.setCustomNameVisible(false);

        // Red Crewmate skin
        String texture = "ewogICJ0aW1lc3RhbXAiIDogMTYwMDQxNDA0MjAxOSwKICAicHJvZmlsZUlkIiA6ICJlYTk3YThkMTFmNzE0Y2UwYTc2ZDdjNTI1M2NjN2Y3MiIsCiAgInByb2ZpbGVOYW1lIiA6ICJfTXJfS2VrcyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9iY2UzOTRlM2VlMDc1OWM5YzZiMWZhMmNhMDk0YThlODdhZWExNTMxYWRiMGU4NmM5Zjk1YTgzYzQ0NTI5ZTEwIgogICAgfQogIH0KfQ==";
        String signature = "oVGXKvwSSfRI0qUR5zXXIzIur4VdckCpiFQIEi/zYy/0XxXHmueHn1FCOP19kvpuCHTzSUxrSasuFtpWV5GDMUKRegYXkEFNOVPNjlkr3UeGqk+bCbVxGXSKkJM78mRCnv1rSDZmI7QOtshh67sS3IGVPRYV9T2IEEH+phXIFewRNjwgYtr7UVWMm74fqfiXirerxIgwGpp+eq8XiJ2DfCfKYaBDCns41FHoX7B5nBVLzqDlQW931e5TgTyDAiu3YWk3ASi9fiawCsTM11fLTJ/VEq9FdOtFXpxCzYUgY5Xy1I0xtRhSEk4LnMe79ffzsXp1noZhy8iKg4nxSLVKBK78dLX8JFj+cMiRkqoGACSJOk4iA4diLhEeRL6jG+clv5Xxv9NfEkM6AGZFwGAlgGWZxcy71ZlzTtFCaTu2LCFxki2B2xNe/JdmPyDEwankv7hcWxon/zCLBukF/aGj2Bzam4b/Bjz6e0vSqElWb8KLTQC46hDAEeqngrDDSaiig7vs5rkgvUmPmyD2qAlI9dXgDcDPmJGlma7jmZiVndDzdDfBlmcIp4o3J2ECMyJYhcY+KWxV20RxAjK71jfLmXOQbRNVX1dZq7uSHlHR/XBjmeP0a+SxHV6fuWsWHvXaBsnLJlRdYrPSnFx+GMwZmYvNty4aS1B2ty7aTfIFLzo=";


        GameProfile gameProfile = ((CraftPlayer) deadPerson).getHandle().getGameProfile();
        Property property = (Property) gameProfile.getProperties().get("textures").toArray()[0];

        texture = property.getValue();
        signature = property.getSignature();

        npc.getGameProfile().getProperties().put("textures", new Property("textures", texture, signature));


        // NOW SEND THE NPC
        for(Player p : Bukkit.getOnlinePlayers()){
            ServerGamePacketListenerImpl ps = ((CraftPlayer) p).getHandle().connection;
            ps.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, npc));
            ps.send(new ClientboundAddPlayerPacket(npc));
            ps.send(new ClientboundSetEntityDataPacket(npc.getId(), npc.getEntityData(), true));

            // Remove body after some time
            //new BukkitRunnable(){
            //    @Override
            //    public void run(){
            //        ps.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, npc));
            //    }
            //}.runTaskLaterAsynchronously(plugin, 20L);
        }
    }

    private void spawnCorpseForPlayer(Player p, UUID deadPerson){
        CraftPlayer craftPlayer = (CraftPlayer) p;
        MinecraftServer server = craftPlayer.getHandle().getServer();
        ServerLevel level = craftPlayer.getHandle().getLevel();

        ServerPlayer npc = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), ChatColor.stripColor( (String) dbPlayerGet(deadPerson, "username"))));

        Location deadPersonLocation = (Location) dbPlayerGet(deadPerson, "deadBodyLocation");

        npc.setPos(deadPersonLocation.getX(), deadPersonLocation.getWorld().getHighestBlockAt(deadPersonLocation).getY()+1, deadPersonLocation.getZ());
        npc.setPose(Pose.SLEEPING);
        npc.setCustomNameVisible(false);

        // Red Crewmate skin
        String texture = "ewogICJ0aW1lc3RhbXAiIDogMTYwMDQxNDA0MjAxOSwKICAicHJvZmlsZUlkIiA6ICJlYTk3YThkMTFmNzE0Y2UwYTc2ZDdjNTI1M2NjN2Y3MiIsCiAgInByb2ZpbGVOYW1lIiA6ICJfTXJfS2VrcyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9iY2UzOTRlM2VlMDc1OWM5YzZiMWZhMmNhMDk0YThlODdhZWExNTMxYWRiMGU4NmM5Zjk1YTgzYzQ0NTI5ZTEwIgogICAgfQogIH0KfQ==";
        String signature = "oVGXKvwSSfRI0qUR5zXXIzIur4VdckCpiFQIEi/zYy/0XxXHmueHn1FCOP19kvpuCHTzSUxrSasuFtpWV5GDMUKRegYXkEFNOVPNjlkr3UeGqk+bCbVxGXSKkJM78mRCnv1rSDZmI7QOtshh67sS3IGVPRYV9T2IEEH+phXIFewRNjwgYtr7UVWMm74fqfiXirerxIgwGpp+eq8XiJ2DfCfKYaBDCns41FHoX7B5nBVLzqDlQW931e5TgTyDAiu3YWk3ASi9fiawCsTM11fLTJ/VEq9FdOtFXpxCzYUgY5Xy1I0xtRhSEk4LnMe79ffzsXp1noZhy8iKg4nxSLVKBK78dLX8JFj+cMiRkqoGACSJOk4iA4diLhEeRL6jG+clv5Xxv9NfEkM6AGZFwGAlgGWZxcy71ZlzTtFCaTu2LCFxki2B2xNe/JdmPyDEwankv7hcWxon/zCLBukF/aGj2Bzam4b/Bjz6e0vSqElWb8KLTQC46hDAEeqngrDDSaiig7vs5rkgvUmPmyD2qAlI9dXgDcDPmJGlma7jmZiVndDzdDfBlmcIp4o3J2ECMyJYhcY+KWxV20RxAjK71jfLmXOQbRNVX1dZq7uSHlHR/XBjmeP0a+SxHV6fuWsWHvXaBsnLJlRdYrPSnFx+GMwZmYvNty4aS1B2ty7aTfIFLzo=";


        //GameProfile gameProfile = ((CraftPlayer) deadPerson).getHandle().getGameProfile();
        //Property property = (Property) gameProfile.getProperties().get("textures").toArray()[0];
        if(dbPlayerGet(deadPerson, "SkinTexture")!="" && dbPlayerGet(deadPerson, "SkinSignature")!=""){
            texture = (String) dbPlayerGet(deadPerson, "SkinTexture");
            signature = (String) dbPlayerGet(deadPerson, "SkinSignature");
        }

        npc.getGameProfile().getProperties().put("textures", new Property("textures", texture, signature));


        // NOW SEND THE NPC
        ServerGamePacketListenerImpl ps = ((CraftPlayer) p).getHandle().connection;
        ps.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, npc));
        ps.send(new ClientboundAddPlayerPacket(npc));
        ps.send(new ClientboundSetEntityDataPacket(npc.getId(), npc.getEntityData(), true));
    }

    private void getDeadPlayers(){
        ArrayList<UUID> allJoinedPlayers = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()){
            allJoinedPlayers.add(p.getUniqueId());
        }
        FindIterable<Document> iterDoc = players.find();
        for (Document document : iterDoc) {
            allJoinedPlayers.add((UUID) document.get("_id"));
        }

        for(UUID u: allJoinedPlayers){
            Integer pLives = getPlayerLives(u);
            if(pLives==0){
                deadPlsSpawnBody.add(u);
            }
        }
    }

    private ArrayList<UUID> getAllServerPlayers(){
        ArrayList<UUID> allJoinedPlayers = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()){
            allJoinedPlayers.add(p.getUniqueId());
        }
        FindIterable<Document> iterDoc = players.find();
        for (Document document : iterDoc) {
            allJoinedPlayers.add((UUID) document.get("_id"));
        }
        return allJoinedPlayers;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) { //  /verify randomcodeSK
        if (!(sender instanceof Player)) {
            sender.sendMessage("§8• §7This command is §conly executable §7by players.");
            return true;
        }
        Player player = (Player) sender;
        if (cmd.getName().equalsIgnoreCase("givelifeshard")) {
            player.getInventory().addItem(lifeShard, lifeShard, lifeShard);
            player.sendMessage("Gave you 3 life shards");
        }
        return true;
    }
}