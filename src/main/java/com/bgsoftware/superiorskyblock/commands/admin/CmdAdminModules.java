package com.bgsoftware.superiorskyblock.commands.admin;

import com.bgsoftware.superiorskyblock.lang.Message;
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.modules.PluginModule;
import com.bgsoftware.superiorskyblock.commands.ISuperiorCommand;
import com.bgsoftware.superiorskyblock.module.BuiltinModules;
import com.bgsoftware.superiorskyblock.lang.PlayerLocales;
import com.bgsoftware.superiorskyblock.utils.debug.PluginDebugger;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class CmdAdminModules implements ISuperiorCommand {

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("modules");
    }

    @Override
    public String getPermission() {
        return "superior.admin.modules";
    }

    @Override
    public String getUsage(java.util.Locale locale) {
        return "admin modules [<" + Message.COMMAND_ARGUMENT_MODULE_NAME.getMessage(locale) + "> [load/unload]]";
    }

    @Override
    public String getDescription(java.util.Locale locale) {
        return Message.COMMAND_DESCRIPTION_ADMIN_MODULES.getMessage(locale);
    }

    @Override
    public int getMinArgs() {
        return 2;
    }

    @Override
    public int getMaxArgs() {
        return 4;
    }

    @Override
    public boolean canBeExecutedByConsole() {
        return true;
    }

    @Override
    public void execute(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {
        if (args.length == 2) {
            StringBuilder modulesList = new StringBuilder();
            java.util.Locale senderLocale = PlayerLocales.getLocale(sender);
            String moduleSeparator = Message.MODULES_LIST_SEPARATOR.getMessage(senderLocale);

            for (PluginModule pluginModule : plugin.getModules().getModules()) {
                modulesList.append(moduleSeparator).append(Message.MODULES_LIST_MODULE_NAME
                        .getMessage(senderLocale, pluginModule.getName(), pluginModule.getAuthor()));
            }

            Message.MODULES_LIST.send(sender, plugin.getModules().getModules().size(), modulesList.substring(moduleSeparator.length()));
        } else {
            PluginModule pluginModule = plugin.getModules().getModule(args[2]);

            if (args.length == 3) {
                if (pluginModule == null) {
                    Message.INVALID_MODULE.send(sender, args[2]);
                    return;
                }

                Message.MODULE_INFO.send(sender, pluginModule.getName(), pluginModule.getAuthor(), "");
            } else {
                String command = args[3].toLowerCase();

                switch (command) {
                    case "load":
                        pluginModule = BuiltinModules.getBuiltinModule(args[2]);

                        if (pluginModule == null) {
                            File moduleFile = new File(plugin.getDataFolder(), "modules/" + args[2] + ".jar");
                            try {
                                pluginModule = plugin.getModules().registerModule(moduleFile);
                                Message.MODULE_LOADED_SUCCESS.send(sender, pluginModule.getName());
                            } catch (Exception ex) {
                                Message.MODULE_LOADED_FAILURE.send(sender, args[2]);
                                ex.printStackTrace();
                                PluginDebugger.debug(ex);
                            }
                        } else {
                            if (pluginModule.isInitialized()) {
                                Message.MODULE_ALREADY_INITIALIZED.send(sender);
                                return;
                            }

                            plugin.getModules().registerModule(pluginModule);
                            Message.MODULE_LOADED_SUCCESS.send(sender, pluginModule.getName());
                        }

                        break;
                    case "unload":
                        if (pluginModule == null) {
                            Message.INVALID_MODULE.send(sender, args[2]);
                            return;
                        }

                        plugin.getModules().unregisterModule(pluginModule);

                        Message.MODULE_UNLOADED_SUCCESS.send(sender);
                        break;
                    default:
                        Message.COMMAND_USAGE.send(sender, plugin.getCommands().getLabel() + " " + getUsage(PlayerLocales.getLocale(sender)));
                        break;
                }
            }
        }

    }

    @Override
    public List<String> tabComplete(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {
        return args.length != 3 ? new ArrayList<>() : plugin.getModules().getModules().stream()
                .map(PluginModule::getName)
                .filter(name -> name.toLowerCase().startsWith(args[2]))
                .collect(Collectors.toList());
    }

}
