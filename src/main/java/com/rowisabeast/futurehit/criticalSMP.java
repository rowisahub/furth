package com.rowisabeast.futurehit;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.rowisabeast.futurehit.event.GiveLifeEvent;
import com.rowisabeast.futurehit.event.RemoveLifeEvent;
import com.rowisabeast.futurehit.lockFut.lockFut;
import com.rowisabeast.futurehit.playerClass.playerClass;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
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
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

public class criticalSMP implements Listener, CommandExecutor {

    public static Futurehit plugin;

    boolean debug = false;

    public MongoCollection<Document> players;
    public MongoCollection<Document> serverDatabase;

    //private final ReentrantLock lock = new ReentrantLock();
    private final lockFut lock = new lockFut(debug);

    public ArrayList<UUID> deadPlsSpawnBody = new ArrayList<UUID>();

    public ItemStack life_Shard;
    public ItemStack life;

    public HashMap<UUID, Scoreboard> playerScoreboard = new HashMap<UUID, Scoreboard>();
    public HashMap<UUID, Objective> playerObjective = new HashMap<UUID, Objective>();

    public HashMap<String, Object> serverDBLocal = new HashMap<String, Object>();
    public HashMap<UUID, playerClass> playerDBLocal = new HashMap<UUID, playerClass>();

    GiveLifeEvent addLifeEvent;
    RemoveLifeEvent removeLifeEvent;

