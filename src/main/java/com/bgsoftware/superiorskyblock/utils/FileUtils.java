package com.bgsoftware.superiorskyblock.utils;

import com.bgsoftware.common.config.CommentedConfiguration;
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.menu.ISuperiorMenu;
import com.bgsoftware.superiorskyblock.api.objects.Pair;
import com.bgsoftware.superiorskyblock.menu.button.SuperiorMenuButton;
import com.bgsoftware.superiorskyblock.menu.button.impl.BackButton;
import com.bgsoftware.superiorskyblock.menu.button.impl.DummyButton;
import com.bgsoftware.superiorskyblock.menu.file.MenuPatternSlots;
import com.bgsoftware.superiorskyblock.menu.pattern.SuperiorMenuPattern;
import com.bgsoftware.superiorskyblock.utils.debug.PluginDebugger;
import com.bgsoftware.superiorskyblock.utils.items.EnchantsUtils;
import com.bgsoftware.superiorskyblock.utils.items.ItemBuilder;
import com.bgsoftware.superiorskyblock.utils.items.TemplateItem;
import com.bgsoftware.superiorskyblock.wrappers.SoundWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public final class FileUtils {

    private static final SuperiorSkyblockPlugin plugin = SuperiorSkyblockPlugin.getPlugin();
    private static final Object fileMutex = new Object();

    private FileUtils() {

    }

    @Nullable
    public static TemplateItem getItemStack(String fileName, ConfigurationSection section) {
        if (section == null || !section.contains("type"))
            return null;

        Material type;
        short data;

        try {
            type = Material.valueOf(section.getString("type"));
            data = (short) section.getInt("data");
        } catch (IllegalArgumentException ex) {
            SuperiorSkyblockPlugin.log("&c[" + fileName + "] Couldn't convert " + section.getCurrentPath() + " into an itemstack. Check type & data sections!");
            PluginDebugger.debug(ex);
            return null;
        }

        ItemBuilder itemBuilder = new ItemBuilder(type, data);

        if (section.contains("name"))
            itemBuilder.withName(StringUtils.translateColors(section.getString("name")));

        if (section.contains("lore"))
            itemBuilder.withLore(section.getStringList("lore"));

        if (section.contains("enchants")) {
            for (String _enchantment : section.getConfigurationSection("enchants").getKeys(false)) {
                Enchantment enchantment;

                try {
                    enchantment = Enchantment.getByName(_enchantment);
                } catch (Exception ex) {
                    SuperiorSkyblockPlugin.log("&c[" + fileName + "] Couldn't convert " + section.getCurrentPath() + ".enchants." + _enchantment + " into an enchantment, skipping...");
                    PluginDebugger.debug(ex);
                    continue;
                }

                itemBuilder.withEnchant(enchantment, section.getInt("enchants." + _enchantment));
            }
        }

        if (section.getBoolean("glow", false)) {
            itemBuilder.withEnchant(EnchantsUtils.getGlowEnchant(), 1);
        }

        if (section.contains("flags")) {
            for (String flag : section.getStringList("flags"))
                itemBuilder.withFlags(ItemFlag.valueOf(flag));
        }

        if (section.contains("skull")) {
            itemBuilder.asSkullOf(section.getString("skull"));
        }

        if (section.getBoolean("unbreakable", false)) {
            itemBuilder.setUnbreakable();
        }

        if (section.contains("effects")) {
            ConfigurationSection effectsSection = section.getConfigurationSection("effects");
            for (String _effect : effectsSection.getKeys(false)) {
                PotionEffectType potionEffectType = PotionEffectType.getByName(_effect);

                if (potionEffectType == null) {
                    SuperiorSkyblockPlugin.log("&c[" + fileName + "] Couldn't convert " + effectsSection.getCurrentPath() + "." + _effect + " into a potion effect, skipping...");
                    continue;
                }

                int duration = effectsSection.getInt(_effect + ".duration", -1);
                int amplifier = effectsSection.getInt(_effect + ".amplifier", 0);

                if (duration == -1) {
                    SuperiorSkyblockPlugin.log("&c[" + fileName + "] Potion effect " + effectsSection.getCurrentPath() + "." + _effect + " is missing duration, skipping...");
                    continue;
                }

                itemBuilder.withPotionEffect(new PotionEffect(potionEffectType, duration, amplifier));
            }
        }

        if (section.contains("entity")) {
            String entity = section.getString("entity");
            try {
                itemBuilder.withEntityType(EntityType.valueOf(entity.toUpperCase()));
            } catch (IllegalArgumentException ex) {
                SuperiorSkyblockPlugin.log("&c[" + fileName + "] Couldn't convert " + entity + " into an entity type, skipping...");
                PluginDebugger.debug(ex);
            }
        }

        if (section.contains("customModel")) {
            itemBuilder.withCustomModel(section.getInt("customModel"));
        }

        return new TemplateItem(itemBuilder);
    }

    @Nullable
    public static <M extends ISuperiorMenu> Pair<MenuPatternSlots, CommentedConfiguration> loadMenu(
            SuperiorMenuPattern.AbstractBuilder<?, ?, M> menuPattern,
            String fileName,
            @Nullable BiFunction<SuperiorSkyblockPlugin, YamlConfiguration, Boolean> convertOldMenu) {
        return loadMenu(menuPattern, fileName, false, convertOldMenu);
    }

    @Nullable
    public static <M extends ISuperiorMenu> Pair<MenuPatternSlots, CommentedConfiguration> loadMenu(
            SuperiorMenuPattern.AbstractBuilder<?, ?, M> menuPattern,
            String fileName,
            boolean customMenu,
            @Nullable BiFunction<SuperiorSkyblockPlugin, YamlConfiguration, Boolean> convertOldMenu) {
        String menuPath = customMenu ? "custom/" : "";

        File file = new File(plugin.getDataFolder(), "menus/" + menuPath + fileName);

        if (!file.exists() && !customMenu)
            FileUtils.saveResource("menus/" + fileName);

        CommentedConfiguration cfg = new CommentedConfiguration();

        try {
            cfg.load(file);
        } catch (InvalidConfigurationException error) {
            SuperiorSkyblockPlugin.log("&c[" + fileName + "] There is an issue with the format of the file.");
            PluginDebugger.debug(error);
            return null;
        } catch (IOException error) {
            SuperiorSkyblockPlugin.log("&c[" + fileName + "] An unexpected error occurred while parsing the file:");
            PluginDebugger.debug(error);
            error.printStackTrace();
            return null;
        }

        if (convertOldMenu != null && convertOldMenu.apply(plugin, cfg)) {
            try {
                cfg.save(file);
            } catch (Exception ex) {
                ex.printStackTrace();
                PluginDebugger.debug(ex);
            }
        }

        menuPattern.setTitle(StringUtils.translateColors(cfg.getString("title", "")))
                .setInventoryType(InventoryType.valueOf(cfg.getString("type", "CHEST")))
                .setPreviousMoveAllowed(cfg.getBoolean("previous-menu", true))
                .setOpeningSound(FileUtils.getSound(cfg.getConfigurationSection("open-sound")));

        MenuPatternSlots menuPatternSlots = new MenuPatternSlots();
        List<String> pattern = cfg.getStringList("pattern");

        menuPattern.setRowsSize(pattern.size());

        String backButton = cfg.getString("back", "");
        boolean backButtonFound = false;

        for (int row = 0; row < pattern.size() && row < 6; row++) {
            String patternLine = pattern.get(row).replace(" ", "");
            for (int i = 0; i < patternLine.length() && i < 9; i++) {
                int slot = row * 9 + i;

                char ch = patternLine.charAt(i);

                boolean isBackButton = backButton.contains(ch + "");

                if (isBackButton) {
                    backButtonFound = true;
                }

                SuperiorMenuButton.AbstractBuilder<?, ?, M> buttonBuilder = isBackButton ?
                        new BackButton.Builder<>() : new DummyButton.Builder<>();

                menuPattern.setButton(slot, buttonBuilder
                        .setButtonItem(getItemStack(fileName, cfg.getConfigurationSection("items." + ch)))
                        .setCommands(cfg.getStringList("commands." + ch))
                        .setClickSound(getSound(cfg.getConfigurationSection("sounds." + ch)))
                        .setRequiredPermission(cfg.getString("permissions." + ch + ".permission"))
                        .setLackPermissionsSound(getSound(cfg.getConfigurationSection("permissions." + ch + ".no-access-sound"))));

                menuPatternSlots.addSlot(ch, slot);
            }
        }

        if (plugin.getSettings().isOnlyBackButton() && !backButtonFound) {
            SuperiorSkyblockPlugin.log("&c[" + fileName + "] Menu doesn't have a back button, it's impossible to close it.");
            return null;
        }

        return new Pair<>(menuPatternSlots, cfg);
    }

    public static Location toLocation(String location) {
        String[] sections = location.split(",");
        return new Location(Bukkit.getWorld(sections[0]), Double.parseDouble(sections[1]), Double.parseDouble(sections[2]),
                Double.parseDouble(sections[3]), Float.parseFloat(sections[4]), Float.parseFloat(sections[5]));
    }

    public static void copyResource(String resourcePath) {
        String fixedPath = resourcePath + ".jar";
        File dstFile = new File(plugin.getDataFolder(), fixedPath);

        if (dstFile.exists())
            //noinspection ResultOfMethodCallIgnored
            dstFile.delete();

        plugin.saveResource(resourcePath, true);

        File file = new File(plugin.getDataFolder(), resourcePath);
        //noinspection ResultOfMethodCallIgnored
        file.renameTo(dstFile);
    }

    public static void saveResource(String resourcePath) {
        saveResource(resourcePath, resourcePath);
    }

    public static void saveResource(String destination, String resourcePath) {
        try {
            for (ServerVersion serverVersion : ServerVersion.getByOrder()) {
                String version = serverVersion.name().substring(1);
                if (resourcePath.endsWith(".yml") && plugin.getResource(resourcePath.replace(".yml", version + ".yml")) != null) {
                    resourcePath = resourcePath.replace(".yml", version + ".yml");
                    break;
                } else if (resourcePath.endsWith(".schematic") && plugin.getResource(resourcePath.replace(".schematic", version + ".schematic")) != null) {
                    resourcePath = resourcePath.replace(".schematic", version + ".schematic");
                    break;
                }
            }

            File file = new File(plugin.getDataFolder(), resourcePath);
            plugin.saveResource(resourcePath, true);

            if (!destination.equals(resourcePath)) {
                File dest = new File(plugin.getDataFolder(), destination);
                //noinspection ResultOfMethodCallIgnored
                file.renameTo(dest);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            PluginDebugger.debug(ex);
        }
    }

    public static InputStream getResource(String resourcePath) {
        try {
            for (ServerVersion serverVersion : ServerVersion.getByOrder()) {
                String version = serverVersion.name().substring(1);
                if (resourcePath.endsWith(".yml") && plugin.getResource(resourcePath.replace(".yml", version + ".yml")) != null) {
                    resourcePath = resourcePath.replace(".yml", version + ".yml");
                    break;
                } else if (resourcePath.endsWith(".schematic") && plugin.getResource(resourcePath.replace(".schematic", version + ".schematic")) != null) {
                    resourcePath = resourcePath.replace(".schematic", version + ".schematic");
                    break;
                }
            }

            return plugin.getResource(resourcePath);
        } catch (Exception ex) {
            ex.printStackTrace();
            PluginDebugger.debug(ex);
            return null;
        }
    }

    public static SoundWrapper getSound(ConfigurationSection section) {
        if (section == null)
            return null;

        String soundType = section.getString("type");

        if (soundType == null)
            return null;

        Sound sound = null;

        try {
            sound = Sound.valueOf(soundType);
        } catch (Exception error) {
            PluginDebugger.debug(error);
        }

        if (sound == null)
            return null;

        return new SoundWrapper(sound, (float) section.getDouble("volume", 1),
                (float) section.getDouble("pitch", 1));
    }

    public static List<Class<?>> getClasses(URL jar, Class<?> clazz) {
        return getClasses(jar, clazz, clazz.getClassLoader());
    }

    public static List<Class<?>> getClasses(URL jar, Class<?> clazz, ClassLoader classLoader) {
        List<Class<?>> list = new ArrayList<>();

        try (URLClassLoader cl = new URLClassLoader(new URL[]{jar}, classLoader); JarInputStream jis = new JarInputStream(jar.openStream())) {
            JarEntry jarEntry;
            while ((jarEntry = jis.getNextJarEntry()) != null) {
                String name = jarEntry.getName();

                if (!name.endsWith(".class")) {
                    continue;
                }

                name = name.replace("/", ".");
                String clazzName = name.substring(0, name.lastIndexOf(".class"));

                Class<?> c = cl.loadClass(clazzName);

                if (clazz.isAssignableFrom(c)) {
                    list.add(c);
                }
            }
        } catch (Throwable ignored) {
        }

        return list;
    }

    public static void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] childFiles = directory.listFiles();
            if (childFiles != null) {
                for (File file : childFiles)
                    deleteDirectory(file);
            }
        }

        //noinspection ResultOfMethodCallIgnored
        directory.delete();
    }

    public static void replaceString(File file, String str, String replace) {
        synchronized (fileMutex) {
            StringBuilder stringBuilder = new StringBuilder();

            try {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null)
                        stringBuilder.append("\n").append(line);
                }

                if (stringBuilder.length() > 0) {
                    try (FileWriter writer = new FileWriter(file)) {
                        writer.write(stringBuilder.substring(1).replace(str, replace));
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                PluginDebugger.debug(ex);
            }
        }
    }

}
