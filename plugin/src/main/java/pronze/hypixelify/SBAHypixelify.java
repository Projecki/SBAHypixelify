package pronze.hypixelify;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.screamingsandals.bedwars.Main;
import org.screamingsandals.bedwars.api.BedwarsAPI;
import org.screamingsandals.bedwars.api.game.Game;
import org.screamingsandals.bedwars.lib.ext.bstats.bukkit.Metrics;
import org.screamingsandals.bedwars.lib.nms.utils.ClassStorage;
import pronze.hypixelify.api.SBAHypixelifyAPI;
import pronze.hypixelify.api.game.GameStorage;
import pronze.hypixelify.api.wrapper.PlayerWrapper;
import pronze.hypixelify.commands.CommandManager;
import pronze.hypixelify.game.Arena;
import pronze.hypixelify.game.RotatingGenerators;
import pronze.hypixelify.inventories.CustomShop;
import pronze.hypixelify.inventories.GamesInventory;
import pronze.hypixelify.lib.lang.I18n;
import pronze.hypixelify.listener.*;
import pronze.hypixelify.placeholderapi.SBAExpansion;
import pronze.hypixelify.service.PlayerWrapperService;
import pronze.hypixelify.utils.Logger;
import pronze.hypixelify.utils.SBAUtil;
import pronze.lib.core.Core;
import pronze.lib.scoreboards.ScoreboardManager;

import java.util.*;

public class SBAHypixelify extends JavaPlugin implements SBAHypixelifyAPI {
    private static SBAHypixelify plugin;
    private final Map<String, Arena> arenas = new HashMap<>();
    private final List<Listener> registeredListeners = new ArrayList<>();
    private String version;
    private PlayerWrapperService playerWrapperService;
    private Configurator configurator;
    private GamesInventory gamesInventory;
    private boolean debug = false;
    private boolean protocolLib;
    private boolean isSnapshot;

    public static Optional<GameStorage> getStorage(Game game) {
        if (plugin.arenas.containsKey(game.getName()))
            return Optional.ofNullable(plugin.arenas.get(game.getName()).getStorage());

        return Optional.empty();
    }

    public static Arena getArena(String arenaName) {
        return plugin.arenas.get(arenaName);
    }

    public static void addArena(Arena arena) {
        plugin.arenas.put(arena.getGame().getName(), arena);
    }

    public static void removeArena(String arenaName) {
        plugin.arenas.remove(arenaName);
    }

    public static Configurator getConfigurator() {
        return plugin.configurator;
    }

    public static boolean isProtocolLib() {
        return plugin.protocolLib;
    }

    public static SBAHypixelify getInstance() {
        return plugin;
    }

    public static GamesInventory getGamesInventory() {
        return plugin.gamesInventory;
    }

    public static PlayerWrapperService getWrapperService() {
        return plugin.playerWrapperService;
    }

    public static boolean isUpgraded() {
        return !Objects.requireNonNull(getConfigurator()
                .config.getString("version")).contains(SBAHypixelify.getInstance().getVersion());
    }

    public static void showErrorMessage(String... messages) {
        Bukkit.getLogger().severe("======PLUGIN ERROR===========");
        Bukkit.getLogger().severe("Plugin: SBAHypixelify is being disabled for the following error:");
        Arrays.stream(messages)
                .filter(Objects::nonNull)
                .forEach(Bukkit.getLogger()::severe);
        Bukkit.getLogger().severe("=============================");
        Bukkit.getServer().getPluginManager().disablePlugin(plugin);
    }

