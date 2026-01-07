# Minecraft AI Plugin

A Spigot plugin that creates an AI agent capable of learning and playing Minecraft using Deep Learning 4J (DL4J). The plugin collects player state data, trains a neural network, and can control players autonomously.

## Features

- **Real-time Player State Collection**: Captures comprehensive player data including health, inventory, position, movement, and environmental conditions
- **Neural Network Training**: Uses DL4J to train an AI model based on player behavior
- **Autonomous Player Control**: AI can control players and make decisions based on learned patterns
- **MLG Detection**: Recognizes and learns from MLG (Major League Gaming) scenarios like water bucket saves
- **Event-based Learning**: Learns from player deaths, damage, successful actions, and interactions
- **Persistent Model**: Saves and loads trained neural network models

## Requirements

- Minecraft Server (Spigot/Paper) 1.20.4+
- Java 17+
- Maven for building

## Installation

1. **Clone and Build**:
   ```bash
   git clone <repository-url>
   cd minecraft-ai-plugin
   mvn clean package
   ```

2. **Install Plugin**:
   - Copy `target/minecraft-ai-plugin-1.0.0.jar` to your server's `plugins/` folder
   - Restart your server

3. **Dependencies**:
   The plugin includes all necessary DL4J dependencies via Maven Shade plugin.

## Usage

### Commands

All commands require the `minecraftai.admin` permission (default: OP).

- `/aiagent start` - Start the AI agent
- `/aiagent stop` - Stop the AI agent  
- `/aiagent status` - Show current AI status
- `/aiagent enable <player>` - Enable AI control for a specific player
- `/aiagent disable <player>` - Disable AI control for a player
- `/aiagent train` - Show training information
- `/aiagent save` - Manually save the AI model

### Getting Started

1. **Start Data Collection**:
   ```
   /aiagent start
   ```

2. **Train the AI**:
   - Play normally on the server
   - The AI automatically learns from all player actions
   - Perform various activities: movement, combat, building, MLG saves

3. **Enable AI Control**:
   ```
   /aiagent enable <playername>
   ```

4. **Monitor Performance**:
   ```
   /aiagent status
   ```

## How It Works

### Data Collection

The plugin continuously collects player state data including:

- **Health & Status**: Health, food, air, effects
- **Movement**: Position, velocity, rotation
- **Inventory**: Items, armor, held items
- **Environment**: Block types, light level, weather
- **Actions**: Movement, jumping, sneaking, interactions

### Neural Network Architecture

- **Input Layer**: 50 features representing player state
- **Hidden Layers**: 128 → 64 neurons with ReLU activation
- **Output Layer**: 5 actions (forward, backward, jump, sneak, sprint)
- **Training**: Uses Adam optimizer with MSE loss

### AI Actions

The AI can perform:

- **Movement**: Forward, backward, strafing
- **Jumping**: Context-aware jumping decisions
- **Sneaking**: Stealth and precision movement
- **Sprinting**: Speed optimization
- **Item Usage**: Food consumption, water bucket MLG saves
- **Block Placement**: Basic building and bridging

### Learning Scenarios

The AI learns from:

- **Successful Actions**: Positive reinforcement for good outcomes
- **Deaths**: Negative reinforcement to avoid fatal mistakes
- **Damage**: Learning damage avoidance patterns
- **MLG Saves**: Water bucket clutch saves from fall damage
- **Item Management**: Appropriate item selection and usage

## Configuration

### Data Storage

Training data is stored in `plugins/MinecraftAIPlugin/training_data/` as JSON files.
The neural network model is saved as `plugins/MinecraftAIPlugin/ai_model.zip`.

### Performance Tuning

Key parameters in the code:

- `COLLECTION_INTERVAL_TICKS = 5` - Data collection frequency
- `AI_UPDATE_INTERVAL_TICKS = 10` - AI decision frequency  
- `LEARNING_RATE = 0.001` - Neural network learning rate
- `ACTION_THRESHOLD = 0.3` - Minimum confidence for actions

## Development

### Project Structure

```
src/main/java/com/example/minecraftai/
├── MinecraftAIPlugin.java          # Main plugin class
├── ai/
│   ├── AIAgent.java                # Core AI management
│   ├── AIPlayerController.java     # Individual player control
│   └── NeuralNetworkManager.java   # DL4J network handling
├── commands/
│   └── AIAgentCommand.java         # Command handling
├── data/
│   ├── PlayerState.java            # Player state data structure
│   └── PlayerStateCollector.java   # Data collection system
└── listeners/
    └── PlayerEventListener.java    # Event handling for training
```

### Extending the AI

To add new behaviors:

1. **Add Features**: Extend `PlayerState` with new data points
2. **Update Network**: Modify `INPUT_SIZE` in `NeuralNetworkManager`
3. **Add Actions**: Extend output actions in `AIPlayerController`
4. **Train Patterns**: Add event handlers in `PlayerEventListener`

### API Integration

The plugin can be extended to expose REST APIs for external training data or remote control:

```java
// Example: External training data endpoint
@RestController
public class AITrainingController {
    @PostMapping("/train")
    public void submitTrainingData(@RequestBody TrainingData data) {
        // Process external training data
    }
}
```

## Troubleshooting

### Common Issues

1. **High Memory Usage**: DL4J can be memory-intensive. Increase server RAM or reduce batch sizes.

2. **Slow Performance**: Reduce collection frequency or AI update intervals.

3. **Model Not Learning**: Ensure sufficient training data and appropriate learning rates.

4. **Plugin Conflicts**: Check for conflicts with other AI or automation plugins.

### Debug Information

Enable debug logging in your server's logging configuration to see detailed AI decision-making processes.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Disclaimer

This plugin is for educational and research purposes. Use responsibly and in accordance with your server's rules and Minecraft's Terms of Service.