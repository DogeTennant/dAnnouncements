package com.dogetennant.dannouncements;

import com.dogetennant.dannouncements.announcement.AnnouncementConfigLoader;
import com.dogetennant.dannouncements.command.CommandRegistry;
import com.dogetennant.dannouncements.command.DAnnouncementsCommand;
import com.dogetennant.dannouncements.command.subcommand.CreateSubCommand;
import com.dogetennant.dannouncements.command.subcommand.ForceSubCommand;
import com.dogetennant.dannouncements.command.subcommand.HelpSubCommand;
import com.dogetennant.dannouncements.command.subcommand.InfoSubCommand;
import com.dogetennant.dannouncements.command.subcommand.JoinSubCommand;
import com.dogetennant.dannouncements.command.subcommand.LineSubCommand;
import com.dogetennant.dannouncements.command.subcommand.ListSubCommand;
import com.dogetennant.dannouncements.command.subcommand.ReloadSubCommand;
import com.dogetennant.dannouncements.command.subcommand.RemoveSubCommand;
import com.dogetennant.dannouncements.command.subcommand.ToggleSubCommand;
import com.dogetennant.dannouncements.command.subcommand.TpSubCommand;
import com.dogetennant.dannouncements.config.ConfigManager;
import com.dogetennant.dannouncements.dispatch.AnnouncementDispatcher;
import com.dogetennant.dannouncements.integration.PlaceholderHook;
import com.dogetennant.dannouncements.listener.JoinAnnouncementListener;
import com.dogetennant.dannouncements.schedule.AnnouncementScheduler;
import com.dogetennant.dannouncements.util.LogUtil;
import org.bukkit.plugin.java.JavaPlugin;

public class DAnnouncements extends JavaPlugin {

    private static DAnnouncements instance;

    private ConfigManager configManager;
    private AnnouncementConfigLoader announcementConfigLoader;
    private AnnouncementDispatcher dispatcher;
    private AnnouncementScheduler scheduler;
    private CommandRegistry commandRegistry;

    @Override
    public void onEnable() {
        instance = this;
        LogUtil.init(this);

        configManager = new ConfigManager(this);
        configManager.load();

        PlaceholderHook.init();

        announcementConfigLoader = new AnnouncementConfigLoader(this);
        announcementConfigLoader.load();

        dispatcher = new AnnouncementDispatcher(this);

        scheduler = new AnnouncementScheduler(this, announcementConfigLoader, dispatcher, configManager.get());
        scheduler.load();

        getServer().getPluginManager().registerEvents(
                new JoinAnnouncementListener(this, announcementConfigLoader, dispatcher), this);

        commandRegistry = new CommandRegistry();
        commandRegistry.register(new HelpSubCommand(commandRegistry));
        commandRegistry.register(new ReloadSubCommand());
        commandRegistry.register(new ListSubCommand());
        commandRegistry.register(new InfoSubCommand());
        commandRegistry.register(new CreateSubCommand());
        commandRegistry.register(new RemoveSubCommand());
        commandRegistry.register(new ToggleSubCommand());
        commandRegistry.register(new ForceSubCommand());
        commandRegistry.register(new LineSubCommand());
        commandRegistry.register(new JoinSubCommand());
        commandRegistry.register(new TpSubCommand());

        DAnnouncementsCommand commandHandler = new DAnnouncementsCommand(commandRegistry);
        var cmd = getCommand("dannouncements");
        if (cmd != null) {
            cmd.setExecutor(commandHandler);
            cmd.setTabCompleter(commandHandler);
        }

        LogUtil.info("dAnnouncements v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (scheduler != null) scheduler.shutdown();
        LogUtil.info("dAnnouncements disabled.");
    }

    public static DAnnouncements getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public AnnouncementConfigLoader getAnnouncementConfigLoader() { return announcementConfigLoader; }
    public AnnouncementDispatcher getDispatcher() { return dispatcher; }
    public AnnouncementScheduler getScheduler() { return scheduler; }
    public CommandRegistry getCommandRegistry() { return commandRegistry; }
}