    @Override
    public void onEnable() {
        plugin = this;
        version = this.getDescription().getVersion();
        isSnapshot = version.toLowerCase().contains("snapshot");
        protocolLib = plugin.getServer().getPluginManager().isPluginEnabled("ProtocolLib");

        if (getServer().getServicesManager().getRegistration(BedwarsAPI.class) == null) {
            showErrorMessage("Could not find Screaming-BedWars plugin!, make sure " +
                    "you have the right one installed, and it's enabled properly!");
            return;
        }

        if (!Main.getVersion().contains("0.3.") || !passedVersionChecks()) {
            showErrorMessage("You need at least a minimum of 0.3.0 snapshot 709+ version" +
                            " of Screaming-BedWars to run SBAHypixelify v2.0!",
                    "Get the latest version from here: https://ci.screamingsandals.org/job/BedWars-0.x.x/");
            return;
        }

        if (!ClassStorage.IS_SPIGOT_SERVER) {
            showErrorMessage("Did not detect spigot",
                    "Make sure you have spigot installed to run this plugin");
            return;
        }

        if (Main.getVersionNumber() < 109) {
            showErrorMessage("Minecraft server is running versions below 1.9, please upgrade!");
            return;
        }

        /* initialize our custom ScoreboardManager library*/
        ScoreboardManager.init(this);

        UpdateChecker.run(this);

        configurator = new Configurator(this);
        configurator.loadDefaults();

        new CommandManager().init(this);
        Logger.init(configurator.config.getBoolean("debug.enabled", false));
        I18n.load(this, configurator.config.getString("locale"));

        playerWrapperService = new PlayerWrapperService();
        debug = configurator.config.getBoolean("debug.enabled", false);

        CustomShop shop = new CustomShop();

        gamesInventory = new GamesInventory();
        gamesInventory.loadInventory();

        registerListener(new BedWarsListener());
        registerListener(new PlayerListener());
        registerListener(new TeamUpgradeListener());

        if (configurator.config.getBoolean("main-lobby.enabled", false))
            registerListener(new MainLobbyBoard());
        if (SBAHypixelify.getConfigurator().config.getBoolean("lobby-scoreboard.enabled", true))
            registerListener(new LobbyScoreboard());

        registerListener(shop);

        final var pluginManager = Bukkit.getServer().getPluginManager();

        try {
            if (pluginManager.isPluginEnabled("PlaceholderAPI")) {
                new SBAExpansion().register();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        preliminaryRotatingGeneratorChecks();
        new Metrics(this, 79505);
        Logger.trace("Registering API service provider");

        getServer().getServicesManager().register(
                SBAHypixelifyAPI.class,
                this,
                this,
                ServicePriority.Normal
        );
        getLogger().info("Plugin has loaded!");
    }

    private void preliminaryRotatingGeneratorChecks() {
        if (configurator.config.getBoolean("floating-generator.enabled", false)) {
            SBAUtil.destroySpawnerArmorStandEntities();
        }
    }

    private boolean passedVersionChecks() {
        try {
           if (Integer.parseInt(Main.getInstance().getBuildInfo()) >= 710) {
               return true;
           }
        } catch (Throwable ignored) {}
        return false;
    }

    public void registerListener(Listener listener) {
        final var plugMan = Bukkit.getServer().getPluginManager();
        plugMan.registerEvents(listener, this);
        Logger.trace("Registered Listener: {}", listener.getClass().getSimpleName());
        registeredListeners.add(listener);
    }

    @Override
    public void onDisable() {
        if (SBAHypixelify.isProtocolLib()) {
            Bukkit.getOnlinePlayers()
                    .stream()
                    .filter(Objects::nonNull)
                    .forEach(SBAUtil::removeScoreboardObjective);
        }

        registeredListeners.forEach(HandlerList::unregisterAll);
        registeredListeners.clear();

        RotatingGenerators.destroy(RotatingGenerators.cache);
        Logger.trace("Cancelling tasks...");
        this.getServer().getScheduler().cancelTasks(plugin);
        this.getServer().getServicesManager().unregisterAll(plugin);
        arenas.clear();
        Logger.trace("Successfully shutdown SBAHypixelify instance");
    }

    /*
    ======================
     * API implementations
     =====================
     */

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getObject(String key, T def) {
        try {
            return (T) getConfigurator().config.get(key, def);
        } catch (Throwable t) {
            return def;
        }
    }

    @Override
    public List<Listener> getRegisteredListeners() {
        return List.copyOf(registeredListeners);
    }

    @Override
    public String getVersion() {
        return plugin.version;
    }

    @Override
    public Optional<pronze.hypixelify.api.game.GameStorage> getGameStorage(Game game) {
        return SBAHypixelify.getStorage(game);
    }

    @Override
    public PlayerWrapper getPlayerWrapper(Player player) {
        return playerWrapperService.getWrapper(player);
    }

    @Override
    public boolean isDebug() {
        return debug;
    }

    @Override
    public boolean isSnapshot() {
        return isSnapshot;
    }
}


