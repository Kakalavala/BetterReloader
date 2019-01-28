package me.kakalavala.reloader.core;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.Event;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.UnknownDependencyException;
import org.bukkit.plugin.java.JavaPlugin;

public class Utils {
	
	private Core core;
	
	public Utils(final Core core) {
		this.core = core;
	}
	
	public void load(final String plName, final CommandSender sender, final boolean isReloading) {
		for (final Plugin pl : core.pm.getPlugins()) {
			if (pl.getName().toLowerCase().startsWith(plName.toLowerCase())) {
				core.msg(sender, String.format("§c%s is already loaded.", plName));
				return;
			}
		}
		
		final List<File> files = new ArrayList<File>();
		final File[] list = new File(core.PATH).listFiles();
		
		String name = "";
		
		for (final File comp : list) {
			if (comp.isFile() && comp.getName().toLowerCase().endsWith(".jar")) {
				try {
					name = core.getPluginLoader().getPluginDescription(comp).getName();
				} catch (InvalidDescriptionException exc) {
					core.msg(sender, String.format("§c%s has an incorrect description.", comp.getName()));
					return;
				}
			
				if (name.toLowerCase().startsWith(plName.toLowerCase())) {
					files.add(comp);
					
					try {
						core.pm.loadPlugin(comp);
					} catch (UnknownDependencyException exc) {
						core.msg(sender, String.format("§c%s is missing a dependant plugin.", comp.getName()));
						return;
					} catch (InvalidPluginException exc) {
						core.msg(sender, String.format("§c%s is not a plugin.", comp.getName()));
						return;
					} catch (InvalidDescriptionException exc) {
						core.msg(sender, String.format("§c%s has an incorrect description.", comp.getName()));
						return;
					}
				}
			}
		}
		
		for (final Plugin pl : core.pm.getPlugins()) {
			for (final File comp : files) {
				try {
					if (pl.getName().equalsIgnoreCase(core.getPluginLoader().getPluginDescription(comp).getName())) {
						core.pm.enablePlugin(pl);
						
						if (!isReloading)
							core.msg(sender, String.format("§a%s was loaded and enabled.", plName));
					}
				} catch (InvalidDescriptionException exc) {
					exc.printStackTrace();
					return;
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public void unload(final String plName, final CommandSender sender, final boolean isReloading) {
		if (core.isExempt(plName) && !isReloading) {
			core.msg(sender, String.format("§cCould not unload %s, it is exempt.", plName));
			return;
		}
		
		final SimplePluginManager spm = (SimplePluginManager) core.pm;
		final List<PluginCommand> plCmds = this.getCommands(plName);
		
		List<Plugin> plugins = null;
		Map<String, Plugin> lookupNames = null;
		Map<Event, SortedSet<RegisteredListener>> listeners = null;
		
		Field commandMap = null;
		Field knownCommands = null;
		
		boolean reloadListeners = true;
		
		try {
			if (spm != null) {
				final Field pluginsField = spm.getClass().getDeclaredField("plugins");
				final Field lookupField = spm.getClass().getDeclaredField("lookupNames");
				
				pluginsField.setAccessible(true);
				plugins = (List<Plugin>) pluginsField.get(spm);
				
				lookupField.setAccessible(true);
				lookupNames = (Map<String, Plugin>) lookupField.get(spm);
				
				try {
					final Field listenersField = spm.getClass().getDeclaredField("listeners");
					
					listenersField.setAccessible(true);
					listeners = (Map<Event, SortedSet<RegisteredListener>>) listenersField.get(spm);
				} catch (Exception exc) {
					reloadListeners = false;
				}
			}
		} catch (IllegalArgumentException | NoSuchFieldException | SecurityException | IllegalAccessException exc) {
			exc.printStackTrace();
		}
		
		boolean unloaded = false;
		
		for (final Plugin pl : core.pm.getPlugins()) {
			if (pl.getName().toLowerCase().startsWith(plName.toLowerCase())) {
				core.pm.disablePlugin(pl);
				
				if (plugins != null && plugins.contains(pl))
					plugins.remove(pl);
				
				if (lookupNames != null && lookupNames.containsKey(pl.getName()))
					lookupNames.remove(pl.getName());
				
				if (listeners != null && reloadListeners) {
					for (final SortedSet<RegisteredListener> set : listeners.values()) {
						for (final Iterator<RegisteredListener> iter = set.iterator(); iter.hasNext();) {
							RegisteredListener value = iter.next();
							
							if (value.getPlugin() == pl)
								iter.remove();
						}
					}
				}
				
				try {
					if (plCmds != null && plCmds.size() > 0) {
						for (final PluginCommand c : plCmds) {
							commandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
							commandMap.setAccessible(true);
							
							knownCommands = SimpleCommandMap.class.getDeclaredField("knownCommands");
							knownCommands.setAccessible(true);
							
							((Map<String, Command>) knownCommands.get((SimpleCommandMap) commandMap.get(Bukkit.getServer()))).remove(c.getName());
							
							c.unregister((CommandMap) commandMap.get(Bukkit.getServer()));
						}
					}
				} catch (Exception exc) {
					exc.printStackTrace();
				}
				
				for (final Plugin pls : core.pm.getPlugins()) {
					if (pls.getDescription().getDepend() != null) {
						for (final String dep : pls.getDescription().getDepend()) {
							if (dep.equalsIgnoreCase(pl.getName()))
								this.unload(pls.getName(), sender, isReloading);
						}
					}
				}
				
				unloaded = true;
				
				if (!isReloading)
					core.msg(sender, String.format("§2%s was unloaded and disabled.", plName));
				
				break;
			}
		}
		
		if (!unloaded)
			core.msg(sender, String.format("§c%s is not a loaded plugin.", plName));
	}
	
	public void reload(final String plName, final CommandSender sender) {
		this.unload(plName, sender, true);
		this.load(plName, sender, true);
		
		core.msg(sender, String.format("§a%s has been reloaded.", plName));
		core.msg(sender, String.format("§cAliases for %s might not work as expectedly.", plName));
	}
	
	public void reloadConfig(final String plName, final CommandSender sender) {
		boolean reloaded = false;
		
		for (final Plugin pl : core.pm.getPlugins()) {
			if (pl.getName().toLowerCase().startsWith(plName.toLowerCase())) {
				reloaded = true;
				core.msg(sender, String.format("§cFailed to reload %s's config.", plName));
				break;
			}
		}
		
		core.msg(sender, ((reloaded) ? String.format("§aReloaded %s's config.", plName) : String.format("§cFailed to reload %s's config.", plName)));
	}
	
	public void disable(final String plName, final CommandSender sender) {
		if (core.isExempt(plName)) {
			core.msg(sender, String.format("§cCould not disable %s, it is exempt.", plName));
			return;
		}
			
		boolean disabled = false;
		
		for (final Plugin pl : core.pm.getPlugins()) {
			if (pl.getName().toLowerCase().startsWith(plName.toLowerCase())) {
				if (pl.isEnabled()) {
					core.pm.disablePlugin(pl);
					disabled = true;
					break;
				}
			}
		}
		
		core.msg(sender, (disabled) ? String.format("§2%s was disabled.", plName) : String.format("§c%s could not be disabled.", plName));
	}
	
	public void enable(final String plName, final CommandSender sender) {
		boolean enabled = false;
		
		for (final Plugin pl : core.pm.getPlugins()) {
			if (pl.getName().toLowerCase().startsWith(plName.toLowerCase())) {
				if (!pl.isEnabled()) {
					core.pm.enablePlugin(pl);
					enabled = true;
					break;
				}
			}
		}
		
		core.msg(sender, (enabled) ? String.format("§a%s was enabled.", plName) : String.format("§c%s could not be enabled.", plName));
	}

	public void info(final String plName, final CommandSender sender) {
		try {
			final PluginDescriptionFile pf = core.pm.getPlugin(plName).getDescription();
			final List<String> pluginInfo = new ArrayList<String>();
			
			pluginInfo.add("§cPlugin Name: §a" + pf.getName());
			
			if (pf.getAuthors() != null && !pf.getAuthors().isEmpty()) {
				String auths = "";
				
				for (final String auth : pf.getAuthors())
					auths += auth + ", ";
				
				pluginInfo.add("§cAuthor(s): §a" + auths.substring(0, auths.length() - 2));
			}
			
			if (pf.getDescription() != null)
				pluginInfo.add("§cDescription: §a" + pf.getDescription());
			
			if (pf.getVersion() != null)
				pluginInfo.add("§cVersion: §a" + pf.getVersion());
			
			if (pf.getAPIVersion() != null)
				pluginInfo.add("§cAPI-Version: §a" + pf.getAPIVersion());
			
			if (pf.getWebsite() != null)
				pluginInfo.add("§cWebsite: §a" + pf.getWebsite());
			
			if (pf.getDepend() != null && !pf.getDepend().isEmpty()) {
				pluginInfo.add("§cRequired Plugins:");
				
				for (final String dep : pf.getDepend())
					pluginInfo.add("§c* §a" + dep);
			}
			
			if (pf.getSoftDepend() != null && !pf.getSoftDepend().isEmpty()) {
				pluginInfo.add("§cRecommended Plugins:");
				
				for (final String dep : pf.getSoftDepend())
					pluginInfo.add("§c* §a" + dep);
			}
			
			sender.sendMessage(pluginInfo.toArray(new String[pluginInfo.size()]));
		} catch (Exception exc) {
			core.msg(sender, String.format("§cCould not receive info for %s.", plName));
		}
	}
	
	public void status(final String plName, final CommandSender sender) {
		try {
			final Plugin pl = core.pm.getPlugin(plName);
			
			core.msg(sender, String.format("§6%s is currently: %s", plName, ((pl.isEnabled()) ? "§aEnabled" : "§cDisabled")));
		} catch (Exception exc) {
			core.msg(sender, String.format("§cFailed to get the status of %s.", plName));
		}
	}
	
	public void list(final CommandSender sender, final boolean sorted) {
		final List<String> enabled = new ArrayList<String>();
		final List<String> disabled = new ArrayList<String>();
		final int loaded = core.pm.getPlugins().length;
		
		for (final Plugin pl : core.pm.getPlugins()) {
			if (pl.isEnabled())
				enabled.add(pl.getName());
			else disabled.add(pl.getName());
		}
		
		core.msg(sender, String.format("§2%s plugins loaded.", loaded));
		
		Collections.sort(enabled, String.CASE_INSENSITIVE_ORDER);
		Collections.sort(disabled, String.CASE_INSENSITIVE_ORDER);
		
		sender.sendMessage(String.format("§6Enabled §8(§a%s§8)§6:", enabled.size()));
		
		String ebl = "";
		String dbl = "";
		
		for (final String pl : enabled)
			ebl += "§a" + pl + (this.isLegacy(core.pm.getPlugin(pl)) ? "*" : "") + ((sorted) ? "§7 " + core.pm.getPlugin(pl).getDescription().getVersion() : "") + "§a, ";
		
		for (final String pl : disabled)
			dbl += "§c" + pl + (this.isLegacy(core.pm.getPlugin(pl)) ? "*" : "") + ((sorted) ? "§7 " + core.pm.getPlugin(pl).getDescription().getVersion() : "") + "§c, ";
		
		if (enabled.size() > 0)
			sender.sendMessage(ebl.substring(0, ebl.length() - 2));
		
		sender.sendMessage(String.format("§6Disabled §8(§c%s§8)§6:", disabled.size()));
		
		if (disabled.size() > 0)
			sender.sendMessage(dbl.substring(0, dbl.length() - 2));
	}
	
	public List<PluginCommand> getCommands(final String plName) {
		final JavaPlugin pl = (JavaPlugin) core.pm.getPlugin(plName);
		final List<PluginCommand> cmds = new ArrayList<PluginCommand>();
		
		try {
			final Map<String, Map<String, Object>> plCmds = pl.getDescription().getCommands();
			
			for (final String cmdName : plCmds.keySet())
				cmds.add(pl.getCommand(cmdName));
			
			return cmds;
		} catch (Exception exc) {
			return null;
		}
	}
	
	public boolean isLegacy(final Plugin pl) {
		return (pl.getDescription().getAPIVersion() == null);
	}
}
