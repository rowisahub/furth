package com.rowisabeast.futurehit.event;

import com.rowisabeast.futurehit.criticalSMP;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GiveLifeEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean isCancelled;
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    @Override
    public boolean isCancelled() {
        return this.isCancelled;
    }
    @Override
    public void setCancelled(boolean isCancelled) {
        this.isCancelled = isCancelled;
    }
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    private final criticalSMP SMP;
    // cSMP
    public GiveLifeEvent(criticalSMP c){
        this.SMP = c;
    }

    public void addLifeToPlayer(Player p){
        SMP.dbPlayerEdit(p, "lives", (int)SMP.dbPlayerGet(p.getUniqueId(), "lives")+1);
        SMP.setPlayerTabNameWithLives(p);
        SMP.editPlayerScoreBoard(p, "Lives: "+((int)SMP.dbPlayerGet(p.getUniqueId(), "lives")-1), "Lives: "+(int)SMP.dbPlayerGet(p.getUniqueId(), "lives"), 9);
        messageAllPlayersPlayerHasGainedALife(p);
        consoleAnu(p);
    }

    private void messageAllPlayersPlayerHasGainedALife(Player p){
        for (Player pl : Bukkit.getOnlinePlayers()){
            if(p.equals(pl)) return;
            //pl.sendMessage(ChatColor.LIGHT_PURPLE+p.getName()+ChatColor.RED+" has gained a LIFE, beware!");
            pl.sendTitle(ChatColor.LIGHT_PURPLE+p.getName()+ChatColor.RED+" has gained a LIFE, beware!", "", 1, 20, 1);
        }
    }

    private void consoleAnu(Player p){
        SMP.plugin.getLogger().info(p.getName()+" got a life!");
    }

}
