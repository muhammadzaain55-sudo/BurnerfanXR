package com.example.minecraftai.data;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class PlayerState {
    
    // Basic player info
    private final String playerName;
    private final long timestamp;
    
    // Health and status
    private final double health;
    private final double maxHealth;
    private final int foodLevel;
    private final float saturation;
    private final int airLevel;
    private final boolean isOnGround;
    private final boolean isInWater;
    private final boolean isInLava;
    private final boolean isSneaking;
    private final boolean isSprinting;
    private final boolean isFlying;
    
    // Location and movement
    private final double x, y, z;
    private final float yaw, pitch;
    private final String worldName;
    private final double velocityX, velocityY, velocityZ;
    
    // Inventory
    private final Map<Integer, ItemStack> inventory;
    private final ItemStack mainHand;
    private final ItemStack offHand;
    private final ItemStack helmet, chestplate, leggings, boots;
    
    // Experience
    private final int level;
    private final float exp;
    private final int totalExperience;
    
    // Game mode and effects
    private final String gameMode;
    private final Map<String, Integer> potionEffects;
    
    // Environmental data
    private final Material blockBelow;
    private final int lightLevel;
    private final boolean isRaining;
    private final boolean isThundering;
    private final long worldTime;
    
    public PlayerState(Player player) {
        this.playerName = player.getName();
        this.timestamp = System.currentTimeMillis();
        
        // Health and status
        this.health = player.getHealth();
        this.maxHealth = player.getMaxHealth();
        this.foodLevel = player.getFoodLevel();
        this.saturation = player.getSaturation();
        this.airLevel = player.getRemainingAir();
        this.isOnGround = player.isOnGround();
        this.isInWater = player.isInWater();
        this.isInLava = player.getLocation().getBlock().getType() == Material.LAVA;
        this.isSneaking = player.isSneaking();
        this.isSprinting = player.isSprinting();
        this.isFlying = player.isFlying();
        
        // Location and movement
        Location loc = player.getLocation();
        this.x = loc.getX();
        this.y = loc.getY();
        this.z = loc.getZ();
        this.yaw = loc.getYaw();
        this.pitch = loc.getPitch();
        this.worldName = loc.getWorld().getName();
        this.velocityX = player.getVelocity().getX();
        this.velocityY = player.getVelocity().getY();
        this.velocityZ = player.getVelocity().getZ();
        
        // Inventory
        this.inventory = new HashMap<>();
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                this.inventory.put(i, contents[i].clone());
            }
        }
        
        this.mainHand = player.getInventory().getItemInMainHand();
        this.offHand = player.getInventory().getItemInOffHand();
        this.helmet = player.getInventory().getHelmet();
        this.chestplate = player.getInventory().getChestplate();
        this.leggings = player.getInventory().getLeggings();
        this.boots = player.getInventory().getBoots();
        
        // Experience
        this.level = player.getLevel();
        this.exp = player.getExp();
        this.totalExperience = player.getTotalExperience();
        
        // Game mode and effects
        this.gameMode = player.getGameMode().name();
        this.potionEffects = new HashMap<>();
        player.getActivePotionEffects().forEach(effect -> 
            potionEffects.put(effect.getType().getName(), effect.getAmplifier())
        );
        
        // Environmental data
        Location below = loc.clone().subtract(0, 1, 0);
        this.blockBelow = below.getBlock().getType();
        this.lightLevel = loc.getBlock().getLightLevel();
        this.isRaining = loc.getWorld().hasStorm();
        this.isThundering = loc.getWorld().isThundering();
        this.worldTime = loc.getWorld().getTime();
    }
    
    // Getters
    public String getPlayerName() { return playerName; }
    public long getTimestamp() { return timestamp; }
    public double getHealth() { return health; }
    public double getMaxHealth() { return maxHealth; }
    public int getFoodLevel() { return foodLevel; }
    public float getSaturation() { return saturation; }
    public int getAirLevel() { return airLevel; }
    public boolean isOnGround() { return isOnGround; }
    public boolean isInWater() { return isInWater; }
    public boolean isInLava() { return isInLava; }
    public boolean isSneaking() { return isSneaking; }
    public boolean isSprinting() { return isSprinting; }
    public boolean isFlying() { return isFlying; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public String getWorldName() { return worldName; }
    public double getVelocityX() { return velocityX; }
    public double getVelocityY() { return velocityY; }
    public double getVelocityZ() { return velocityZ; }
    public Map<Integer, ItemStack> getInventory() { return inventory; }
    public ItemStack getMainHand() { return mainHand; }
    public ItemStack getOffHand() { return offHand; }
    public ItemStack getHelmet() { return helmet; }
    public ItemStack getChestplate() { return chestplate; }
    public ItemStack getLeggings() { return leggings; }
    public ItemStack getBoots() { return boots; }
    public int getLevel() { return level; }
    public float getExp() { return exp; }
    public int getTotalExperience() { return totalExperience; }
    public String getGameMode() { return gameMode; }
    public Map<String, Integer> getPotionEffects() { return potionEffects; }
    public Material getBlockBelow() { return blockBelow; }
    public int getLightLevel() { return lightLevel; }
    public boolean isRaining() { return isRaining; }
    public boolean isThundering() { return isThundering; }
    public long getWorldTime() { return worldTime; }
}