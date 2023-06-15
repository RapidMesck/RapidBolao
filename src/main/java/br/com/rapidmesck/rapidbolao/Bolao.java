package br.com.rapidmesck.rapidbolao;

import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class Bolao extends JavaPlugin implements CommandExecutor {
    private Economy econ;
    private Timer timer;
    private Map<UUID, Bet> bets = new HashMap<>();
    private boolean bolaoIsActive = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!setupEconomy() ) {
            this.getLogger().severe("Disabled due to no Vault dependency found!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.getCommand("bolao").setExecutor(this);
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            return false;
        }

        if (!(sender instanceof Player)) {
            return false;
        }

        Player player = (Player) sender;

        if (args[0].equalsIgnoreCase("start") && player.isOp()) {
            if (bolaoIsActive) {
                player.sendMessage(getConfig().getString("messages.bolaoActive"));
                return true;
            }

            if (args.length < 2) {
                return false;
            }

            int timeInMinutes;
            try {
                timeInMinutes = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(getConfig().getString("messages.invalidTime"));
                return true;
            }

            bolaoIsActive = true;
            bets.clear();
            String messageStart = String.format(getConfig().getString("messages.bolaoStarted"), timeInMinutes);
            player.sendMessage(messageStart);
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    bolaoIsActive = false;
                    if (bets.isEmpty()) {
                        Bukkit.broadcastMessage(getConfig().getString("messages.noBets"));
                    } else {
                        UUID winner = bets.get(new Random().nextInt(bets.size())).getPlayerId();
                        double totalBet = bets.values().stream().mapToDouble(Bet::getAmount).sum();
                        econ.depositPlayer(Bukkit.getOfflinePlayer(winner), totalBet);
                        String messageWinner = String.format(getConfig().getString("messages.bolaoFinished"), Bukkit.getOfflinePlayer(winner).getName(), totalBet);
                        Bukkit.broadcastMessage(messageWinner);
                    }
                }
            }, timeInMinutes * 60 * 1000);

            return true;
        }

        if (!bolaoIsActive) {
            player.sendMessage(getConfig().getString("messages.noActiveBolao"));
            return true;
        }

        if (bets.containsKey(player.getUniqueId())) {
            player.sendMessage(getConfig().getString("messages.alreadyPlacedBet"));
            return true;
        }

        double betAmount = 1000.0;  // Aposta fixa de 1000

        if (econ.getBalance(player) < betAmount) {
            player.sendMessage(getConfig().getString("messages.insufficientFunds"));
            return true;
        }

        econ.withdrawPlayer(player, betAmount);
        bets.put(player.getUniqueId(), new Bet(player.getUniqueId(), betAmount));

        String messageBetPlaced = String.format(getConfig().getString("messages.betPlaced"), betAmount);
        player.sendMessage(messageBetPlaced);
        return true;
    }

    @Override
    public void onDisable() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
