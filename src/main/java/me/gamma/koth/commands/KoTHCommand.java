package me.gamma.koth.commands;

import me.gamma.koth.KoTHPlugin;
import me.gamma.koth.commands.subcommands.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class KoTHCommand implements CommandExecutor, TabCompleter {

    private final KoTHPlugin plugin;
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public KoTHCommand(KoTHPlugin plugin) {
        this.plugin = plugin;
        registerSubCommands();
    }

    private void registerSubCommands() {
        subCommands.put("create", new CreateCommand(plugin));
        subCommands.put("remove", new RemoveCommand(plugin));
        subCommands.put("list", new ListCommand(plugin));
        subCommands.put("enable", new EnableCommand(plugin));
        subCommands.put("reload", new ReloadCommand(plugin));
        subCommands.put("wand", new WandCommand(plugin));
        subCommands.put("rewards", new RewardsCommand(plugin));
        subCommands.put("loot", new LootCommand(plugin));
        subCommands.put("cmd", new CmdCommand(plugin));
        subCommands.put("removecmd", new RemoveCmdCommand(plugin));
        subCommands.put("start", new StartCommand(plugin));
        subCommands.put("stop", new StopCommand(plugin));
        subCommands.put("tp", new TpCommand(plugin));
        subCommands.put("info", new InfoCommand(plugin));
        subCommands.put("claim", new ClaimCommand(plugin));
        subCommands.put("help", new HelpCommand(plugin));
        subCommands.put("schedule", new ScheduleCommand(plugin));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasAnyKoTHPermission(sender)) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("NO_PERMISSION"));
            return true;
        }

        if (args.length == 0) {
            SubCommand helpCommand = subCommands.get("help");
            if (helpCommand.hasPermission(sender)) {
                helpCommand.execute(sender, new String[0]);
            } else {
                sender.sendMessage(plugin.getMessagesManager().getMessage("NO_PERMISSION"));
            }
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("COMMAND_NOT_FOUND",
                    "%command%", "/koth " + args[0]));
            return true;
        }

        if (!subCommand.hasPermission(sender)) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("NO_PERMISSION"));
            return true;
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        subCommand.execute(sender, subArgs);
        return true;
    }

    private boolean hasAnyKoTHPermission(CommandSender sender) {
        String[] permissions = {
            "koth.admin",
            "koth.player",
            "koth.create",
            "koth.remove",
            "koth.list",
            "koth.enable",
            "koth.reload",
            "koth.wand",
            "koth.rewards",
            "koth.loot",
            "koth.cmd",
            "koth.start",
            "koth.stop",
            "koth.tp",
            "koth.info",
            "koth.claim",
            "koth.schedule",
            "koth.schedule.admin"
        };
        
        for (String perm : permissions) {
            if (sender.hasPermission(perm)) {
                return true;
            }
        }
        
        // Si es consola, siempre permitir
        if (!(sender instanceof Player)) {
            return true;
        }
        
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!hasAnyKoTHPermission(sender)) {
            return Collections.emptyList();
        }
        
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return subCommands.keySet().stream()
                    .filter(cmd -> cmd.startsWith(partial))
                    .filter(cmd -> subCommands.get(cmd).hasPermission(sender))
                    .collect(Collectors.toList());
        }

        if (args.length > 1) {
            String subCommandName = args[0].toLowerCase();
            SubCommand subCommand = subCommands.get(subCommandName);
            if (subCommand != null && subCommand.hasPermission(sender)) {
                return subCommand.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }

        return Collections.emptyList();
    }

    public Map<String, SubCommand> getSubCommands() {
        return subCommands;
    }
}