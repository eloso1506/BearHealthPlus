package com.bonifazlandmc.bearhealthplus;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class BearHealthPlus extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private ItemManager itemManager;
    private YamlConfiguration itemsConfig;
    private boolean useItemsAdder;

    @Override
    public void onEnable() {
        initializeFiles();
        loadConfigurations();
        registerComponents();
        getLogger().info(ChatColor.GREEN + "BearHealthPlus activado correctamente.");
    }

    private void initializeFiles() {
        saveDefaultConfig();
        File itemsFile = new File(getDataFolder(), "items.yml");
        if (!itemsFile.exists()) saveResource("items.yml", false);
        reloadItemsConfig();
    }

    private void loadConfigurations() {
        reloadConfig();
        useItemsAdder = isItemsAdderAvailable();
        itemManager = new ItemManager(this, useItemsAdder);
    }

    private boolean isItemsAdderAvailable() {
    try {
        Class.forName("dev.lone.itemsadder.api.CustomStack");
        return getServer().getPluginManager().isPluginEnabled("ItemsAdder");
    } catch (ClassNotFoundException e) {
        return false;
    }
}

    private void registerComponents() {
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("bhp")).setExecutor(this);
        Objects.requireNonNull(getCommand("bhp")).setTabCompleter(this);
    }

    private void reloadItemsConfig() {
        itemsConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "items.yml"));
    }

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player) && (args.length == 0 || !args[0].equalsIgnoreCase("reload"))) {
			sender.sendMessage(ChatColor.RED + "Este comando solo puede ser usado por jugadores.");
			return true;
		}
	
		// Comando /bhp add {corazones} {jugador}
		if (args.length >= 3 && args[0].equalsIgnoreCase("add")) {
			if (!sender.hasPermission("bhp.admin")) {
				sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
				return true;
			}
	
			Player target = Bukkit.getPlayer(args[2]);
			if (target == null) {
				sender.sendMessage(ChatColor.RED + "El jugador no está en línea.");
				return true;
			}
	
			try {
				int extraHearts = Integer.parseInt(args[1]);
				AttributeInstance maxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
				if (maxHealth != null) {
					double newHealth = maxHealth.getBaseValue() + (extraHearts * 2);
					maxHealth.setBaseValue(newHealth);
					sender.sendMessage(ChatColor.GREEN + "Se añadieron " + extraHearts + " corazones a " + target.getName() + ".");
				}
			} catch (NumberFormatException e) {
				sender.sendMessage(ChatColor.RED + "Número de corazones inválido.");
			}
			return true;
		}
			switch (args.length > 0 ? args[0].toLowerCase() : "") {
			case "help":
				sendHelpMessage(sender);
				break;
	
			case "give":
				if (args.length < 4) {
					sender.sendMessage(ChatColor.RED + "Uso: /bhp give {item} {cantidad} {jugador}");
					break;
				}
				giveItem(sender, args);
				break;
	
			case "get":
				if (args.length < 2) {
					sender.sendMessage(ChatColor.RED + "Uso: /bhp get {item}");
					break;
				}
				getItem(sender, args[1]);
				break;
	
			case "reset":
				if (args.length < 2) {
					sender.sendMessage(ChatColor.RED + "Uso: /bhp reset {jugador}");
					break;
				}
				resetHealth(sender, args[1]);
				break;
	
			case "set":
				if (args.length < 3) {
					sender.sendMessage(ChatColor.RED + "Uso: /bhp set {jugador} {corazones}");
					break;
				}
				setHealth(sender, args[1], args[2]);
				break;
	
			case "reload":
				reloadConfig();
				reloadItemsConfig();
				sender.sendMessage(ChatColor.GREEN + "Configuraciones recargadas correctamente.");
				break;
	
			default:
				sender.sendMessage(ChatColor.RED + "Comando desconocido. Usa /bhp help para ver los comandos.");
				break;
		}
		return true;
	}


    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== BearHealthPlus Comandos ===");
        sender.sendMessage(ChatColor.YELLOW + "/bhp help" + ChatColor.WHITE + " - Muestra este mensaje.");
        sender.sendMessage(ChatColor.YELLOW + "/bhp give {item} {cantidad} {jugador}" + ChatColor.WHITE + " - Da un ítem a un jugador.");
        sender.sendMessage(ChatColor.YELLOW + "/bhp get {item}" + ChatColor.WHITE + " - Obtienes un ítem.");
        sender.sendMessage(ChatColor.YELLOW + "/bhp reset {jugador}" + ChatColor.WHITE + " - Restablece la vida de un jugador.");
        sender.sendMessage(ChatColor.YELLOW + "/bhp set {jugador} {corazones}" + ChatColor.WHITE + " - Establece la cantidad exacta de corazones de un jugador.");
        sender.sendMessage(ChatColor.YELLOW + "/bhp reload" + ChatColor.WHITE + " - Recarga la configuración.");
    }

    private void giveItem(CommandSender sender, String[] args) {
        String itemKey = args[1];
        int amount;
        Player target = Bukkit.getPlayer(args[3]);

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
            return;
        }
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "La cantidad debe ser un número.");
            return;
        }

        ItemStack item = itemManager.loadItem(itemKey);
        if (item == null) {
            sender.sendMessage(ChatColor.RED + "El ítem especificado no existe.");
            return;
        }

        item.setAmount(amount);
        target.getInventory().addItem(item);
        sender.sendMessage(ChatColor.GREEN + "Has dado " + amount + "x " + itemKey + " a " + target.getName());
    }

    private void getItem(CommandSender sender, String itemKey) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Solo los jugadores pueden usar este comando.");
            return;
        }
        Player player = (Player) sender;

        ItemStack item = itemManager.loadItem(itemKey);
        if (item == null) {
            sender.sendMessage(ChatColor.RED + "El ítem especificado no existe.");
            return;
        }

        player.getInventory().addItem(item);
        player.sendMessage(ChatColor.GREEN + "Has recibido el ítem: " + itemKey);
    }

    private void resetHealth(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
            return;
        }
        AttributeInstance maxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(20.0);
            target.setHealth(20.0);
            sender.sendMessage(ChatColor.GREEN + "Vida de " + target.getName() + " restablecida.");
        }
    }

    private void setHealth(CommandSender sender, String playerName, String heartsStr) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
            return;
        }

        double hearts;
        try {
            hearts = Double.parseDouble(heartsStr) * 2.0;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "La cantidad de corazones debe ser un número.");
            return;
        }

        AttributeInstance maxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(hearts);
            target.setHealth(hearts);
            sender.sendMessage(ChatColor.GREEN + "La vida de " + target.getName() + " ha sido establecida en " + hearts / 2.0 + " corazones.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return Arrays.asList("help", "give", "get", "reset", "set", "reload");
        return Collections.emptyList();
    }

    public boolean isUsingItemsAdder() {
        return useItemsAdder;
    }
}
