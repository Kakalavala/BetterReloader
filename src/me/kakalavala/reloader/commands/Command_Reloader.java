package me.kakalavala.reloader.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import me.kakalavala.reloader.core.Core;
import me.kakalavala.reloader.core.Utils;

public class Command_Reloader implements CommandExecutor {
	
	private Core core;
	private Utils util;
	
	public Command_Reloader(final Core core) {
		this.core = core;
		this.util = new Utils(core);
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String command, String[] args) {
		if (args.length < 1) {
			this.help(sender);
			return false;
		} else {
			final String action = args[0].toLowerCase();
			String plName = core.getName();
			
			try {
				plName = args[1];
			} catch (Exception exc) {}
			
			switch (action) {
				case "reload":
					if (this.doPermCheck(sender, action)) {
						util.reload(plName, sender);
						return true;
					} else return false;
				case "enable":
					if (this.doPermCheck(sender, action)) {
						util.enable(plName, sender);
						return true;
					} else return false;
				case "disable":
					if (this.doPermCheck(sender, action)) {
						util.disable(plName, sender);
						return true;
					} else return false;
				case "load":
					if (this.doPermCheck(sender, action)) {
						util.load(plName, sender, false);
						return true;
					} else return false;
				case "unload":
					if (this.doPermCheck(sender, action)) {
						util.unload(plName, sender, false);
						return true;
					} else return false;
				case "info":
					if (this.doPermCheck(sender, action)) {
						util.info(plName, sender);
						return true;
					} else return false;
				case "status":
					if (this.doPermCheck(sender, action)) {
						util.status(plName, sender);
						return true;
					} else return false;
				case "config":
					if (this.doPermCheck(sender, action)) {
						util.reloadConfig(plName, sender);
						return true;
					} else return false;
				case "list":
					if (this.doPermCheck(sender, action)) {
						util.list(sender, (plName.equalsIgnoreCase("-v") || plName.equalsIgnoreCase("-version")));
						return true;
					} else return false;
				default:
					this.help(sender);
					return false;
			}
		}
	}
	
	private void help(final CommandSender sender) {
		final String[] msg = {
			"§6-------- §4§l[§6Reloader Help§4§l] §6--------",
			this.getPermColour(sender, "reloader.reload") + "/reloader reload <plugin> §6- §7Reloads <plugin>",
			this.getPermColour(sender, "reloader.enable") + "/reloader enable <plugin> §6- §7Enables <plugin>",
			this.getPermColour(sender, "reloader.disable") + "/reloader disable <plugin> §6- §7Disables <plugin>",
			this.getPermColour(sender, "reloader.load") + "/reloader load <plugin> §6- §7Loads <plugin>",
			this.getPermColour(sender, "reloader.unload") + "/reloader unload <plugin> §6- §7Unloads <plugin>",
			this.getPermColour(sender, "reloader.info") + "/reloader info <plugin> §6- §7Gets info about <plugin>",
			this.getPermColour(sender, "reloader.status") + "/reloader status <plugin> §6- §7Checks the status of <plugin>",
			this.getPermColour(sender, "reloader.config") + "/reloader config <plugin> §6- §7Reloads the config of <plugin>",
			this.getPermColour(sender, "reloader.list") + "/reloader list [-v / -version] §6- §7Lists plugins",
			"§7§o- Tip: Leave <plugin> blank for BetterReloader -"
		};
		
		sender.sendMessage(msg);
	}
	
	private boolean doPermCheck(final CommandSender sender, final String perm) {
		if (!sender.hasPermission("reloader." + perm))
			core.msg(sender, "§cYou lack the required permissions!");
		
		return sender.hasPermission("reloader." + perm);
	}
	
	private String getPermColour(final CommandSender sender, final String perm) {
		return (sender.hasPermission(perm)) ? "§a" : "§c";
	}
}
