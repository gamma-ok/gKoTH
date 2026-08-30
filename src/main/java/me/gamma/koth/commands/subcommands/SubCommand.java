package me.gamma.koth.commands.subcommands;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface SubCommand {

    String getName();
    String getDescription();
    String getUsage();
    String getPermission();
    boolean hasPermission(CommandSender sender);
    void execute(CommandSender sender, String[] args);
    
    default List<String> tabComplete(CommandSender sender, String[] args) {
        return java.util.Collections.emptyList();
    }
}