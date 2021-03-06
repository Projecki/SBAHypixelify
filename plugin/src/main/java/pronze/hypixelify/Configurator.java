package pronze.hypixelify;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.screamingsandals.bedwars.Main;
import org.screamingsandals.bedwars.lib.ext.configurate.yaml.YamlConfigurationLoader;
import pronze.hypixelify.utils.Logger;
import pronze.hypixelify.utils.SBAUtil;
import pronze.hypixelify.utils.ShopUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Configurator {

    //TODO: remove the static shits later
    public static HashMap<String, Integer> game_size = new HashMap<>();
    public static HashMap<String, List<String>> Scoreboard_Lines;
    public static List<String> overstats_message;
    public static List<String> gamestart_message;
    public static String date;
    public static boolean tag_health;
    public final File dataFolder;
    public final SBAHypixelify main;

    public File configFile,
            langFolder,
            shopFolder,
            gamesInventoryFolder;
    public FileConfiguration config;

    public Configurator(SBAHypixelify main) {
        this.dataFolder = main.getDataFolder();
        this.main = main;
    }

    private static void checkOrSet(AtomicBoolean modify, FileConfiguration config, String path, Object value) {
        if (!config.isSet(path)) {
            if (value instanceof Map) {
                config.createSection(path, (Map<?, ?>) value);
            } else {
                config.set(path, value);
            }
            modify.set(true);
        }
    }

    public void loadDefaults() {
        dataFolder.mkdirs();

        /* To avoid config confusions*/
        deleteFile("config.yml");

        configFile = new File(dataFolder, "bwaconfig.yml");
        langFolder = new File(dataFolder, "languages");
        gamesInventoryFolder = new File(dataFolder, "games-inventory");
        shopFolder = new File(dataFolder, "shops");

        config = new YamlConfiguration();

        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }

        if (!shopFolder.exists()) {
            shopFolder.mkdirs();
        }
        if (!gamesInventoryFolder.exists()) {
            gamesInventoryFolder.mkdirs();
        }

        addFile("games-inventory/solo.yml");
        addFile("games-inventory/double.yml");
        addFile("games-inventory/triple.yml");
        addFile("games-inventory/squad.yml");

        addFile("shops/" + (Main.isLegacy() ? "legacy-" : "") + "shop.yml");
        addFile("shops/" + (Main.isLegacy() ? "legacy-" : "") + "upgradeShop.yml");

        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        var modify = new AtomicBoolean(false);
        checkOrSetConfig(modify, "locale", "en");
        checkOrSetConfig(modify, "prefix", "[SBAHypixelify]");
        checkOrSetConfig(modify, "debug.enabled", false);
        checkOrSetConfig(modify, "permanent-items", true);
        checkOrSetConfig(modify, "store.replace-store-with-hypixelstore", true);
        checkOrSetConfig(modify, "running-generator-drops", Arrays.asList("DIAMOND", "IRON_INGOT", "EMERALD", "GOLD_INGOT"));
        checkOrSetConfig(modify, "block-item-drops", true);
        checkOrSetConfig(modify, "allowed-item-drops", Arrays.asList("DIAMOND", "IRON_INGOT", "EMERALD", "GOLD_INGOT", "GOLDEN_APPLE", "ENDER_PEAL", "OBSIDIAN", "TNT"));
        checkOrSetConfig(modify, "give-killer-resources", true);
        checkOrSetConfig(modify, "remove-sword-on-upgrade", true);
        checkOrSetConfig(modify, "block-players-putting-certain-items-onto-chest", true);
        checkOrSetConfig(modify, "disable-armor-inventory-movement", true);
        checkOrSetConfig(modify, "version", SBAHypixelify.getInstance().getVersion());
        checkOrSetConfig(modify, "floating-generator.enabled", true);
        checkOrSetConfig(modify, "floating-generator.holo-height", 2.0);
        checkOrSetConfig(modify, "floating-generator.item-height", 0.25);
        checkOrSetConfig(modify, "floating-generator.holo-text", Arrays.asList(
                "§eTier §c{tier}",
                "{material}",
                "§eSpawns in §c{seconds} §eseconds"
        ));

        checkOrSetConfig(modify, "upgrades.timer-upgrades-enabled", true);
        checkOrSetConfig(modify, "upgrades.show-upgrade-message", true);
        checkOrSetConfig(modify, "upgrades.trap-detection-range", 7);
        checkOrSetConfig(modify, "upgrades.multiplier", 0.25);
        checkOrSetConfig(modify, "upgrades.prices.Sharpness-Prot-I", 4);
        checkOrSetConfig(modify, "upgrades.prices.Sharpness-Prot-II", 8);
        checkOrSetConfig(modify, "upgrades.prices.Sharpness-Prot-III", 12);
        checkOrSetConfig(modify, "upgrades.prices.Sharpness-Prot-IV", 16);
        checkOrSetConfig(modify, "upgrades.time.Diamond-I", 120);
        checkOrSetConfig(modify, "upgrades.time.Emerald-I", 200);
        checkOrSetConfig(modify, "upgrades.time.Diamond-II", 400);
        checkOrSetConfig(modify, "upgrades.time.Emerald-II", 520);
        checkOrSetConfig(modify, "upgrades.time.Diamond-III", 700);
        checkOrSetConfig(modify, "upgrades.time.Emerald-III", 900);
        checkOrSetConfig(modify, "upgrades.time.Diamond-IV", 1100);
        checkOrSetConfig(modify, "upgrades.time.Emerald-IV", 1200);
        checkOrSetConfig(modify, "date.format", "MM/dd/yy");
        checkOrSetConfig(modify, "lobby-scoreboard.solo-prefix", "Solo");
        checkOrSetConfig(modify, "lobby-scoreboard.doubles-prefix", "Doubles");
        checkOrSetConfig(modify, "lobby-scoreboard.triples-prefix", "Triples");
        checkOrSetConfig(modify, "lobby-scoreboard.squads-prefix", "Squads");
        checkOrSetConfig(modify, "lobby-scoreboard.enabled", true);
        checkOrSetConfig(modify, "lobby-scoreboard.interval", 2);
        checkOrSetConfig(modify, "lobby-scoreboard.state.waiting", "&fWaiting...");
        checkOrSetConfig(modify, "first_start", true);
        checkOrSetConfig(modify, "shout.time-out", 60);
        checkOrSetConfig(modify, "message.maximum-enchant-lore", Arrays.asList("Maximum Enchant", "Your team already has maximum Enchant."));
        checkOrSetConfig(modify, "disable-sword-armor-damage", true);
        checkOrSetConfig(modify, "shop-name", "[SBAHypixelify] shop");

        checkOrSetConfig(modify, "game.tab-health", true);
        checkOrSetConfig(modify, "game.tag-health", true);

        checkOrSetConfig(modify, "games-inventory.enabled", true);
        checkOrSetConfig(modify, "games-inventory.gui.solo-prefix", "Bed Wars Solo");
        checkOrSetConfig(modify, "games-inventory.gui.double-prefix", "Bed Wars Doubles");
        checkOrSetConfig(modify, "games-inventory.gui.triple-prefix", "Bed Wars Triples");
        checkOrSetConfig(modify, "games-inventory.gui.squad-prefix", "Bed Wars Squads");

        checkOrSetConfig(modify, "game-start.message", Arrays.asList(
                "&a\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac"
                , "                             &f&lBed Wars"
                , ""
                , "    &e&lProtect your bed and destroy the enemy beds."
                , "     &e&lUpgrade yourself and your team by collecting"
                , "   &e&lIron, Gold, Emerald and Diamond from generators"
                , "            &e&lto access powerful upgrades."
                , ""
                , "&a\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac"
        ));
        checkOrSetConfig(modify, "overstats.message", Arrays.asList(
                "&a\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac"
                , "                             &e&lBEDWARS"
                , ""
                , "                             {color}{win_team}"
                , "                             {win_team_players}"
                , ""
                , "    &e&l1st&7 - &f{first_1_kills_player} &7- &f{first_1_kills}"
                , "    &6&l2nd&7 - &f{first_2_kills_player} &7- &f{first_2_kills}"
                , "    &c&l3rd&7 - &f{first_3_kills_player} &7- &f{first_3_kills}"
                , ""
                , "&a\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac\u25ac"
        ));
        checkOrSetConfig(modify, "scoreboard.you", "&7YOU");
        checkOrSetConfig(modify, "scoreboard.lines.default", Arrays.asList(
                "&7{date}"
                , ""
                , "{tier}"
                , ""
                , "{team_status}"
                , ""
                , "&fKills: &a{kills}"
                , "&fBed Broken: &a{beds}"
                , ""
                , "&ewww.minecraft.net"
        ));
        checkOrSetConfig(modify, "scoreboard.lines.5", Arrays.asList(
                "&7{date}"
                , ""
                , "{tier}"
                , ""
                , "{team_status}"
                , ""
                , "&ewww.minecraft.net"
        ));

        checkOrSetConfig(modify, "lobby-scoreboard.state.countdown", "&fStarting in &a{countdown}s");

        //TODO: Create an algorithm
        checkOrSetConfig(modify, "lobby-scoreboard.title", Arrays.asList(
                  "&e&lBED WARS"
                , "&e&lBED WARS"
                , "&e&lBED WARS"
                , "&e&lBED WARS"
                , "&e&lBED WARS"
                , "&e&lBED WARS"
                , "&e&lBED WARS"
                , "&e&lBED WARS"
                , "&e&lBED WARS"
                , "&e&lBED WARS"
                , "&e&lBED WARS"
                , "&e&lBED WARS"
                , "&e&lBED WARS"
                , "&e&lBED WARS"
                , "&e&lBED WARS"
                , "&e&lBED WARS"
                , "&e&lBED WARS"
                , "&e&lBED WARS"
                , "&e&lBED WARS"
                , "&6&lB&e&lED WARS"
                , "&f&lB&6&lE&e&lD WARS"
                , "&f&lBE&6&lD&e&l WARS"
                , "&f&lBED&6&l &e&lWARS"
                , "&f&lBED &6&lW&e&lARS"
                , "&f&lBED W&6&lA&e&lRS"
                , "&f&lBED WA&6&lR&e&lS"
                , "&f&lBED WAR&6&lS"
                , "&f&lBED WARS"
                , "&e&lBED WARS"
                , "&f&lBED WARS"));

        checkOrSetConfig(modify, "lobby_scoreboard.lines", Arrays.asList(
                "&7{date}"
                , ""
                , "&fMap: &a{game}"
                , "&fPlayers: &a{players}/{maxplayers}"
                , ""
                , "{state}"
                , ""
                , "&fMode: &a{mode}"
                , "&fVersion: &7v1.1"
                , ""
                , "&ewww.sample.net"
        ));
        checkOrSetConfig(modify, "main-lobby.enabled", false);
        checkOrSetConfig(modify, "main-lobby.title", "&e&lBED WARS");
        checkOrSetConfig(modify, "main-lobby.custom-chat", true);
        checkOrSetConfig(modify, "main-lobby.chat-format", "{color}[{level}✫] {name}: {message}");
        checkOrSetConfig(modify, "main-lobby.lines", Arrays.asList(
                "",
                "Your Level: &a{level}",
                "",
                "Progress: {progress}",
                "{bar}",
                "",
                "Loot Chests: &60",
                "",
                "Total Kills: &a{kills}",
                "Total Wins: &a{deaths}",
                "",
                "K/D ratio: &6{k/d}",
                "",
                "&ewww.minecraft.net"
        ));

        checkOrSetConfig(modify, "commands.invalid-command",
                "[SBAHypixelify] &cUnknown command, do /bwaddon help for more.");

        checkOrSetConfig(modify, "dragon.name-format", "%teamcolor%%team% Dragon");
        checkOrSetConfig(modify, "dragon.custom-name-enabled", true);
        checkOrSetConfig(modify, "commands.player-only", "[SBAHypixelify] &cOnly players can use this command!");
        checkOrSetConfig(modify, "commands.no-permissions", "[SBAHypixelify]&cYou do not have permissions to do this command!");
        checkOrSetConfig(modify, "experimental.reset-item-meta-on-purchase", false);

        Main.getGameNames().forEach(gameName -> {
            final var configKey = "lobby-scoreboard.player-size.games." + gameName;
            checkOrSetConfig(modify, configKey, 4);
            int size = config.getInt("lobby-scoreboard.player-size.games." + gameName, 4);
            game_size.put(gameName, size);
        });

        if (modify.get()) {
            saveConfig();
        }

        date = config.getString("date.format");
        Scoreboard_Lines = new HashMap<>();
        for (String key : config.getConfigurationSection("scoreboard.lines").getKeys(false))
            Scoreboard_Lines.put(key,
                    SBAUtil.translateColors(getStringList("scoreboard.lines." + key)));

        tag_health = SBAHypixelify.getConfigurator().config.getBoolean("tag_health");
        overstats_message = SBAUtil.translateColors(getStringList("overstats.message"));
        gamestart_message = SBAUtil.translateColors(getStringList("game-start.message"));

        ShopUtil.initKeys();
        if (config.getBoolean("first_start")) {
            Bukkit.getLogger().info( "§aDetected first start");
            upgradeCustomFiles();
            config.set("first_start", false);
            saveConfig();
        }
    }

    private void addFile(String fileName, String saveTo) {
        final var file = new File(dataFolder, fileName);
        if (!file.exists()) {
            main.saveResource(saveTo, false);
        }
    }

    private void deleteFile(String fileName) {
        final var file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }
    }

    private void addFile(String fileName) {
        addFile(fileName, fileName);
    }

    public void upgradeCustomFiles() {
        config.set("version", SBAHypixelify.getInstance().getVersion());
        config.set("autoset-bw-config", false);
        saveConfig();

        main.saveResource("shops/shop.yml", true);
        main.saveResource("shops/upgradeShop.yml", true);
        try (final var inputStream = main.getResource("config.yml")){
            if (inputStream != null) {
                final var configFile =
                        new File(Main.getInstance().getDataFolder().getAbsolutePath(), "config.yml");
                if (configFile.exists()) {
                    Logger.trace("Replacing BedWars config.yml");
                    Logger.trace("Deleting config file, status: {}", String.valueOf(configFile.delete()));;
                }
                Logger.trace("Creating config file, status: {}", String.valueOf(configFile.createNewFile()));
                try (final var outputStream = new FileOutputStream(configFile)) {
                    inputStream.transferTo(outputStream);
                } catch (Exception e) {
                    e.printStackTrace();
                }
             }
        } catch (IOException e) {
            e.printStackTrace();
        }
        SBAUtil.reloadPlugin(Main.getInstance());
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public List<String> getStringList(String string) {
        final var list = new ArrayList<String>();
        for (String s : config.getStringList(string)) {
            s = ChatColor.translateAlternateColorCodes('&', s);
            list.add(s);
        }
        return list;
    }

    public String getString(String path) {
        if (config.isSet(path)) {
            final var val = config.getString(path);
            if (val != null) {
                return ChatColor.translateAlternateColorCodes('&', val);
            }
        }
        return null;
    }

    public String getString(String path, String def) {
        final var str = getString(path);
        if (str == null) {
            return def;
        }
        return str;
    }

    private void checkOrSetConfig(AtomicBoolean modify, String path, Object value) {
        checkOrSet(modify, this.config, path, value);
    }

}