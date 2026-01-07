package com.example.minecraftai.ai;

import com.example.minecraftai.MinecraftAIPlugin;
import com.example.minecraftai.data.PlayerState;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NeuralNetworkManager {
    
    private final MinecraftAIPlugin plugin;
    private MultiLayerNetwork network;
    private final Path modelPath;
    
    // Network architecture parameters
    private static final int INPUT_SIZE = 50;  // Number of input features
    private static final int HIDDEN_SIZE_1 = 128;
    private static final int HIDDEN_SIZE_2 = 64;
    private static final int OUTPUT_SIZE = 5;  // Number of possible actions
    
    // Training parameters
    private static final double LEARNING_RATE = 0.001;
    private static final int BATCH_SIZE = 32;
    
    public NeuralNetworkManager(MinecraftAIPlugin plugin) {
        this.plugin = plugin;
        this.modelPath = Paths.get(plugin.getDataFolder().getAbsolutePath(), "ai_model.zip");
    }
    
    public void initializeNetwork() {
        // Try to load existing model first
        if (loadNetwork()) {
            plugin.getLogger().info("Loaded existing AI model");
            return;
        }
        
        // Create new network if no existing model
        MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
            .seed(123)
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .updater(new Adam(LEARNING_RATE))
            .weightInit(WeightInit.XAVIER)
            .list()
            .layer(0, new DenseLayer.Builder()
                .nIn(INPUT_SIZE)
                .nOut(HIDDEN_SIZE_1)
                .activation(Activation.RELU)
                .build())
            .layer(1, new DenseLayer.Builder()
                .nIn(HIDDEN_SIZE_1)
                .nOut(HIDDEN_SIZE_2)
                .activation(Activation.RELU)
                .build())
            .layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                .nIn(HIDDEN_SIZE_2)
                .nOut(OUTPUT_SIZE)
                .activation(Activation.SOFTMAX)
                .build())
            .build();
        
        network = new MultiLayerNetwork(config);
        network.init();
        
        plugin.getLogger().info("Created new AI neural network");
    }
    
    public INDArray convertStateToInput(PlayerState state) {
        // Convert player state to neural network input
        double[] input = new double[INPUT_SIZE];
        int index = 0;
        
        // Health and status (10 features)
        input[index++] = state.getHealth() / state.getMaxHealth(); // Normalized health
        input[index++] = state.getFoodLevel() / 20.0; // Normalized food level
        input[index++] = state.getSaturation() / 20.0; // Normalized saturation
        input[index++] = state.getAirLevel() / 300.0; // Normalized air level
        input[index++] = state.isOnGround() ? 1.0 : 0.0;
        input[index++] = state.isInWater() ? 1.0 : 0.0;
        input[index++] = state.isInLava() ? 1.0 : 0.0;
        input[index++] = state.isSneaking() ? 1.0 : 0.0;
        input[index++] = state.isSprinting() ? 1.0 : 0.0;
        input[index++] = state.isFlying() ? 1.0 : 0.0;
        
        // Position and movement (8 features)
        input[index++] = Math.tanh(state.getX() / 1000.0); // Normalized X position
        input[index++] = state.getY() / 256.0; // Normalized Y position (world height)
        input[index++] = Math.tanh(state.getZ() / 1000.0); // Normalized Z position
        input[index++] = state.getYaw() / 360.0; // Normalized yaw
        input[index++] = state.getPitch() / 180.0; // Normalized pitch
        input[index++] = Math.tanh(state.getVelocityX() * 10); // Normalized velocity
        input[index++] = Math.tanh(state.getVelocityY() * 10);
        input[index++] = Math.tanh(state.getVelocityZ() * 10);
        
        // Inventory (10 features - simplified)
        input[index++] = hasItem(state, "SWORD") ? 1.0 : 0.0;
        input[index++] = hasItem(state, "PICKAXE") ? 1.0 : 0.0;
        input[index++] = hasItem(state, "AXE") ? 1.0 : 0.0;
        input[index++] = hasItem(state, "SHOVEL") ? 1.0 : 0.0;
        input[index++] = hasItem(state, "BOW") ? 1.0 : 0.0;
        input[index++] = hasItem(state, "FOOD") ? 1.0 : 0.0;
        input[index++] = hasItem(state, "WATER_BUCKET") ? 1.0 : 0.0;
        input[index++] = hasItem(state, "BLOCKS") ? 1.0 : 0.0;
        input[index++] = hasArmor(state) ? 1.0 : 0.0;
        input[index++] = getInventoryFullness(state);
        
        // Experience (3 features)
        input[index++] = Math.min(state.getLevel() / 50.0, 1.0); // Normalized level
        input[index++] = state.getExp(); // Experience progress
        input[index++] = Math.min(state.getTotalExperience() / 1000.0, 1.0); // Normalized total exp
        
        // Environmental (9 features)
        input[index++] = getBlockTypeValue(state.getBlockBelow());
        input[index++] = state.getLightLevel() / 15.0; // Normalized light level
        input[index++] = state.isRaining() ? 1.0 : 0.0;
        input[index++] = state.isThundering() ? 1.0 : 0.0;
        input[index++] = (state.getWorldTime() % 24000) / 24000.0; // Normalized time of day
        input[index++] = state.getGameMode().equals("SURVIVAL") ? 1.0 : 0.0;
        input[index++] = state.getGameMode().equals("CREATIVE") ? 1.0 : 0.0;
        input[index++] = state.getGameMode().equals("ADVENTURE") ? 1.0 : 0.0;
        input[index++] = state.getPotionEffects().size() / 10.0; // Normalized effect count
        
        // Pad remaining features with zeros if needed
        while (index < INPUT_SIZE) {
            input[index++] = 0.0;
        }
        
        return Nd4j.create(input).reshape(1, INPUT_SIZE);
    }
    
    public INDArray predict(PlayerState state) {
        INDArray input = convertStateToInput(state);
        return network.output(input);
    }
    
    public void trainNetwork(INDArray input, INDArray target) {
        DataSet dataSet = new DataSet(input, target.reshape(1, OUTPUT_SIZE));
        network.fit(dataSet);
    }
    
    public void saveNetwork() {
        try {
            // Create directory if it doesn't exist
            modelPath.getParent().toFile().mkdirs();
            
            ModelSerializer.writeModel(network, modelPath.toFile(), true);
            plugin.getLogger().info("AI model saved successfully");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save AI model: " + e.getMessage());
        }
    }
    
    public boolean loadNetwork() {
        try {
            File modelFile = modelPath.toFile();
            if (modelFile.exists()) {
                network = ModelSerializer.restoreMultiLayerNetwork(modelFile);
                return true;
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load AI model: " + e.getMessage());
        }
        return false;
    }
    
    // Helper methods for feature extraction
    private boolean hasItem(PlayerState state, String itemType) {
        return state.getInventory().values().stream()
            .anyMatch(item -> item != null && item.getType().name().contains(itemType));
    }
    
    private boolean hasArmor(PlayerState state) {
        return state.getHelmet() != null || state.getChestplate() != null ||
               state.getLeggings() != null || state.getBoots() != null;
    }
    
    private double getInventoryFullness(PlayerState state) {
        long filledSlots = state.getInventory().values().stream()
            .mapToLong(item -> item != null ? 1 : 0)
            .sum();
        return filledSlots / 36.0; // 36 inventory slots
    }
    
    private double getBlockTypeValue(org.bukkit.Material blockType) {
        // Simplified block type encoding
        switch (blockType) {
            case AIR: return 0.0;
            case STONE: return 0.1;
            case DIRT: return 0.2;
            case GRASS_BLOCK: return 0.3;
            case WATER: return 0.4;
            case LAVA: return 0.5;
            case SAND: return 0.6;
            default: return 0.9;
        }
    }
    
    public MultiLayerNetwork getNetwork() {
        return network;
    }
}