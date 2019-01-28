package me.kakalavala.reloader.core;

import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import me.kakalavala.reloader.commands.Command_Reloader;

public class Core extends JavaPlugin {
	
	public final PluginDescriptionFile pf = this.getDescription();
	public final Logger log = Logger.getLogger(pf.getName());
	public final PluginManager pm = Bukkit.getPluginManager();
	
	public final Utils utils = new Utils(this);
	
	public final String PATH = this.getDataFolder().getParent();
	
	public void onEnable() {
		this.getConfig().options().copyDefaults(true);
		
		this.registerCommands();
		this.registerListeners();
	}
	
	public void onDisable() {
		
	}
	
	private void registerCommands() {
		this.getCommand("reloader").setExecutor(new Command_Reloader(this));
	}
	
	private void registerListeners() {
		pm.registerEvents(new Listener() {
			@EventHandler(priority = EventPriority.HIGHEST)
			public void onCommandEvent(final PlayerCommandPreprocessEvent e) {
				if (e.isCancelled())
					return;
				
				final CommandSender sender = e.getPlayer();
				
				String cmd = e.getMessage().substring(1).toLowerCase();
				
				if (cmd.indexOf(":") != -1)
					cmd = cmd.substring(cmd.indexOf(":") + 1);
				
				if (cmd.equalsIgnoreCase("pl") || cmd.equalsIgnoreCase("plugins")) {
					if (overrideList() && sender.hasPermission("reloader.list")) {
						utils.list(sender, false);
						e.setCancelled(true);
					}
				} else if (cmd.equalsIgnoreCase("pl -v") || cmd.equalsIgnoreCase("plugins -v") || cmd.equalsIgnoreCase("pl -version") || cmd.equalsIgnoreCase("plugins -version")) {
					if (overrideList() && sender.hasPermission("reloader.list")) {
						utils.list(sender, true);
						e.setCancelled(true);
					}
				} else return;
			}
		}, this);
	}

	public void msg(final CommandSender sender, final String str) {
		sender.sendMessage(this.getPrefix() + str);
	}
	
	public String getPrefix() {
		this.reloadConfig();
		
		String prefix = ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("prefix"));
		
		return (prefix.trim().length() > 0) ? prefix + "§r " : "§r";
	}
	
	public boolean overrideList() {
		this.reloadConfig();
		return this.getConfig().getBoolean("override-pl");
	}
	
	public List<String> getExemptPlugins() {
		this.reloadConfig();
		return this.getConfig().getStringList("exempt-plugins");
	}
	
	public boolean isExempt(final String plName) {
		boolean exempt = false;
		
		for (final String pl : this.getExemptPlugins()) {
			if (pl.equalsIgnoreCase(plName)) {
				exempt = true;
				break;
			}
		}
		
		return exempt;
	}
}
