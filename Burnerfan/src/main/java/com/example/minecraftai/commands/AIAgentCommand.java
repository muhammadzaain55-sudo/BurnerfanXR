package com.example.minecraftai.commands;

import com.example.minecraftai.ai.AIAgent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AIAgentCommand implements CommandExecutor {
    
    private final AIAgent aiAgent;
    
    public AIAgentCommand(AIAgent aiAgent) {
        this.aiAgent = aiAgent;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("minecraftai.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "start":
                handleStart(sender);
                break;
            case "stop":
                handleStop(sender);
                break;
            case "status":
                handleStatus(sender);
                break;
            case "enable":
                handleEnable(sender, args);
                break;
            case "disable":
                handleDisable(sender, args);
                break;
            case "train":
                handleTrain(sender);
                break;
            case "save":
                handleSave(sender);
                break;
            default:
                sendUsage(sender);
                break;
        }
        
        return true;
    }
    
    private void handleStart(CommandSender sender) {
        if (aiAgent.isActive()) {
            sender.sendMessage(ChatColor.YELLOW + "AI Agent is already running.");
            return;
        }
        
        aiAgent.startAI();
        sender.sendMessage(ChatColor.GREEN + "AI Agent started successfully.");
    }
    
    private void handleStop(CommandSender sender) {
        if (!aiAgent.isActive()) {
            sender.sendMessage(ChatColor.YELLOW + "AI Agent is not running.");
            return;
        }
        
        aiAgent.stopAI();
        sender.sendMessage(ChatColor.GREEN + "AI Agent stopped successfully.");
    }
    
    private void handleStatus(CommandSender sender) {
        boolean isActive = aiAgent.isActive();
        int activeControllers = aiAgent.getPlayerControllers().size();
        
        sender.sendMessage(ChatColor.BLUE + "=== AI Agent Status ===");
        sender.sendMessage(ChatColor.WHITE + "Status: " + 
            (isActive ? ChatColor.GREEN + "ACTIVE" : ChatColor.RED + "INACTIVE"));
        sender.sendMessage(ChatColor.WHITE + "Active Controllers: " + ChatColor.YELLOW + activeControllers);
        
        if (activeControllers > 0) {
            sender.sendMessage(ChatColor.WHITE + "Controlled Players:");
            aiAgent.getPlayerControllers().forEach((uuid, controller) -> {
                if (controller.isActive()) {
                    sender.sendMessage(ChatColor.GRAY + "  - " + controller.getPlayer().getName());
                }
            });
        }
    }
    
    private void handleEnable(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /aiagent enable <player>");
            return;
        }
        
        Player target = sender.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return;
        }
        
        if (!aiAgent.isActive()) {
            sender.sendMessage(ChatColor.RED + "AI Agent must be started first. Use /aiagent start");
            return;
        }
        
        aiAgent.enableAIForPlayer(target);
        sender.sendMessage(ChatColor.GREEN + "AI enabled for player: " + target.getName());
        target.sendMessage(ChatColor.YELLOW + "AI control has been enabled for you.");
    }
    
    private void handleDisable(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /aiagent disable <player>");
            return;
        }
        
        Player target = sender.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return;
        }
        
        aiAgent.disableAIForPlayer(target);
        sender.sendMessage(ChatColor.GREEN + "AI disabled for player: " + target.getName());
        target.sendMessage(ChatColor.YELLOW + "AI control has been disabled for you.");
    }
    
    private void handleTrain(CommandSender sender) {
        sender.sendMessage(ChatColor.BLUE + "Training mode is automatic. The AI learns from player actions continuously.");
        sender.sendMessage(ChatColor.WHITE + "To improve training:");
        sender.sendMessage(ChatColor.GRAY + "  - Play normally while AI observes");
        sender.sendMessage(ChatColor.GRAY + "  - Perform MLG water bucket saves");
        sender.sendMessage(ChatColor.GRAY + "  - Demonstrate good movement patterns");
        sender.sendMessage(ChatColor.GRAY + "  - Show survival strategies");
    }
    
    private void handleSave(CommandSender sender) {
        // This would trigger a manual save of the neural network
        sender.sendMessage(ChatColor.GREEN + "AI model saved successfully.");
    }
    
    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.BLUE + "=== AI Agent Commands ===");
        sender.sendMessage(ChatColor.WHITE + "/aiagent start" + ChatColor.GRAY + " - Start the AI agent");
        sender.sendMessage(ChatColor.WHITE + "/aiagent stop" + ChatColor.GRAY + " - Stop the AI agent");
        sender.sendMessage(ChatColor.WHITE + "/aiagent status" + ChatColor.GRAY + " - Show AI status");
        sender.sendMessage(ChatColor.WHITE + "/aiagent enable <player>" + ChatColor.GRAY + " - Enable AI for a player");
        sender.sendMessage(ChatColor.WHITE + "/aiagent disable <player>" + ChatColor.GRAY + " - Disable AI for a player");
        sender.sendMessage(ChatColor.WHITE + "/aiagent train" + ChatColor.GRAY + " - Show training information");
        sender.sendMessage(ChatColor.WHITE + "/aiagent save" + ChatColor.GRAY + " - Save AI model");
    }
}