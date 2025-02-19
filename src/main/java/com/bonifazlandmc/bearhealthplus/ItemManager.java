package com.bonifazlandmc.bearhealthplus;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
	for (String keyString : registeredRecipes.keySet()) {
		NamespacedKey key = new NamespacedKey(plugin, keyString);
		Bukkit.removeRecipe(key);
	}
        registeredRecipes.clear();

        registerAllRecipes();
        plugin.getLogger().info("Ítems y recetas recargados correctamente.");
    }

    private Map<String, Object> getItemData(String path) {
        if (config == null || !config.contains("items." + path)) return null;
        return config.getConfigurationSection("items." + path).getValues(false);
    }

    public ItemStack loadItem(String path) {
        Map<String, Object> itemData = getItemData(path);
        if (itemData == null) return null;

        Map<String, Object> item = (Map<String, Object>) itemData.get("item");
        if (item == null) return null;

        String name = ChatColor.translateAlternateColorCodes('&', (String) item.getOrDefault("name", "&fÍtem Desconocido"));
        List<String> lore = ((List<String>) item.getOrDefault("lore", new ArrayList<>())).stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());

        String materialString = (String) item.getOrDefault("material", "PAPER");

        ItemStack itemStack;
        try {
            if (materialString.startsWith("IA:") && plugin.isUsingItemsAdder()) {
                try {
                    Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
                    Object customStack = customStackClass.getMethod("getInstance", String.class)
                            .invoke(null, materialString.replace("IA:", ""));
                    if (customStack != null) {
                        itemStack = (ItemStack) customStackClass.getMethod("getItemStack").invoke(customStack);
                    } else {
                        plugin.getLogger().warning("ItemsAdder: No se encontró el ítem " + materialString);
                        return null;
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error al intentar cargar un ítem de ItemsAdder: " + e.getMessage());
                    return null;
                }
            } else {
                Material material = Material.getMaterial(materialString);
                if (material == null) {
                    plugin.getLogger().warning("Material inválido: " + materialString);
                    return null;
                }
                itemStack = new ItemStack(material);
            }
        } catch (NoClassDefFoundError e) {
            plugin.getLogger().warning("ItemsAdder no está presente. Ignorando ítem personalizado.");
            return null;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            if (item.containsKey("customModelData")) {
                meta.setCustomModelData((int) item.get("customModelData"));
            }
            itemStack.setItemMeta(meta);
        }

        return itemStack;
    }

    private void registerAllRecipes() {
        registeredRecipes.clear();

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
        Map<String, Object> itemData = getItemData(path);
        if (itemData == null || !itemData.containsKey("recipe")) return null;

        Map<String, Object> recipeData = (Map<String, Object>) itemData.get("recipe");
        List<String> shape = (List<String>) recipeData.get("Shape");
        Map<String, String> materials = (Map<String, String>) recipeData.get("Materials");

        if (shape == null || materials == null) {
            plugin.getLogger().warning("Receta incompleta para el ítem: " + path);
            return null;
        }
        if (!plugin.isUsingItemsAdder()) {
            for (String materialName : materials.values()) {
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

        for (Map.Entry<String, String> entry : materials.entrySet()) {
            char keyChar = entry.getKey().charAt(0);
            String materialName = entry.getValue();

            if (materialName.startsWith("IA:")) {
                try {
                    Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
                    Object customStack = customStackClass.getMethod("getInstance", String.class)
                            .invoke(null, materialName.replace("IA:", ""));
                    if (customStack != null) {
                        shapedRecipe.setIngredient(keyChar, new RecipeChoice.ExactChoice(
                                (ItemStack) customStackClass.getMethod("getItemStack").invoke(customStack)
                        ));
                    } else {
                        plugin.getLogger().warning("ItemsAdder: No se encontró el ítem " + materialName);
                        return null; 
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error al intentar cargar un ítem de ItemsAdder: " + e.getMessage());
                    return null; 
                }
            } else {
                Material material = Material.getMaterial(materialName);
                if (material != null) {
                    shapedRecipe.setIngredient(keyChar, material);
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
        Map<String, Object> itemData = getItemData(path);
        return itemData != null && itemData.containsKey("recipe");
    }
}