    public criticalSMP(Futurehit plugin) {
        criticalSMP.plugin = plugin;

        Bukkit.getPluginManager().registerEvents(this, plugin);

        players = Futurehit.database.getCollection("players");
        serverDatabase = Futurehit.database.getCollection("serverdb");
        getServerDB();
        plugin.getLogger().info("Database Ready!");

        // Add custom items
        makeNewRecipes();

        //Get player body
        //getDeadPlayers();

        plugin.getCommand("bdussy").setExecutor(this);
        plugin.getCommand("donate").setExecutor(this);

        addLifeEvent = new GiveLifeEvent(this);
        removeLifeEvent = new RemoveLifeEvent(this);

         Bukkit.getScheduler().runTaskLater(plugin, () ->
                Bukkit.getScheduler().runTaskTimerAsynchronously(
                        plugin,
                        this::tick,
                        0,
                        5
                ),
        0
        );
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                Bukkit.getScheduler().runTaskTimerAsynchronously(
                        plugin,
                        this::backupServerDB,
                        2400,
                        2400
                ),
                0
        );
    }

    // Make custom recipes
    public void makeNewRecipes() {

        // live_shard
        ItemStack lifeShard = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta lifeShardMeta = lifeShard.getItemMeta();
        lifeShardMeta.setUnbreakable(true);
        lifeShardMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS);
        lifeShardMeta.setDisplayName("Life Shard");
        lifeShardMeta.setCustomModelData(14123);
        lifeShard.setItemMeta(lifeShardMeta);
        life_Shard = lifeShard;

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
        NamespacedKey lifeKey = new NamespacedKey(plugin, "life");
        ShapedRecipe lifeRecipe = new ShapedRecipe(lifeKey, LIFE);


        //                     1   2   3   / Rows
        //                    123 123 123  / setup

        //        3                 4              1                    1
        // `%`=LIFE_shard  `$`=diamond_blocks  `#`=totem  `*`=netherrite_block
        lifeRecipe.shape("$%$", "%#%", "$*$")
                .setIngredient('$', Material.DIAMOND_BLOCK)
                .setIngredient('#', Material.TOTEM_OF_UNDYING)
                .setIngredient('*', Material.NETHERITE_BLOCK)
                .setIngredient('%', new RecipeChoice.ExactChoice(lifeShard));

        Bukkit.addRecipe(recipe_god_apple);
        Bukkit.addRecipe(lifeRecipe);

        life = LIFE;

    }

    /*
        Item recipe shape

        1      2      3   (1)
        1      2      3   (2)
        1      2      3   (3)
    */
    // On player join event
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // PLayer joined
        Player player = event.getPlayer();

        playerjoin(player.getUniqueId());

        //plugin.getLogger().info(getPLayerLives(player.getUniqueId()).toString());
        Integer playerLives = getPlayerLives(player.getUniqueId());//works

        dbPlayerEdit(event.getPlayer(), "isOnline", true);
        dbServerEdit("numberOfPlayersOnline", (int) dbServerGet("numberOfPlayersOnline") + 1);

        //plugin.getLogger().info("Player `" + player.getName() + "`has `" + playerLives.toString() + "` live(s)");

        //createBoardList(player); // Sidebar
        setPlayerTabNameWithLives(player); // Tablist

        //Spawn all dead body's for player
       // for (UUID p : deadPlsSpawnBody) {
        //    spawnCorpseForPlayer(player, p);
        //}

        //If player join with 0 lives, put them into spectator
        if (playerLives == 0) {
            player.setGameMode(GameMode.SPECTATOR);
            event.setJoinMessage("You don't have any lives!");
        }

        //GameProfile gameProfile = ((CraftPlayer) player).getHandle().getGameProfile();
        //Property property = (Property) gameProfile.getProperties().get("textures").toArray()[0];
        //String texture = property.getValue();
        //String signature = property.getSignature();


        // SkinText
        //dbPlayerEdit(player, "SkinTexture", texture);
        //dbPlayerEdit(player, "SkinSignature", signature);

        if (Bukkit.getOnlinePlayers().size() < 2) {
            return;
        }
        player.sendTitle(ChatColor.LIGHT_PURPLE+dbServerGet("currentBountyName").toString(), ChatColor.LIGHT_PURPLE+dbServerGet("currentBountyName").toString()+ChatColor.RED+" is the bounty! BEWARE!!", 1, 20, 10);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        dbPlayerEdit(event.getPlayer(), "isOnline", false);
        dbServerEdit("numberOfPlayersOnline", (int) dbServerGet("numberOfPlayersOnline") - 1);
        playerLeaveDB(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
//        if(plugin.serverData.getInt("endFreeTime")!=0){
//            if(System.currentTimeMillis()<plugin.serverData.getInt("endFreeTime")){
//                return;
//            }
//        }
        Player killedPlayer = e.getEntity();
        dbPlayerEdit(killedPlayer, "isAlive", false);
        if (isPlayerBounty(killedPlayer.getUniqueId())) {
            if (debug) {
                plugin.getLogger().info("Player that dies is bounty");
            }
            if (killedPlayer.getKiller() != null) {
                Bukkit.getPluginManager().callEvent(removeLifeEvent);
                removeLifeEvent.removeLifeFromPlayer(killedPlayer);
                Player killer = e.getEntity().getKiller();

                dbPlayerEdit(killer, "numberOfTimesPlayerHasKilledBounty", (int) dbPlayerGet(killer.getUniqueId(), "numberOfTimesPlayerHasKilledBounty") + 1);

                dbServerEdit("wasBountyKilledByPlayer", true);

                //spawning dead body
//                if (getPlayerLives(killedPlayer.getUniqueId()) == 0) {
                    // Player has no lives, THEY ARE DEAD HAHAHA
                    // add players dead body, from codys stream, IN CHECKMARK DM

                    // Adding dead body here
                    //((CraftPlayer) player).getHandle();

                    //deadPlsSpawnBody.add(killedPlayer.getUniqueId());

                    // HEHE, sorry kody
                    //spawnCorpseForAll(killedPlayer);
//                    dbPlayerEdit(killedPlayer, "deadBodyLocationWorld", killedPlayer.getWorld().getName());
//                    dbPlayerEdit(killedPlayer, "deadBodyLocationX", killedPlayer.getLocation().getX());
//                    dbPlayerEdit(killedPlayer, "deadBodyLocationY", killedPlayer.getLocation().getY());
//                    dbPlayerEdit(killedPlayer, "deadBodyLocationZ", killedPlayer.getLocation().getZ());

                    // Giver Killer a LifeShard

//                }
                e.getDrops().add(life_Shard);
            }
        } else {
            // Player isn't bounty
//            if(plugin.serverData.getInt("endFreeTime")!=0){
//                if(System.currentTimeMillis()<plugin.serverData.getInt("endFreeTime")){
//                    return;
//                }
//            }
            if (killedPlayer.getKiller() != null) {
                Player Killer = e.getEntity().getKiller();
                if (isPlayerBounty(Killer.getUniqueId())) {
                    // The Killer is the bounty
                    dbPlayerEdit(killedPlayer, "ifKilledByBounty", true);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player && e.getDamager() instanceof Player) {

//            if(plugin.serverData.getInt("endFreeTime")!=0){
//                if(System.currentTimeMillis()<plugin.serverData.getInt("endFreeTime")){
//                    e.setCancelled(true);
//                    Bukkit.getPlayer(e.getDamager().getUniqueId()).sendMessage("You can't hurt another player while the the 12hour protection time is active!");
//                    return;
//                }
//            }

            Player player = Bukkit.getPlayer(e.getEntity().getUniqueId());
            Player damager = Bukkit.getPlayer(e.getDamager().getUniqueId());
            if ((Boolean) dbPlayerGet(damager.getUniqueId(), "ifKilledByBounty")) {
                e.setCancelled(true);
                damager.sendMessage(ChatColor.BOLD + ChatColor.RED.toString() + player.customName() + ChatColor.DARK_RED + " is a Bounty, and has killed you. You can't hurt them!");
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        //
        dbPlayerEdit(e.getPlayer(), "isAlive", true);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if ((e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) && e.getItem() == (life)) {
            //
            Bukkit.getPluginManager().callEvent(addLifeEvent);
            addLifeEvent.addLifeToPlayer(p);

            e.useItemInHand();
        }
    }
//
//    @EventHandler
//    public void onPlayerMove(PlayerMoveEvent e){
//        if(dbServerGet("currentBountyUUID")!=null || dbServerGet("currentBountyUUID")!=""){
//
//            UUID bountyUUID = (UUID) dbServerGet("currentBountyUUID");
//
//            if(Bukkit.getPlayer(bountyUUID)==null) return;
//
//            Player p = Bukkit.getPlayer(bountyUUID);
//
//            if(e.getPlayer() == p) {
//                for (Player allplayers : Bukkit.getOnlinePlayers()) {
//                    allplayers.setCompassTarget(p.getLocation());
//                }
//            }
//        }
//    }

    //public Objective obj;
    public void createBoardList(Player p) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();
        Objective obj = board.registerNewObjective("CriticalSMP", "dummy", "CriticalSMP", RenderType.INTEGER);

        String lastpNAME;
        if ((int) dbServerGet("howManyBountys") == 0) {
            lastpNAME = "INIT";
        } else {
            lastpNAME = (String) dbServerGet("currentBountyName");
        }

        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.getScore("┌────────────────────────").setScore(10);
        obj.getScore("│ Lives: " + getPlayerLives(p.getUniqueId())).setScore(9); // Client
        obj.getScore("│").setScore(8);
        obj.getScore("│").setScore(7);
        obj.getScore("│ Current Bounty: " + lastpNAME).setScore(6); // Server
        obj.getScore("│ Next Bounty: INIT").setScore(5); // Server
        obj.getScore("│").setScore(4);
        obj.getScore("│ Bounty kills: " + getPlayerBKills(p.getUniqueId())).setScore(3); // Client
        obj.getScore("│ Bounty deaths: " + getPlayerBDeaths(p.getUniqueId())).setScore(2); // Client
        obj.getScore("└────────────────────────").setScore(1);
        p.setScoreboard(board);
        playerScoreboard.put(p.getUniqueId(), board);
        playerObjective.put(p.getUniqueId(), obj);
    }

    public void editPlayerScoreBoard(Player p, String Original, String text, Integer score) {
        //if (score == 10 || score == 1) return;
        //obj.getScoreboard().resetScores("│ "+Original);
        //obj.getScore("│ "+text).setScore(score);
        //playerScoreboard.get(p.getUniqueId()).resetScores("│ " + Original);
        //playerObjective.get(p.getUniqueId()).getScore("│ " + text).setScore(score);
    }

    public void setPlayerTabNameWithLives(Player p) {
        Integer pl = getPlayerLives(p.getUniqueId());
        StringBuilder ph = new StringBuilder();
        for (int n = 0; n < pl; n++) {
            ph.append("§c♥");
        }
        p.setPlayerListName(p.getName() + " " + ph.toString());

        // p.playNote(p.getLocation(), Instrument.CHIME, Note.flat(1, Note.Tone.A));
    }

    public Boolean isPlayerBounty(UUID uuid) {
        //Document Db = getPLayerFromDB(uuid);
        //return Boolean.parseBoolean((String) Db.get("isCurrentBounty"));
        return (boolean) dbPlayerGet(uuid, "isBounty");
    }

    public Integer getPlayerBKills(UUID uuid) {
        //Document Db = getPLayerFromDB(uuid);
        //return (int) Db.get("numberOfTimesPlayerHasKilledBounty");
        return (int) dbPlayerGet(uuid, "numberOfTimesPlayerHasKilledBounty");
    }

    public Object dbPlayerGet(UUID uuid, String key) {
        Document Db = getPLayerFromDB(uuid);
        return Db.get(key);
    }

    public Integer getPlayerBDeaths(UUID uuid) {
        Document Db = getPLayerFromDB(uuid);
        return (int) Db.get("numberOfTImesPlayerHasDiedAsBounty");
    }

    public void dbPlayerEdit(Player pl, String key, Object valueToUpdate) {
        UUID uuid = pl.getUniqueId();
        //players.updateOne(Filters.eq("_id", uuid), Updates.set(key, valueToUpdate));
        playerDBLocal.get(uuid).PlayerDBLocal.put(key, valueToUpdate);
    }

    public void dpPlayerUUIDEdit(UUID uuid, String key, Object valueToUpdate) {
        //players.updateOne(Filters.eq("_id", uuid), Updates.set(key, valueToUpdate));
        playerDBLocal.get(uuid).PlayerDBLocal.put(key, valueToUpdate);
    }

    public void dbServerEdit(String key, Object valueToUpdate) {
        //serverDatabase.updateOne(Filters.eq("_id", "CriticalSMP"), Updates.set(key, valueToUpdate));
        serverDBLocal.put(key, valueToUpdate);
    }

    public Integer getPlayerLives(UUID uuid) {
        Document Db = getPLayerFromDB(uuid);
        return (int) Db.get("lives");
    }

    private void playerjoin(UUID uuid){
        // playerDBLocal
        Player pl = Bukkit.getPlayer(uuid);
        if(playerDBLocal.get(uuid)!=null){ // if player is already in the local system, then we're good
            return;
        }

        Document Db = players.find(new Document("_id", uuid)).first();

        if(playerDBLocal.get(uuid)==null && Db!=null){ // if player isn't in the local db but has played befor add them to the local
            playerClass pc = new playerClass(
                    uuid,
                    pl.getName(),
                    (int)Db.get("lives"),
                    (int)Db.get("numberOfTimesPlayerHasKilledBounty"),
                    (int)Db.get("numberOfTImesPlayerHasDiedAsBounty"),
                    (boolean)Db.get("ifKilledByBounty"),
                    (String)Db.get("connectionInformationHost"),
                    (int)Db.get("connectionInformationPort"),
                    (boolean)Db.get("isOnline"),
                    (boolean)Db.get("isBounty"),
                    (boolean)Db.get("isNextBounty"),
                    (String)Db.get("deadBodyLocationWorld"),
                    (double)Db.get("deadBodyLocationX"),
                    (double)Db.get("deadBodyLocationY"),
                    (double)Db.get("deadBodyLocationz"),
                    (String)Db.get("SkinTexture"),
                    (String)Db.get("SkinSignature"),
                    (boolean)Db.get("isAlive")
            );
            playerDBLocal.put(uuid, pc);
            return;
        }
        // local and main database havn't been set, New Player

        if(playerDBLocal.get(uuid)==null && Db==null) {
             players.insertOne(new Document() // New Database entry
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
                    .append("deadBodyLocationWorld", "")
                    .append("deadBodyLocationX", 0.0)
                    .append("deadBodyLocationY", 0.0)
                    .append("deadBodyLocationz", 0.0)
                    .append("SkinTexture", "")
                    .append("SkinSignature", "")
                    .append("isAlive", true));
            Document Dbs = players.find(new Document("_id", uuid)).first();
            playerClass pc = new playerClass( // New Local DB entry
                    uuid,
                    pl.getName(),
                    (int) Dbs.get("lives"),
                    (int) Dbs.get("numberOfTimesPlayerHasKilledBounty"),
                    (int) Dbs.get("numberOfTImesPlayerHasDiedAsBounty"),
                    (boolean) Dbs.get("ifKilledByBounty"),
                    (String) Dbs.get("connectionInformationHost"),
                    (int) Dbs.get("connectionInformationPort"),
                    (boolean) Dbs.get("isOnline"),
                    (boolean) Dbs.get("isBounty"),
                    (boolean) Dbs.get("isNextBounty"),
                    (String) Dbs.get("deadBodyLocationWorld"),
                    (double) Dbs.get("deadBodyLocationX"),
                    (double) Dbs.get("deadBodyLocationY"),
                    (double) Dbs.get("deadBodyLocationz"),
                    (String) Dbs.get("SkinTexture"),
                    (String) Dbs.get("SkinSignature"),
                    (boolean) Dbs.get("isAlive")
            );
            playerDBLocal.put(uuid, pc);
            return;
        }

        // just in case it doesn't match the others
        playerClass pc = new playerClass( // New Local DB entry
                uuid,
                pl.getName(),
                (int) Db.get("lives"),
                (int) Db.get("numberOfTimesPlayerHasKilledBounty"),
                (int) Db.get("numberOfTImesPlayerHasDiedAsBounty"),
                (boolean) Db.get("ifKilledByBounty"),
                (String) Db.get("connectionInformationHost"),
                (int) Db.get("connectionInformationPort"),
                (boolean) Db.get("isOnline"),
                (boolean) Db.get("isBounty"),
                (boolean) Db.get("isNextBounty"),
                (String) Db.get("deadBodyLocationWorld"),
                (double) Db.get("deadBodyLocationX"),
                (double) Db.get("deadBodyLocationY"),
                (double) Db.get("deadBodyLocationz"),
                (String) Db.get("SkinTexture"),
                (String) Db.get("SkinSignature"),
                (boolean) Db.get("isAlive")
        );
        playerDBLocal.put(uuid, pc);
    }

    public Document getPLayerFromDB(UUID uuid) { // WORKS
        if(playerDBLocal.get(uuid)==null) return null;
        return playerDBLocal.get(uuid).PlayerDBLocal;
    }

    //Set and get Server Database
    private void getServerDB() {
        Document ts = serverDatabase.find(new Document("_id", "CriticalSMP")).first();
        if (ts == null || ts.size() == 0) {
            serverDatabase.insertOne(new Document()
                    .append("_id", "CriticalSMP")
                    .append("nextBountyTimeRemaining", 0)
                    .append("defaultNextBountyTimeTick", 6000) // 360000
                    .append("isBountyTimerUp", false)
                    .append("name", "CriticalSMP")
                    .append("currentBountyUUID", "")
                    .append("currentBountyName", "")
                    .append("isBountyDead", false)
                    .append("wasBountyKilledByPlayer", false)
                    .append("isBountyInGame", false)
                    .append("isServerReadyForNextBounty", false)
                    .append("nextBountyUUID", "")
                    .append("nextBountyName", "")
                    .append("numberOfPlayersOnline", 0)
                    .append("bountyOfflineTime", 18000)
                    .append("howManyBountys", 0));//
        }
        // serverDatabase was already created

        //First time start-up, get everything
        Document Db = serverDatabase.find(new Document("_id", "CriticalSMP")).first();
        serverDBLocal.put("_id", Db.get("_id"));
        serverDBLocal.put("nextBountyTimeRemaining", Db.get("nextBountyTimeRemaining"));
        serverDBLocal.put("defaultNextBountyTimeTick", Db.get("defaultNextBountyTimeTick"));
        serverDBLocal.put("isBountyTimerUp", Db.get("isBountyTimerUp"));
        serverDBLocal.put("name", Db.get("name"));
        serverDBLocal.put("currentBountyUUID", Db.get("currentBountyUUID"));
        serverDBLocal.put("currentBountyName", Db.get("currentBountyName"));
        serverDBLocal.put("isBountyDead", Db.get("isBountyDead"));
        serverDBLocal.put("wasBountyKilledByPlayer", Db.get("wasBountyKilledByPlayer"));
        serverDBLocal.put("isBountyInGame", Db.get("isBountyInGame"));
        serverDBLocal.put("isServerReadyForNextBounty", Db.get("isServerReadyForNextBounty"));
        serverDBLocal.put("nextBountyUUID", Db.get("nextBountyUUID"));
        serverDBLocal.put("nextBountyName", Db.get("nextBountyName"));
        serverDBLocal.put("numberOfPlayersOnline", Db.get("numberOfPlayersOnline"));
        serverDBLocal.put("bountyOfflineTime", Db.get("bountyOfflineTime"));
        serverDBLocal.put("howManyBountys", Db.get("howManyBountys"));

    }

    private void backupServerDB(){

        // Tell all players there might be temporary lag
        for(Player p : Bukkit.getOnlinePlayers()){
            p.sendMessage(ChatColor.RED+"Saving Database"+ChatColor.DARK_RED+"(lag)");
        }

        ArrayList<Bson> ValUpdate = new ArrayList<>();
        for(Map.Entry<String, Object> localDB : serverDBLocal.entrySet()){
            ValUpdate.add(Updates.set(localDB.getKey(), localDB.getValue()));
        }
        serverDatabase.updateMany(Filters.eq("_id", "CriticalSMP"), ValUpdate);

        for(Player p : Bukkit.getOnlinePlayers()){
            p.sendMessage(ChatColor.YELLOW+"...");
        }
        // Saving Player Data Now!
        for(Player p : Bukkit.getOnlinePlayers()){
            UUID uuid = p.getUniqueId();
            Document Db = players.find(new Document("_id", uuid)).first();
            ArrayList<Bson> ValUpdates = new ArrayList<>();
            for(Map.Entry<String, Object> pla : Db.entrySet()){
                if(pla.getKey()==null || pla.getValue()==null) continue;
                ValUpdates.add(Updates.set(pla.getKey(), pla.getValue()));
            }
            players.updateMany(Filters.eq("_id", uuid), ValUpdates);

            p.sendMessage(ChatColor.GREEN+"Saved!");
        }
    }
    //private void backupPlayersDB(){
    //    for(Player p : Bukkit.getOnlinePlayers()){
    //        playerClass pc = playerDBLocal.get(p.getUniqueId());
    //        players.updateOne(Filters.eq("_id", p.getUniqueId()), pc.PlayerDBLocal);
    //    }
    //}
    private void playerLeaveDB(UUID uuid){
        Document Db = players.find(new Document("_id", uuid)).first();
        ArrayList<Bson> ValUpdate = new ArrayList<>();
        for(Map.Entry<String, Object> pla : Db.entrySet()){
            if(pla.getKey()==null || pla.getValue()==null) continue;
            ValUpdate.add(Updates.set(pla.getKey(), pla.getValue()));
        }
        players.updateMany(Filters.eq("_id", uuid), ValUpdate);

        //players.updateOne(Filters.eq("_id", uuid), pc.PlayerDBLocal);
    }

    private Integer getCurrentBountyTimeRemaining() {
        return (int) serverDBLocal.get("nextBountyTimeRemaining");
    }

    private Object dbServerGet(String key) {
        return serverDBLocal.get(key);
    }

    private static void animatedTitleBounty(Player pickerPlayer) {
        //
        ArrayList<Player> alps = new ArrayList<Player>(Bukkit.getOnlinePlayers());

        for(int i = 0; i <= 54; i++){
            //
            Collections.shuffle(alps);
            sendAllTitB(alps.get(new Random().nextInt(alps.size())));


        }
    }
    private static void sendAllTitB(Player p){
        for(Player player : Bukkit.getOnlinePlayers()){
            player.sendTitle(p.getName(), "", 0, 1, 0);
        }
    }

    private void setServerDBStartNexBounty() {
        serverDBLocal.put("nextBountyTimeRemaining", serverDBLocal.get("defaultNextBountyTimeTick"));
        serverDBLocal.put("isBountyTimerUp", false);
        serverDBLocal.put("isBountyDead", false);
        serverDBLocal.put("wasBountyKilledByPlayer", false);
        serverDBLocal.put("isServerReadyForNextBounty", false);
        serverDBLocal.put("currentBountyUUID", "");
        serverDBLocal.put("currentBountyName", "");
        for (UUID uuid : getAllDadabasePlayers()) {
            if (debug) {
                plugin.getLogger().info("Player with UUID: " + uuid + " got reset");
            }
            dpPlayerUUIDEdit(uuid, "isBounty", false);
            dpPlayerUUIDEdit(uuid, "isNextBounty", false);
            dpPlayerUUIDEdit(uuid, "ifKilledByBounty", false);
        }
        //if(debug){
        //    plugin.getLogger().info("");
        //}
        if (debug) {
            plugin.getLogger().info("");
        }
    }

    // Set player bounty
    // Set time remaining for bounty
    // if bounty leaves, wait around 5 min, then if the player is back, continue count down, if not find new player

    // run async task every 5 hours, not repeat, as we might need to stop the loop from running for a bit
    private void startNextBounty() {
        //checking if there is enough people
        if (Bukkit.getOnlinePlayers().size() < 2) {
            // not enough people, going to wait...
//            plugin.getLogger().warning("Not enough players");
            if (debug) {
                plugin.getLogger().info(Bukkit.getOnlinePlayers().size() + "<2");
            }
            //lock.unlock();
            return;
        }
        setServerDBStartNexBounty();
        // Get and set bounty
        setNewBounty();
    }

    private void setNewBounty() {
        //
        ArrayList<Player> allPlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        int ranPl = new Random().nextInt(allPlayers.size());
        Player pickedPlayer = allPlayers.get(ranPl);

        // add cool animated title

        if (debug) {
            plugin.getLogger().info("Picked player name: " + pickedPlayer.getName() + "\nPicker Player UUID: " + pickedPlayer.getUniqueId());
        }
        //edit player scoreboards
        String lastpNAME;
        if ((int) dbServerGet("howManyBountys") == 0) {
            lastpNAME = "INIT";
        } else {
            lastpNAME = (String) dbServerGet("currentBountyName");
            // Change player lives
            //dbPlayerEdit(Bukkit.getPlayer((UUID) dbServerGet("currentBountyUUID")), "lives", (int) dbPlayerGet((UUID) dbServerGet("currentBountyUUID"), "lives")-1);
            //Bukkit.getPluginManager().callEvent(removeLifeEvent);
            //removeLifeEvent.removeLifeFromPlayer(Bukkit.getPlayer((UUID) dbServerGet("currentBountyUUID")));
        }

        for (Player olp : allPlayers) {
            editPlayerScoreBoard(olp, "Current Bounty: " + lastpNAME, "Current Bounty: " + pickedPlayer.getName(), 6);
        }

        for(Player player : Bukkit.getOnlinePlayers()){
            player.sendTitle(ChatColor.LIGHT_PURPLE+dbServerGet("currentBountyName").toString(), ChatColor.LIGHT_PURPLE+pickedPlayer.getName()+ChatColor.RED+" is the new bounty! BEWARE!!", 1, 40, 10);
            player.setCompassTarget(pickedPlayer.getLocation());
        }

        dbServerEdit("currentBountyUUID", pickedPlayer.getUniqueId());
        dbServerEdit("currentBountyName", pickedPlayer.getName());
        dbPlayerEdit(pickedPlayer, "isBounty", true);
        dbServerEdit("howManyBountys", (int) dbServerGet("howManyBountys") + 1);
        //lock.unlock();
    }

    public int secondsToTicks(int Seconds) {
        // 20 ticks per 1 second
        return Seconds * 20;
    }

    private void tick() {
        //plugin.getLogger().warning("Running tick");
        if (lock.tryLock()) {
            if (debug) {
                plugin.getLogger().warning("Tick is locked");
            }
            return;
        }
        lock.lock();
        if (debug) {
            plugin.getLogger().warning("tick isn't locked, locking and running code");
        }

        if(!plugin.serverData.getBoolean("started")){
            // fututerhit hasn't started the server yet
            return;
        }
//

        // Every runnable tick
        Integer bt = getCurrentBountyTimeRemaining();
        if (bt > 0) {
            if (Bukkit.getPlayer((UUID) dbServerGet("currentBountyUUID")) == null) {
                plugin.getLogger().warning("current bounty isn't online");
                int op = Bukkit.getOnlinePlayers().size();
                if (op < 2) {
                    // no player online
                    if (debug) {
                        plugin.getLogger().info(op + "<2\nunlocked");
                    }
                    lock.unlock();
                    return;
                }
                dbServerEdit("bountyOfflineTime", (int) dbServerGet("bountyOfflineTime") - 1);
                if (debug) {
                    plugin.getLogger().info("bounty offline time: " + dbServerGet("bountyOfflineTime"));
                }
                lock.unlock();
                return;
            }
            if (Bukkit.getOnlinePlayers().size() < 2) {
                // not enough player to resume
                if (debug) {
                    plugin.getLogger().info(Bukkit.getOnlinePlayers().size() + "<2\nunlocked");
                }
                lock.unlock();
                return;
            }

            dbServerEdit("nextBountyTimeRemaining", bt - 1);
            lock.unlock();

        } else if (bt == 0) {
//            plugin.getLogger().warning("no bounty Time");
            //checking to see if this is the first time running
            if ((int) dbServerGet("howManyBountys") == 0) {
                // probably the first time running
                //
                if (debug) {
                    plugin.getLogger().warning("Probably the first time running, starting bounty");
                }
//
                startNextBounty();
                return;
            }


//            plugin.getLogger().warning("DB time is up");
            // Check to see if the server is ready for next bounty
            dbServerEdit("isBountyTimerUp", true);
            if ((Boolean) dbServerGet("wasBountyKilledByPlayer")) {
//                plugin.getLogger().warning("Start nex bounty");
                if (debug) {
                    plugin.getLogger().info("Bounty was killed by player, next");
                }
                startNextBounty();
                return;
                //
            }
            lock.unlock();
        }
        lock.unlock();
    }

    // add body from event
    public void spawnCorpseForAll(Player deadPerson) {
        CraftPlayer craftPlayer = (CraftPlayer) deadPerson;
        MinecraftServer server = craftPlayer.getHandle().getServer();
        ServerLevel level = craftPlayer.getHandle().getLevel();

        ServerPlayer npc = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), ChatColor.stripColor(deadPerson.getDisplayName())));


        //npc.setYHeadRot(deadPerson.getLocation().getDirection().angle(deadPerson.getLocation().getDirection()));


        npc.setPos(deadPerson.getLocation().getX(), deadPerson.getLocation().getWorld().getHighestBlockAt(deadPerson.getLocation()).getY() + 1, deadPerson.getLocation().getZ());
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
        for (Player p : Bukkit.getOnlinePlayers()) {
            ServerGamePacketListenerImpl ps = ((CraftPlayer) p).getHandle().connection;
            ps.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, npc));
            ps.send(new ClientboundAddPlayerPacket(npc));
            ps.send(new ClientboundSetEntityDataPacket(npc.getId(), npc.getEntityData(), true));
            ps.send(new ClientboundRotateHeadPacket(npc, (byte) (npc.getYHeadRot()*256/360)));


            // Remove body after some time
            //new BukkitRunnable(){
            //    @Override
            //    public void run(){
            //        ps.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, npc));
            //    }
            //}.runTaskLaterAsynchronously(plugin, 20L);
        }
    }

    private void spawnCorpseForPlayer(Player p, UUID deadPerson) {
        CraftPlayer craftPlayer = (CraftPlayer) p;
        MinecraftServer server = craftPlayer.getHandle().getServer();
        ServerLevel level = craftPlayer.getHandle().getLevel();

        ServerPlayer npc = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), ChatColor.stripColor((String) dbPlayerGet(deadPerson, "username"))));

        String wname = (String) dbPlayerGet(deadPerson, "deadBodyLocationWorld");
        Double BX = (Double) dbPlayerGet(deadPerson, "deadBodyLocationX");
        Double BY = (Double) dbPlayerGet(deadPerson, "deadBodyLocationY");
        ;
        Double BZ = (Double) dbPlayerGet(deadPerson, "deadBodyLocationz");
        ;
        Location deadPersonLocation = new Location(Bukkit.getWorld(wname), BX, BY, BZ);

        npc.setPos(deadPersonLocation.getX(), deadPersonLocation.getWorld().getHighestBlockAt(deadPersonLocation).getY() + 1, deadPersonLocation.getZ());
        npc.setPose(Pose.SLEEPING);
        npc.setCustomNameVisible(false);

        // Red Crewmate skin
        String texture = "ewogICJ0aW1lc3RhbXAiIDogMTYwMDQxNDA0MjAxOSwKICAicHJvZmlsZUlkIiA6ICJlYTk3YThkMTFmNzE0Y2UwYTc2ZDdjNTI1M2NjN2Y3MiIsCiAgInByb2ZpbGVOYW1lIiA6ICJfTXJfS2VrcyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9iY2UzOTRlM2VlMDc1OWM5YzZiMWZhMmNhMDk0YThlODdhZWExNTMxYWRiMGU4NmM5Zjk1YTgzYzQ0NTI5ZTEwIgogICAgfQogIH0KfQ==";
        String signature = "oVGXKvwSSfRI0qUR5zXXIzIur4VdckCpiFQIEi/zYy/0XxXHmueHn1FCOP19kvpuCHTzSUxrSasuFtpWV5GDMUKRegYXkEFNOVPNjlkr3UeGqk+bCbVxGXSKkJM78mRCnv1rSDZmI7QOtshh67sS3IGVPRYV9T2IEEH+phXIFewRNjwgYtr7UVWMm74fqfiXirerxIgwGpp+eq8XiJ2DfCfKYaBDCns41FHoX7B5nBVLzqDlQW931e5TgTyDAiu3YWk3ASi9fiawCsTM11fLTJ/VEq9FdOtFXpxCzYUgY5Xy1I0xtRhSEk4LnMe79ffzsXp1noZhy8iKg4nxSLVKBK78dLX8JFj+cMiRkqoGACSJOk4iA4diLhEeRL6jG+clv5Xxv9NfEkM6AGZFwGAlgGWZxcy71ZlzTtFCaTu2LCFxki2B2xNe/JdmPyDEwankv7hcWxon/zCLBukF/aGj2Bzam4b/Bjz6e0vSqElWb8KLTQC46hDAEeqngrDDSaiig7vs5rkgvUmPmyD2qAlI9dXgDcDPmJGlma7jmZiVndDzdDfBlmcIp4o3J2ECMyJYhcY+KWxV20RxAjK71jfLmXOQbRNVX1dZq7uSHlHR/XBjmeP0a+SxHV6fuWsWHvXaBsnLJlRdYrPSnFx+GMwZmYvNty4aS1B2ty7aTfIFLzo=";


        //GameProfile gameProfile = ((CraftPlayer) deadPerson).getHandle().getGameProfile();
        //Property property = (Property) gameProfile.getProperties().get("textures").toArray()[0];
        if (dbPlayerGet(deadPerson, "SkinTexture") != "" && dbPlayerGet(deadPerson, "SkinSignature") != "") {
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

    private void getDeadPlayers() {
        ArrayList<UUID> allJoinedPlayers = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            allJoinedPlayers.add(p.getUniqueId());
        }
        FindIterable<Document> iterDoc = players.find();
        for (Document document : iterDoc) {
            allJoinedPlayers.add((UUID) document.get("_id"));
        }

        for (UUID u : allJoinedPlayers) {
            Integer pLives = getPlayerLives(u);
            if (pLives == 0) {
                deadPlsSpawnBody.add(u);
            }
        }
    }

    private ArrayList<UUID> getAllServerPlayers() {
        ArrayList<UUID> allJoinedPlayers = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            allJoinedPlayers.add(p.getUniqueId());
        }
        FindIterable<Document> iterDoc = players.find();
        for (Document document : iterDoc) {
            allJoinedPlayers.add((UUID) document.get("_id"));
        }
        return allJoinedPlayers;
    }

    private ArrayList<UUID> getAllDadabasePlayers() {
        ArrayList<UUID> allJoinedPlayers = new ArrayList<>();
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
        if (cmd.getName().equalsIgnoreCase("bdussy")) {
            if(player.isOp()){
                startNextBounty();
                plugin.serverData.set("started", true);
                try {
                    plugin.serverData.save(plugin.data);
                } catch (IOException e) {
                    e.printStackTrace();
                }


                //long s = System.currentTimeMillis()+TimeUnit.HOURS.toMillis(12);
                //                plugin.serverData.set("endFreeTime", s);
                //                try {
                //                    plugin.serverData.save(plugin.data);
                //                } catch (IOException e) {
                //                    e.printStackTrace();
                //                }
            }

            return true;
        }
        if(cmd.getName().equalsIgnoreCase("donate")){
            player.sendMessage(
                    ChatColor.DARK_BLUE+"Paypal"+ChatColor.RESET+": "+ChatColor.BLUE+"https://www.paypal.com/donate/?business=6BPJ86S23NJ8S&no_recurring=0&item_name=Donate+to+Row%21&currency_code=USD" + "\n" +
                    ChatColor.GREEN+"Cashapp"+ChatColor.RESET+": "+ChatColor.DARK_GREEN+"$weerow"
                    );
            return true;
        }
        return true;
    }

    private void sp(Player p){
        CraftPlayer craftPlayer = (CraftPlayer) p;
        MinecraftServer server = craftPlayer.getHandle().getServer();
        ServerLevel level = craftPlayer.getHandle().getLevel();

        ServerPlayer npc = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), ChatColor.stripColor(p.getDisplayName())));
        npc.setPos(p.getLocation().getX(), p.getLocation().getY(), p.getLocation().getZ());

        // Red Crewmate skin
        String texture = "ewogICJ0aW1lc3RhbXAiIDogMTYwMDQxNDA0MjAxOSwKICAicHJvZmlsZUlkIiA6ICJlYTk3YThkMTFmNzE0Y2UwYTc2ZDdjNTI1M2NjN2Y3MiIsCiAgInByb2ZpbGVOYW1lIiA6ICJfTXJfS2VrcyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9iY2UzOTRlM2VlMDc1OWM5YzZiMWZhMmNhMDk0YThlODdhZWExNTMxYWRiMGU4NmM5Zjk1YTgzYzQ0NTI5ZTEwIgogICAgfQogIH0KfQ==";
        String signature = "oVGXKvwSSfRI0qUR5zXXIzIur4VdckCpiFQIEi/zYy/0XxXHmueHn1FCOP19kvpuCHTzSUxrSasuFtpWV5GDMUKRegYXkEFNOVPNjlkr3UeGqk+bCbVxGXSKkJM78mRCnv1rSDZmI7QOtshh67sS3IGVPRYV9T2IEEH+phXIFewRNjwgYtr7UVWMm74fqfiXirerxIgwGpp+eq8XiJ2DfCfKYaBDCns41FHoX7B5nBVLzqDlQW931e5TgTyDAiu3YWk3ASi9fiawCsTM11fLTJ/VEq9FdOtFXpxCzYUgY5Xy1I0xtRhSEk4LnMe79ffzsXp1noZhy8iKg4nxSLVKBK78dLX8JFj+cMiRkqoGACSJOk4iA4diLhEeRL6jG+clv5Xxv9NfEkM6AGZFwGAlgGWZxcy71ZlzTtFCaTu2LCFxki2B2xNe/JdmPyDEwankv7hcWxon/zCLBukF/aGj2Bzam4b/Bjz6e0vSqElWb8KLTQC46hDAEeqngrDDSaiig7vs5rkgvUmPmyD2qAlI9dXgDcDPmJGlma7jmZiVndDzdDfBlmcIp4o3J2ECMyJYhcY+KWxV20RxAjK71jfLmXOQbRNVX1dZq7uSHlHR/XBjmeP0a+SxHV6fuWsWHvXaBsnLJlRdYrPSnFx+GMwZmYvNty4aS1B2ty7aTfIFLzo=";


        GameProfile gameProfile = ((CraftPlayer) p).getHandle().getGameProfile();
        Property property = (Property) gameProfile.getProperties().get("textures").toArray()[0];

        texture = property.getValue();
        signature = property.getSignature();

        npc.getGameProfile().getProperties().put("textures", new Property("textures", texture, signature));

        ServerGamePacketListenerImpl ps = ((CraftPlayer) p).getHandle().connection;
        ps.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, npc));
        ps.send(new ClientboundAddPlayerPacket(npc));
        ps.send(new ClientboundSetEntityDataPacket(npc.getId(), npc.getEntityData(), true));
        ps.send(new ClientboundRotateHeadPacket(npc, (byte) (npc.getYHeadRot()*256/360)));
    }
}