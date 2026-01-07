package com.example.minecraftai.ai;

import com.example.minecraftai.MinecraftAIPlugin;
import com.example.minecraftai.data.PlayerState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.nd4j.linalg.api.ndarray.INDArray;

public class AIPlayerController {
    
    private final Player player;
    private final NeuralNetworkManager networkManager;
    private final MinecraftAIPlugin plugin;
    private boolean isActive = false;
    
    // Action thresholds
    private static final double ACTION_THRESHOLD = 0.3;
    private static final double MOVEMENT_SPEED = 0.2;
    private static final double JUMP_VELOCITY = 0.42;
    
    // Action indices
    private static final int ACTION_FORWARD = 0;
    private static final int ACTION_BACKWARD = 1;
    private static final int ACTION_JUMP = 2;
    private static final int ACTION_SNEAK = 3;
    private static final int ACTION_SPRINT = 4;
    
    public AIPlayerController(Player player, NeuralNetworkManager networkManager, MinecraftAIPlugin plugin) {
        this.player = player;
        this.networkManager = networkManager;
        this.plugin = plugin;
    }
    
    public void activate() {
        isActive = true;
        plugin.getLogger().info("AI controller activated for " + player.getName());
    }
    
    public void deactivate() {
        isActive = false;
        plugin.getLogger().info("AI controller deactivated for " + player.getName());
    }
    
    public void update() {
        if (!isActive || !player.isOnline()) {
            return;
        }
        
        try {
            // Get current player state
            PlayerState currentState = new PlayerState(player);
            
            // Get AI decision
            INDArray prediction = networkManager.predict(currentState);
            
            // Execute actions based on prediction
            executeActions(prediction);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error in AI controller update: " + e.getMessage());
        }
    }
    
    private void executeActions(INDArray prediction) {
        // Extract action probabilities
        double[] actions = prediction.toDoubleVector();
        
        // Movement actions
        Vector movement = new Vector(0, 0, 0);
        boolean shouldJump = false;
        boolean shouldSneak = false;
        boolean shouldSprint = false;
        
        // Forward/Backward movement
        if (actions[ACTION_FORWARD] > ACTION_THRESHOLD) {
            movement.setZ(MOVEMENT_SPEED);
        } else if (actions[ACTION_BACKWARD] > ACTION_THRESHOLD) {
            movement.setZ(-MOVEMENT_SPEED);
        }
        
        // Jump action
        if (actions[ACTION_JUMP] > ACTION_THRESHOLD && player.isOnGround()) {
            shouldJump = true;
        }
        
        // Sneak action
        if (actions[ACTION_SNEAK] > ACTION_THRESHOLD) {
            shouldSneak = true;
        }
        
        // Sprint action
        if (actions[ACTION_SPRINT] > ACTION_THRESHOLD && !shouldSneak) {
            shouldSprint = true;
        }
        
        // Apply movement
        if (movement.lengthSquared() > 0) {
            // Rotate movement vector based on player's yaw
            double yaw = Math.toRadians(player.getLocation().getYaw());
            double cos = Math.cos(yaw);
            double sin = Math.sin(yaw);
            
            double newX = movement.getX() * cos - movement.getZ() * sin;
            double newZ = movement.getX() * sin + movement.getZ() * cos;
            
            movement.setX(newX);
            movement.setZ(newZ);
            
            // Apply velocity
            Vector currentVelocity = player.getVelocity();
            currentVelocity.add(movement);
            player.setVelocity(currentVelocity);
        }
        
        // Apply jump
        if (shouldJump) {
            Vector velocity = player.getVelocity();
            velocity.setY(JUMP_VELOCITY);
            player.setVelocity(velocity);
        }
        
        // Apply sneak
        if (shouldSneak != player.isSneaking()) {
            player.setSneaking(shouldSneak);
        }
        
        // Apply sprint
        if (shouldSprint != player.isSprinting()) {
            player.setSprinting(shouldSprint);
        }
        
        // Special actions based on context
        performContextualActions(actions);
    }
    
    private void performContextualActions(double[] actions) {
        // MLG Water Bucket
        if (player.getFallDistance() > 10 && hasWaterBucket()) {
            // Look down and place water
            Location loc = player.getLocation();
            loc.setPitch(90); // Look straight down
            player.teleport(loc);
            
            // Use water bucket
            player.getInventory().getItemInMainHand().setType(Material.WATER_BUCKET);
            // Simulate right-click to place water
            // Note: This is simplified - in a real implementation, you'd need to handle block placement
        }
        
        // Food consumption when low health
        if (player.getHealth() < 10 && hasFood()) {
            // Find and consume food
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                if (player.getInventory().getItem(i) != null && isFood(player.getInventory().getItem(i).getType())) {
                    player.getInventory().setHeldItemSlot(i);
                    // Simulate eating (right-click hold)
                    break;
                }
            }
        }
        
        // Block placement for bridging
        if (isOverVoid() && hasBlocks()) {
            // Look down and place block
            Location loc = player.getLocation();
            loc.setPitch(90);
            player.teleport(loc);
            
            // Place block below
            Location below = player.getLocation().subtract(0, 1, 0);
            if (below.getBlock().getType() == Material.AIR) {
                // Simulate block placement
                // Note: This is simplified - in a real implementation, you'd need proper block placement logic
            }
        }
    }
    
    // Helper methods
    private boolean hasWaterBucket() {
        return player.getInventory().contains(Material.WATER_BUCKET);
    }
    
    private boolean hasFood() {
        return player.getInventory().contains(Material.BREAD) ||
               player.getInventory().contains(Material.COOKED_BEEF) ||
               player.getInventory().contains(Material.APPLE) ||
               player.getInventory().contains(Material.COOKED_CHICKEN);
    }
    
    private boolean hasBlocks() {
        return player.getInventory().contains(Material.COBBLESTONE) ||
               player.getInventory().contains(Material.DIRT) ||
               player.getInventory().contains(Material.STONE);
    }
    
    private boolean isFood(Material material) {
        return material == Material.BREAD ||
               material == Material.COOKED_BEEF ||
               material == Material.APPLE ||
               material == Material.COOKED_CHICKEN ||
               material == Material.CARROT ||
               material == Material.POTATO;
    }
    
    private boolean isOverVoid() {
        Location below = player.getLocation().subtract(0, 2, 0);
        return below.getY() < 5 && below.getBlock().getType() == Material.AIR;
    }
    
    public void reset() {
        // Reset AI state after death/respawn
        isActive = true;
        plugin.getLogger().info("AI controller reset for " + player.getName());
    }
    
    public boolean isActive() {
        return isActive && player.isOnline();
    }
    
    public Player getPlayer() {
        return player;
    }
}