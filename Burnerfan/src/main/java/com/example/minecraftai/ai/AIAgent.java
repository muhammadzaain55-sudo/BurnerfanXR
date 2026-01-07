package com.example.minecraftai.ai;

import com.example.minecraftai.MinecraftAIPlugin;
import com.example.minecraftai.data.PlayerState;
import com.example.minecraftai.data.PlayerStateCollector;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AIAgent {
    
    private final MinecraftAIPlugin plugin;
    private final PlayerStateCollector stateCollector;
    private final Map<UUID, AIPlayerController> playerControllers;
    private final NeuralNetworkManager networkManager;
    private boolean isActive = false;
    private BukkitRunnable aiUpdateTask;
    
    // AI decision intervals
    private static final int AI_UPDATE_INTERVAL_TICKS = 10; // Update every 10 ticks (0.5 seconds)
    
    public AIAgent(MinecraftAIPlugin plugin, PlayerStateCollector stateCollector) {
        this.plugin = plugin;
        this.stateCollector = stateCollector;
        this.playerControllers = new ConcurrentHashMap<>();
        this.networkManager = new NeuralNetworkManager(plugin);
        
        // Initialize neural network
        networkManager.initializeNetwork();
    }
    
    public void startAI() {
        if (isActive) {
            return;
        }
        
        isActive = true;
        
        // Start AI update task
        aiUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAI();
            }
        };
        aiUpdateTask.runTaskTimer(plugin, 0L, AI_UPDATE_INTERVAL_TICKS);
        
        plugin.getLogger().info("AI Agent started");
    }
    
    public void stopAI() {
        if (!isActive) {
            return;
        }
        
        isActive = false;
        
        if (aiUpdateTask != null) {
            aiUpdateTask.cancel();
            aiUpdateTask = null;
        }
        
        // Stop all player controllers
        playerControllers.clear();
        
        plugin.getLogger().info("AI Agent stopped");
    }
    
    private void updateAI() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            AIPlayerController controller = playerControllers.get(player.getUniqueId());
            if (controller != null && controller.isActive()) {
                controller.update();
            }
        }
    }
    
    public void enableAIForPlayer(Player player) {
        AIPlayerController controller = new AIPlayerController(player, networkManager, plugin);
        playerControllers.put(player.getUniqueId(), controller);
        controller.activate();
        
        plugin.getLogger().info("AI enabled for player: " + player.getName());
    }
    
    public void disableAIForPlayer(Player player) {
        AIPlayerController controller = playerControllers.remove(player.getUniqueId());
        if (controller != null) {
            controller.deactivate();
        }
        
        plugin.getLogger().info("AI disabled for player: " + player.getName());
    }
    
    // Event handlers for training data
    public void onPlayerMove(Player player, Location from, Location to) {
        if (!isActive) return;
        
        // Train network with movement data
        PlayerState state = new PlayerState(player);
        INDArray input = networkManager.convertStateToInput(state);
        
        // Create target output based on movement decision
        INDArray target = createMovementTarget(from, to);
        
        // Train the network
        networkManager.trainNetwork(input, target);
    }
    
    public void onPlayerDeath(Player player, PlayerDeathEvent event) {
        if (!isActive) return;
        
        // Negative reward for death
        PlayerState state = new PlayerState(player);
        INDArray input = networkManager.convertStateToInput(state);
        INDArray target = Nd4j.create(new double[]{0.0, 0.0, 0.0, 0.0, 0.0}); // All actions bad
        
        networkManager.trainNetwork(input, target);
    }
    
    public void onPlayerDamage(Player player, EntityDamageEvent event) {
        if (!isActive) return;
        
        // Negative reward for taking damage
        PlayerState state = new PlayerState(player);
        INDArray input = networkManager.convertStateToInput(state);
        
        // Reduce confidence in current action
        INDArray target = createDamageAvoidanceTarget(event);
        networkManager.trainNetwork(input, target);
    }
    
    public void onPlayerJump(Player player) {
        if (!isActive) return;
        
        // Positive reward for successful jumps (avoiding fall damage)
        if (player.getFallDistance() == 0) {
            PlayerState state = new PlayerState(player);
            INDArray input = networkManager.convertStateToInput(state);
            INDArray target = Nd4j.create(new double[]{0.0, 0.0, 1.0, 0.0, 0.0}); // Jump action
            
            networkManager.trainNetwork(input, target);
        }
    }
    
    public void onPlayerInteract(Player player, PlayerInteractEvent event) {
        if (!isActive) return;
        
        // Train based on interaction success
        PlayerState state = new PlayerState(player);
        INDArray input = networkManager.convertStateToInput(state);
        INDArray target = createInteractionTarget(event);
        
        networkManager.trainNetwork(input, target);
    }
    
    public void onPlayerSneak(Player player, boolean sneaking) {
        if (!isActive) return;
        
        // Train sneaking behavior
        PlayerState state = new PlayerState(player);
        INDArray input = networkManager.convertStateToInput(state);
        INDArray target = sneaking ? 
            Nd4j.create(new double[]{0.0, 0.0, 0.0, 1.0, 0.0}) : // Sneak
            Nd4j.create(new double[]{0.0, 0.0, 0.0, 0.0, 0.0});   // No action
        
        networkManager.trainNetwork(input, target);
    }
    
    public void onPlayerSprint(Player player, boolean sprinting) {
        if (!isActive) return;
        
        // Train sprinting behavior
        PlayerState state = new PlayerState(player);
        INDArray input = networkManager.convertStateToInput(state);
        INDArray target = sprinting ? 
            Nd4j.create(new double[]{0.0, 0.0, 0.0, 0.0, 1.0}) : // Sprint
            Nd4j.create(new double[]{0.0, 0.0, 0.0, 0.0, 0.0});   // No action
        
        networkManager.trainNetwork(input, target);
    }
    
    public void onPlayerItemChange(Player player, PlayerItemHeldEvent event) {
        if (!isActive) return;
        
        // Train item selection behavior
        PlayerState state = new PlayerState(player);
        INDArray input = networkManager.convertStateToInput(state);
        
        // Positive reward for selecting appropriate items
        INDArray target = createItemSelectionTarget(player, event);
        networkManager.trainNetwork(input, target);
    }
    
    public void onPlayerRespawn(Player player, PlayerRespawnEvent event) {
        if (!isActive) return;
        
        // Reset AI state after respawn
        AIPlayerController controller = playerControllers.get(player.getUniqueId());
        if (controller != null) {
            controller.reset();
        }
    }
    
    // Helper methods for creating training targets
    private INDArray createMovementTarget(Location from, Location to) {
        double deltaX = to.getX() - from.getX();
        double deltaZ = to.getZ() - from.getZ();
        
        // Normalize movement direction
        double magnitude = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (magnitude > 0) {
            deltaX /= magnitude;
            deltaZ /= magnitude;
        }
        
        // Map to action space: [forward, backward, left, right, jump]
        return Nd4j.create(new double[]{
            Math.max(0, deltaZ),  // Forward (positive Z)
            Math.max(0, -deltaZ), // Backward (negative Z)
            Math.max(0, -deltaX), // Left (negative X)
            Math.max(0, deltaX),  // Right (positive X)
            0.0                   // Jump (handled separately)
        });
    }
    
    private INDArray createDamageAvoidanceTarget(EntityDamageEvent event) {
        // Create target that encourages damage avoidance
        switch (event.getCause()) {
            case FALL:
                return Nd4j.create(new double[]{0.0, 0.0, 1.0, 0.0, 0.0}); // Jump to avoid fall damage
            case LAVA:
            case FIRE:
                return Nd4j.create(new double[]{0.0, 1.0, 0.0, 0.0, 0.0}); // Move backward from danger
            case DROWNING:
                return Nd4j.create(new double[]{0.0, 0.0, 1.0, 0.0, 0.0}); // Jump to surface
            default:
                return Nd4j.create(new double[]{0.0, 1.0, 0.0, 0.0, 0.0}); // General avoidance
        }
    }
    
    private INDArray createInteractionTarget(PlayerInteractEvent event) {
        // Positive reward for successful interactions
        if (event.getItem() != null) {
            switch (event.getItem().getType()) {
                case WATER_BUCKET:
                    // MLG water bucket usage
                    return Nd4j.create(new double[]{0.0, 0.0, 0.0, 0.0, 0.0}); // Use item action
                case BREAD:
                case APPLE:
                case COOKED_BEEF:
                case GOLDEN_APPLE :        
            
                    // Eating when low health
                    return Nd4j.create(new double[]{0.0, 0.0, 0.0, 0.0, 0.0}); // Use item action
                default:
                    return Nd4j.create(new double[]{0.0, 0.0, 0.0, 0.0, 0.0});
            }
        }
        return Nd4j.create(new double[]{0.0, 0.0, 0.0, 0.0, 0.0});
    }
    
    private INDArray createItemSelectionTarget(Player player, PlayerItemHeldEvent event) {
        // Reward appropriate item selection based on context
        if (player.getHealth() < 10 && hasFood(player)) {
            return Nd4j.create(new double[]{0.0, 0.0, 0.0, 0.0, 0.0}); // Select food
        } else if (player.getFallDistance() > 10 && hasWaterBucket(player)) {
            return Nd4j.create(new double[]{0.0, 0.0, 0.0, 0.0, 0.0}); // Select water bucket
        }
        return Nd4j.create(new double[]{0.0, 0.0, 0.0, 0.0, 0.0});
    }
    
    private boolean hasFood(Player player) {
        return player.getInventory().contains(org.bukkit.Material.BREAD) ||
               player.getInventory().contains(org.bukkit.Material.COOKED_BEEF) ||
               player.getInventory().contains(org.bukkit.Material.APPLE);
    }
    
    private boolean hasWaterBucket(Player player) {
        return player.getInventory().contains(org.bukkit.Material.WATER_BUCKET);
    }
    
    public void shutdown() {
        stopAI();
        networkManager.saveNetwork();
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public Map<UUID, AIPlayerController> getPlayerControllers() {
        return new HashMap<>(playerControllers);
    }
}