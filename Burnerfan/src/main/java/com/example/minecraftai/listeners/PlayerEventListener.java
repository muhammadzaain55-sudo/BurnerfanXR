package com.example.minecraftai.listeners;

import com.example.minecraftai.ai.AIAgent;
import com.example.minecraftai.data.PlayerStateCollector;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

public class PlayerEventListener implements Listener {
    
    private final PlayerStateCollector stateCollector;
    private final AIAgent aiAgent;
    
    public PlayerEventListener(PlayerStateCollector stateCollector, AIAgent aiAgent) {
        this.stateCollector = stateCollector;
        this.aiAgent = aiAgent;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Record movement data
        String moveData = String.format("from:%.2f,%.2f,%.2f to:%.2f,%.2f,%.2f", 
            event.getFrom().getX(), event.getFrom().getY(), event.getFrom().getZ(),
            event.getTo().getX(), event.getTo().getY(), event.getTo().getZ());
        
        stateCollector.recordEvent(player, "MOVE", moveData);
        
        // Check for MLG scenarios (falling with potential water bucket save)
        if (player.getVelocity().getY() < -0.5 && player.getFallDistance() > 10) {
            if (player.getInventory().contains(org.bukkit.Material.WATER_BUCKET)) {
                stateCollector.recordEvent(player, "MLG_OPPORTUNITY", 
                    "fall_distance:" + player.getFallDistance() + ",has_water_bucket:true");
            }
        }
        
        // Notify AI agent of movement
        aiAgent.onPlayerMove(player, event.getFrom(), event.getTo());
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        String deathData = String.format("cause:%s,location:%.2f,%.2f,%.2f,items_dropped:%d",
            event.getEntity().getLastDamageCause() != null ? 
                event.getEntity().getLastDamageCause().getCause().name() : "UNKNOWN",
            player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(),
            event.getDrops().size());
        
        stateCollector.recordEvent(player, "DEATH", deathData);
        aiAgent.onPlayerDeath(player, event);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        String damageData = String.format("cause:%s,damage:%.2f,final_health:%.2f",
            event.getCause().name(), event.getFinalDamage(), 
            player.getHealth() - event.getFinalDamage());
        
        stateCollector.recordEvent(player, "DAMAGE", damageData);
        aiAgent.onPlayerDamage(player, event);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJump(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Detect jumping (Y velocity increase while on ground)
        if (event.getFrom().getY() < event.getTo().getY() && 
            player.isOnGround() && player.getVelocity().getY() > 0.1) {
            
            stateCollector.recordEvent(player, "JUMP", 
                "velocity_y:" + player.getVelocity().getY());
            aiAgent.onPlayerJump(player);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        String interactData = String.format("action:%s,item:%s,block:%s",
            event.getAction().name(),
            event.getItem() != null ? event.getItem().getType().name() : "NONE",
            event.getClickedBlock() != null ? event.getClickedBlock().getType().name() : "NONE");
        
        stateCollector.recordEvent(player, "INTERACT", interactData);
        
        // Special handling for water bucket (MLG water bucket)
        if (event.getItem() != null && 
            event.getItem().getType() == org.bukkit.Material.WATER_BUCKET &&
            player.getFallDistance() > 5) {
            
            stateCollector.recordEvent(player, "MLG_WATER_BUCKET", 
                "fall_distance:" + player.getFallDistance());
        }
        
        aiAgent.onPlayerInteract(player, event);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        
        stateCollector.recordEvent(player, "SNEAK_TOGGLE", 
            "sneaking:" + event.isSneaking());
        aiAgent.onPlayerSneak(player, event.isSneaking());
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
        Player player = event.getPlayer();
        
        stateCollector.recordEvent(player, "SPRINT_TOGGLE", 
            "sprinting:" + event.isSprinting());
        aiAgent.onPlayerSprint(player, event.isSprinting());
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        
        String itemData = String.format("from_slot:%d,to_slot:%d,item:%s",
            event.getPreviousSlot(), event.getNewSlot(),
            player.getInventory().getItem(event.getNewSlot()) != null ?
                player.getInventory().getItem(event.getNewSlot()).getType().name() : "NONE");
        
        stateCollector.recordEvent(player, "ITEM_HELD_CHANGE", itemData);
        aiAgent.onPlayerItemChange(player, event);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        String respawnData = String.format("location:%.2f,%.2f,%.2f,bed_spawn:%s",
            event.getRespawnLocation().getX(), 
            event.getRespawnLocation().getY(), 
            event.getRespawnLocation().getZ(),
            event.isBedSpawn());
        
        stateCollector.recordEvent(player, "RESPAWN", respawnData);
        aiAgent.onPlayerRespawn(player, event);
    }
}