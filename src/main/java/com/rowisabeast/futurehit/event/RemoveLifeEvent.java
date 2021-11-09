package com.rowisabeast.futurehit.event;

import com.rowisabeast.futurehit.criticalSMP;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.PlayerDeathEvent;

public class RemoveLifeEvent extends Event implements Cancellable {
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
    public RemoveLifeEvent(criticalSMP c){
        this.SMP = c;
    }

    public void removeLifeFromPlayer(Player killedPlayer){
        SMP.dbPlayerEdit(killedPlayer, "lives", (int)SMP.dbPlayerGet(killedPlayer.getUniqueId(), "lives")-1);
        SMP.dbPlayerEdit(killedPlayer, "numberOfTImesPlayerHasDiedAsBounty", (int) SMP.dbPlayerGet(killedPlayer.getUniqueId(),"numberOfTImesPlayerHasDiedAsBounty")+1);
        SMP.setPlayerTabNameWithLives(killedPlayer);
        SMP.editPlayerScoreBoard("Lives: "+((int)SMP.dbPlayerGet(killedPlayer.getUniqueId(), "lives")+1), "Lives: "+(int)SMP.dbPlayerGet(killedPlayer.getUniqueId(), "lives"), 9);
        //messageAllPlayersPlayerHasLostALife(p);
        consoleAnu(killedPlayer);
    }

    private void messageAllPlayersPlayerHasLostALife(Player p){
        for (Player pl : Bukkit.getOnlinePlayers()){
            if(p.equals(pl)) return;
            //pl.sendMessage(ChatColor.LIGHT_PURPLE+p.getName()+ChatColor.RED+" has gained a LIFE, beware!");
            pl.sendTitle(ChatColor.LIGHT_PURPLE+p.getName()+ChatColor.RED+" has lost a LIFE, beware!", "", 1, 20, 1);
        }
    }

    private void consoleAnu(Player p){
        SMP.plugin.getLogger().info(p.getName()+" lost a life!");
    }

}
