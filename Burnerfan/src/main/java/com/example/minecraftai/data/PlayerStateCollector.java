package com.example.minecraftai.data;

import com.example.minecraftai.MinecraftAIPlugin;
import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PlayerStateCollector {
    
    private final MinecraftAIPlugin plugin;
    private final ConcurrentLinkedQueue<PlayerState> stateQueue;
    private final Gson gson;
    private BukkitRunnable collectionTask;
    private BukkitRunnable saveTask;
    private final Path dataDirectory;
    
    // Collection settings
    private static final int COLLECTION_INTERVAL_TICKS = 5; // Collect every 5 ticks (4 times per second)
    private static final int SAVE_INTERVAL_TICKS = 200; // Save every 200 ticks (10 seconds)
    private static final int MAX_QUEUE_SIZE = 10000;
    
    public PlayerStateCollector(MinecraftAIPlugin plugin) {
        this.plugin = plugin;
        this.stateQueue = new ConcurrentLinkedQueue<>();
        this.gson = new Gson();
        this.dataDirectory = Paths.get(plugin.getDataFolder().getAbsolutePath(), "training_data");
        
        // Create data directory if it doesn't exist
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create data directory: " + e.getMessage());
        }
    }
    
    public void startCollection() {
        if (collectionTask != null && !collectionTask.isCancelled()) {
            return;
        }
        
        // Start state collection task
        collectionTask = new BukkitRunnable() {
            @Override
            public void run() {
                collectPlayerStates();
            }
        };
        collectionTask.runTaskTimer(plugin, 0L, COLLECTION_INTERVAL_TICKS);
        
        // Start save task
        saveTask = new BukkitRunnable() {
            @Override
            public void run() {
                saveCollectedData();
            }
        };
        saveTask.runTaskTimerAsynchronously(plugin, SAVE_INTERVAL_TICKS, SAVE_INTERVAL_TICKS);
        
        plugin.getLogger().info("Player state collection started");
    }
    
    public void stopCollection() {
        if (collectionTask != null) {
            collectionTask.cancel();
            collectionTask = null;
        }
        
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }
        
        // Save any remaining data
        saveCollectedData();
        
        plugin.getLogger().info("Player state collection stopped");
    }
    
    private void collectPlayerStates() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (stateQueue.size() < MAX_QUEUE_SIZE) {
                PlayerState state = new PlayerState(player);
                stateQueue.offer(state);
            }
        }
    }
    
    public void recordEvent(Player player, String eventType, String eventData) {
        if (stateQueue.size() < MAX_QUEUE_SIZE) {
            PlayerState state = new PlayerState(player);
            PlayerEvent event = new PlayerEvent(state, eventType, eventData);
            // Store event with enhanced context
            stateQueue.offer(state);
        }
    }
    
    private void saveCollectedData() {
        if (stateQueue.isEmpty()) {
            return;
        }
        
        List<PlayerState> statesToSave = new ArrayList<>();
        PlayerState state;
        while ((state = stateQueue.poll()) != null) {
            statesToSave.add(state);
        }
        
        if (statesToSave.isEmpty()) {
            return;
        }
        
        // Save to JSON file with timestamp
        String filename = "player_states_" + System.currentTimeMillis() + ".json";
        Path filePath = dataDirectory.resolve(filename);
        
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            gson.toJson(statesToSave, writer);
            plugin.getLogger().info("Saved " + statesToSave.size() + " player states to " + filename);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player states: " + e.getMessage());
        }
    }
    
    public List<PlayerState> getRecentStates(int count) {
        return stateQueue.stream().limit(count).toList();
    }
    
    public int getQueueSize() {
        return stateQueue.size();
    }
    
    // Inner class for events
    public static class PlayerEvent {
        private final PlayerState state;
        private final String eventType;
        private final String eventData;
        private final long timestamp;
        
        public PlayerEvent(PlayerState state, String eventType, String eventData) {
            this.state = state;
            this.eventType = eventType;
            this.eventData = eventData;
            this.timestamp = System.currentTimeMillis();
        }
        
        // Getters
        public PlayerState getState() { return state; }
        public String getEventType() { return eventType; }
        public String getEventData() { return eventData; }
        public long getTimestamp() { return timestamp; }
    }
}