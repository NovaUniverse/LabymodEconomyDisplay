package net.novauniverse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;

import net.labymod.serverapi.api.LabyAPI;
import net.labymod.serverapi.api.serverinteraction.economy.EconomyBalanceType;
import net.milkbowl.vault.economy.Economy;

public class LabymodEconomyDisplay extends JavaPlugin implements Listener {
	private List<UUID> labymodUsers;
	private HashMap<UUID, Double> balanceCache;
	private Economy economy;

	@Override
	public void onEnable() {
		saveDefaultConfig();

		labymodUsers = new ArrayList<>();
		balanceCache = new HashMap<>();

		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			getLogger().warning("No economy provider found. Shutting down");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
		economy = rsp.getProvider();

		PluginMessageListener lmcListener = new PluginMessageListener() {
			@Override
			public void onPluginMessageReceived(String channel, Player player, byte[] message) {
				if (!labymodUsers.contains(player.getUniqueId())) {
					labymodUsers.add(player.getUniqueId());
				}
			}
		};

		Bukkit.getServer().getMessenger().registerIncomingPluginChannel(this, "labymod3:main", lmcListener);
		Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(this, "abymod3:main");

		Bukkit.getPluginManager().registerEvents(this, this);

		new BukkitRunnable() {

			@Override
			public void run() {
				for (Player player : Bukkit.getServer().getOnlinePlayers()) {
					if (labymodUsers.contains(player.getUniqueId())) {
						double balance = economy.getBalance(player);
						boolean sendBalance = false;

						if (!balanceCache.containsKey(player.getUniqueId())) {
							sendBalance = true;
						} else if (balanceCache.get(player.getUniqueId()) != balance) {
							sendBalance = true;
						}

						if (sendBalance) {
							LabyAPI.getService().getEconomyDisplayTransmitter().transmit(player.getUniqueId(), EconomyBalanceType.CASH, true, (int) Math.floor(balance));
							//System.out.println("transmitting balance to " + player.getUniqueId());
							balanceCache.put(player.getUniqueId(), balance);
						}
					}
				}
			}
		}.runTaskTimer(this, 20L, 20L);
	}

	@Override
	public void onDisable() {
		labymodUsers.clear();
		balanceCache.clear();
		HandlerList.unregisterAll((Plugin) this);
		Bukkit.getScheduler().cancelTasks((Plugin) this);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerQuit(PlayerQuitEvent e) {
		labymodUsers.remove(e.getPlayer().getUniqueId());
		balanceCache.remove(e.getPlayer().getUniqueId());
	}
}