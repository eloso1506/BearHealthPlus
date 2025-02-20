package com.bonifazlandmc.bearhealthplus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class ItemManager {
    private final BearHealthPlus plugin;
    private YamlConfiguration config;
    private final Map<String, ShapedRecipe> registeredRecipes = new HashMap<>();

    public ItemManager(BearHealthPlus plugin, boolean useItemsAdder) {
        this.plugin = plugin;
        reloadItems();
        registerAllRecipes();
    }

    public void reloadItems() {
        File itemsFile = new File(plugin.getDataFolder(), "items.yml");
        if (!itemsFile.exists()) {
            plugin.getLogger().warning("No se encontró items.yml en la carpeta de datos.");
            return;
        }
        config = YamlConfiguration.loadConfiguration(itemsFile);

        // Eliminar recetas antiguas antes de recargar
        for (String keyString : registeredRecipes.keySet()) {
            NamespacedKey key = new NamespacedKey(plugin, keyString);
            Bukkit.removeRecipe(key);
        }
        registeredRecipes.clear();

        registerAllRecipes();
        plugin.getLogger().info("Ítems y recetas recargados correctamente.");
    }

    private ConfigurationSection getItemSection(String path) {
        if (config == null) return null;
        return config.getConfigurationSection("items." + path);
    }

    public ItemStack loadItem(String path) {
        ConfigurationSection itemSection = getItemSection(path);
        if (itemSection == null) return null;

        ConfigurationSection item = itemSection.getConfigurationSection("item");
        if (item == null) return null;

        String name = ChatColor.translateAlternateColorCodes('&', item.getString("name", "&fÍtem Desconocido"));
        List<String> lore = item.getStringList("lore").stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());

        String materialString = item.getString("material", "PAPER");
        ItemStack itemStack;

        if (materialString.startsWith("IA:") && plugin.isUsingItemsAdder()) {
            itemStack = loadItemsAdderItem(materialString);
        } else {
            Material material = Material.getMaterial(materialString);
            if (material == null) {
                plugin.getLogger().warning("Material inválido: " + materialString);
                return null;
            }
            itemStack = new ItemStack(material);
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            if (item.contains("customModelData")) {
                meta.setCustomModelData(item.getInt("customModelData"));
            }
            itemStack.setItemMeta(meta);
        }

        return itemStack;
    }

    private ItemStack loadItemsAdderItem(String materialString) {
        try {
            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Object customStack = customStackClass.getMethod("getInstance", String.class)
                    .invoke(null, materialString.replace("IA:", ""));
            if (customStack != null) {
                return (ItemStack) customStackClass.getMethod("getItemStack").invoke(customStack);
            } else {
                plugin.getLogger().warning("ItemsAdder: No se encontró el ítem " + materialString);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error al cargar un ítem de ItemsAdder: " + e.getMessage());
        }
        return null;
    }

    private void registerAllRecipes() {
        if (config == null || !config.contains("items")) return;

        for (String path : config.getConfigurationSection("items").getKeys(false)) {
            ShapedRecipe recipe = getRecipe(path);
            if (recipe != null) {
                registeredRecipes.put(path, recipe);
                Bukkit.addRecipe(recipe);
                plugin.getLogger().info("Receta registrada: " + path);
            }
        }
    }

    public ShapedRecipe getRecipe(String path) {
        ConfigurationSection itemSection = getItemSection(path);
        if (itemSection == null) return null;

        ConfigurationSection recipeSection = itemSection.getConfigurationSection("recipe");
        if (recipeSection == null) return null;

        List<String> shape = recipeSection.getStringList("Shape");
        ConfigurationSection materialsSection = recipeSection.getConfigurationSection("Materials");

        if (shape.isEmpty() || materialsSection == null) {
            plugin.getLogger().warning("Receta incompleta para el ítem: " + path);
            return null;
        }

        // Validar ItemsAdder antes de procesar la receta
        if (!plugin.isUsingItemsAdder()) {
            for (Object obj : materialsSection.getValues(false).values()) {
                String materialName = obj.toString(); // Convertimos Object a String
                if (materialName.startsWith("IA:")) {
                    plugin.getLogger().warning("ItemsAdder no está presente. Omitiendo receta: " + path);
                    return null;
                }
            }
        }

        ItemStack resultItem = loadItem(path);
        if (resultItem == null) return null;

        NamespacedKey key = new NamespacedKey(plugin, path);
        ShapedRecipe shapedRecipe = new ShapedRecipe(key, resultItem);
        shapedRecipe.shape(shape.toArray(new String[0]));

        for (String keyChar : materialsSection.getKeys(false)) {
            String materialName = materialsSection.getString(keyChar);
            if (materialName == null) continue;

            if (materialName.startsWith("IA:")) {
                ItemStack iaItem = loadItemsAdderItem(materialName);
                if (iaItem != null) {
                    shapedRecipe.setIngredient(keyChar.charAt(0), new RecipeChoice.ExactChoice(iaItem));
                } else {
                    plugin.getLogger().warning("ItemsAdder: No se encontró el ítem " + materialName);
                    return null;
                }
            } else {
                Material material = Material.getMaterial(materialName);
                if (material != null) {
                    shapedRecipe.setIngredient(keyChar.charAt(0), material);
                } else {
                    plugin.getLogger().warning("Material inválido en la receta: " + materialName);
                    return null;
                }
            }
        }
        return shapedRecipe;
    }

    public Map<String, ShapedRecipe> getRegisteredRecipes() {
        return registeredRecipes;
    }

    public boolean hasRecipe(String path) {
        return getItemSection(path) != null && getItemSection(path).contains("recipe");
    }
}
