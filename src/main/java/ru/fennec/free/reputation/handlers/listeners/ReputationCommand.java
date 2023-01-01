package ru.fennec.free.reputation.handlers.listeners;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import ru.fennec.free.reputation.common.abstracts.AbstractCommand;
import ru.fennec.free.reputation.common.interfaces.IDatabase;
import ru.fennec.free.reputation.common.interfaces.IGamePlayer;
import ru.fennec.free.reputation.handlers.database.configs.MessagesConfig;
import ru.fennec.free.reputation.handlers.messages.MessageManager;
import ru.fennec.free.reputation.handlers.players.PlayersContainer;

import java.util.ArrayList;

public class ReputationCommand extends AbstractCommand {

    private final MessagesConfig messagesConfig;
    private final IDatabase database;
    private final PlayersContainer playersContainer;
    private final MessageManager messageManager;

    public ReputationCommand(Plugin plugin, MessagesConfig messagesConfig, IDatabase database,
                             PlayersContainer playersContainer, MessageManager messageManager) {
        super(plugin, "reputation");
        this.messagesConfig = messagesConfig;
        this.database = database;
        this.playersContainer = playersContainer;
        this.messageManager = messageManager;
    }

    @Override
    public void execute(CommandSender commandSender, String label, String[] args) {
        switch (args.length) {
            case 0:
                sendHelp(commandSender);
                break;
            case 1:
                switch (args[0].toLowerCase()) {
                    case "help" -> sendHelp(commandSender);
                    case "info", "me", "self" -> sendSelfInfo(commandSender);
                    case "reload" -> reloadPlugin(commandSender);
                    default -> sendPlayerInfo(commandSender, args[0]);
                }
            case 2:
                if (args[0].equalsIgnoreCase("give")) {
                    giveReputation(commandSender, args[1]);
                } else {
                    sendHelp(commandSender);
                }
            case 3:
                if (args[0].equalsIgnoreCase("player") && args[2].equalsIgnoreCase("reset")) {
                    resetPlayerReputation(commandSender, args[1]);
                } else {
                    sendHelp(commandSender);
                }
        }
    }

    private void sendHelp(CommandSender commandSender) {
        messagesConfig.playerSection().helpStrings().forEach(str -> commandSender.sendMessage(messageManager.parsePluginPlaceholders(str)));
        if (commandSender.hasPermission("reputation.admin.help")) {
            messagesConfig.adminSection().helpStrings().forEach(str -> commandSender.sendMessage(messageManager.parsePluginPlaceholders(str)));
        }
    }

    private void sendSelfInfo(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(messageManager.parsePluginPlaceholders(messagesConfig.playerSection().notAPlayer()));
        } else {
            IGamePlayer gamePlayer = playersContainer.getCachedPlayerByUUID(((Player) commandSender).getUniqueId());
            commandSender.sendMessage(messageManager.parsePlaceholders(gamePlayer, messagesConfig.playerSection().selfInfo()));
        }
    }

    private void sendPlayerInfo(CommandSender commandSender, String targetName) {
        Player targetPlayer = Bukkit.getPlayer(targetName);
        if (targetPlayer == null) {
            commandSender.sendMessage(messageManager.parsePluginPlaceholders(messagesConfig.playerSection().playerIsOffline()));
        } else {
            IGamePlayer targetGamePlayer = playersContainer.getCachedPlayerByUUID(targetPlayer.getUniqueId());
            if (targetGamePlayer == null) {
                commandSender.sendMessage(messageManager.parsePluginPlaceholders(messagesConfig.playerSection().playerNotInCache()));
            } else {
                commandSender.sendMessage(messageManager.parsePlaceholders(targetGamePlayer, messagesConfig.playerSection().playerInfo()));
            }
        }
    }

    private void giveReputation(CommandSender commandSender, String targetName) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(messageManager.parsePluginPlaceholders(messagesConfig.playerSection().notAPlayer()));
        } else {
            IGamePlayer gamePlayer = playersContainer.getCachedPlayerByUUID(((Player) commandSender).getUniqueId());
            Player targetPlayer = Bukkit.getPlayer(targetName);
            if (targetPlayer == null) {
                commandSender.sendMessage(messageManager.parsePluginPlaceholders(messagesConfig.playerSection().playerIsOffline()));
            } else {
                IGamePlayer targetGamePlayer = playersContainer.getCachedPlayerByUUID(targetPlayer.getUniqueId());
                if (targetGamePlayer == null) {
                    commandSender.sendMessage(messageManager.parsePluginPlaceholders(messagesConfig.playerSection().playerNotInCache()));
                } else {
                    if (gamePlayer.getIDsWhomGaveReputation().contains(targetGamePlayer.getId())) {
                        commandSender.sendMessage(messageManager.parsePlaceholders(targetGamePlayer, messagesConfig.playerSection().alreadyGaveReputation()));
                    } else {
                        targetGamePlayer.setPlayerReputation(targetGamePlayer.getPlayerReputation()+1);
                        gamePlayer.getIDsWhomGaveReputation().add(targetGamePlayer.getId());
                        commandSender.sendMessage(messageManager.parsePlaceholders(targetGamePlayer, messagesConfig.playerSection().gaveReputation()));
                    }
                }
            }
        }
    }

    private void resetPlayerReputation(CommandSender commandSender, String targetName) {
        if (commandSender.hasPermission("reputation.admin.reset")) {
            Player targetPlayer = Bukkit.getPlayer(targetName);
            if (targetPlayer == null) {
                commandSender.sendMessage(messageManager.parsePluginPlaceholders(messagesConfig.playerSection().playerIsOffline()));
            } else {
                IGamePlayer targetGamePlayer = playersContainer.getCachedPlayerByUUID(targetPlayer.getUniqueId());
                if (targetGamePlayer == null) {
                    commandSender.sendMessage(messageManager.parsePluginPlaceholders(messagesConfig.playerSection().playerNotInCache()));
                } else {
                    targetGamePlayer.setPlayerReputation(0);
                    targetGamePlayer.setIDsWhomGaveReputation(new ArrayList<>());
                    playersContainer.getAllCachedPlayers().forEach(cachedPlayer -> cachedPlayer.getIDsWhomGaveReputation().remove(targetGamePlayer.getId()));
                    commandSender.sendMessage(messageManager.parsePlaceholders(targetGamePlayer, messagesConfig.adminSection().playerReset()));
                }
            }
        } else {
            commandSender.sendMessage(messageManager.parsePluginPlaceholders(messagesConfig.adminSection().noPermission()));
        }
    }

    private void reloadPlugin(CommandSender commandSender) {
        //ToDo
    }
}
