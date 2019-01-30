package com.bgsoftware.superiorskyblock.hooks;

import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class EconomyHook {

    private static Economy econ;

    static{
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            econ = null;
        }else {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                econ = null;
            }else {
                econ = rsp.getProvider();
            }
        }
    }

    public static double getMoneyInBank(SuperiorPlayer superiorPlayer){
        return getMoneyInBank(superiorPlayer.asPlayer());
    }

    public static double getMoneyInBank(Player player){
        if(!econ.hasAccount(player))
            econ.createPlayerAccount(player);

        return econ.getBalance(player);
    }

    public static void depositMoney(Player player, double amount){
        econ.depositPlayer(player, amount);
    }

    public static void withdrawMoney(SuperiorPlayer superiorPlayer, double amount){
        withdrawMoney(superiorPlayer.asPlayer(), amount);
    }

    public static void withdrawMoney(Player player, double amount){
        econ.withdrawPlayer(player, amount);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isVaultEnabled(){
        return econ != null;
    }

}
