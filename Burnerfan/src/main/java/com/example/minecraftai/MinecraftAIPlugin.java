package com.example.minecraftai;

import com.example.minecraftai.ai.AIAgent;
import com.example.minecraftai.commands.AIAgentCommand;
import com.example.minecraftai.data.PlayerStateCollector;
import com.example.minecraftai.listeners.PlayerEventListener;
import org.bukkit.plugin.java.JavaPlugin;

public class MinecraftAIPlugin extends JavaPlugin {
    
    private PlayerStateCollector stateCollector;
    private AIAgent aiAgent;
    
    @Override
    public void onEnable() {
        getLogger().info("MinecraftAI Plugin is starting...");
        
        // Initialize components
        stateCollector = new PlayerStateCollector(this);
        aiAgent = new AIAgent(this, stateCollector);
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerEventListener(stateCollector, aiAgent), this);
        
        // Register commands
        getCommand("aiagent").setExecutor(new AIAgentCommand(aiAgent));
        
        // Start data collection
        stateCollector.startCollection();
        
        getLogger().info("MinecraftAI Plugin enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("MinecraftAI Plugin is shutting down...");
        
        if (stateCollector != null) {
            stateCollector.stopCollection();
        }
        
        if (aiAgent != null) {
            aiAgent.shutdown();
        }
        
        getLogger().info("MinecraftAI Plugin disabled successfully!");
    }
    
    public PlayerStateCollector getStateCollector() {
        return stateCollector;
    }
    
    public AIAgent getAIAgent() {
        return aiAgent;
    }
}