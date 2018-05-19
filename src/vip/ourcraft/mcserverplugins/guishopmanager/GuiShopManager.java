package vip.ourcraft.mcserverplugins.guishopmanager;

import net.minecraft.server.v1_12_R1.Block;
import org.black_ixx.bossshop.BossShop;
import org.black_ixx.bossshop.api.BossShopAPI;
import org.black_ixx.bossshop.core.BSShop;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import vip.creeper.mcserverplugins.creeperkits.CreeperKits;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by July on 2018/05/19.
 */
public class GuiShopManager extends JavaPlugin implements Listener {
    private BossShopAPI bsAPI;
    private HashMap<ItemStack, BSShop> guiItems;

    public void onEnable() {
        if (!hookBossShop()) {
            getLogger().info("BossShop hook 失败!");
            setEnabled(false);
            return;
        }

        this.guiItems = new HashMap<>();

        loadConfig();
        getCommand("gsm").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    public boolean onCommand(CommandSender cs, Command cmd, String lable, String[] args) {
        if (cs.hasPermission("guishopmanager.admin") && args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            loadConfig();
            cs.sendMessage("ok.");

            return true;
        }

        return false;
    }

    private void loadConfig() {
        saveDefaultConfig();
        reloadConfig();
        guiItems.clear();

        FileConfiguration config = getConfig();

        for (String itemName : config.getConfigurationSection("gui_items").getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection("gui_items." + itemName);

            guiItems.put(CreeperKits.getInstance().getKitManager().getKit(section.getString("kit_name")).getItems().get(section.getInt("kit_index")), bsAPI.getShop(section.getString("shop_name")));
        }

        getLogger().info("载入了 " + guiItems.size() + " 个 item.");
    }

    private boolean hookBossShop() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("BossShop");

        if (plugin != null) {
            bsAPI = ((BossShop) plugin).getAPI();
            return true;
        }

        return false;
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerInventory playerInventory = player.getInventory();
        Set<ItemStack> needGiveItems = new HashSet<>();

        needGiveItems.addAll(guiItems.keySet());

        for (ItemStack guiItem : guiItems.keySet()) {
            for (ItemStack invItem : playerInventory.getContents()) {
                if (equalsMeta(guiItem, invItem)) {
                    needGiveItems.remove(guiItem);
                    break;
                }
            }
        }

        for (ItemStack item : needGiveItems) {
            if (playerInventory.firstEmpty() == -1) {
                player.sendMessage("§a[GSM] §c给予菜单失败: 背包空间不足, 请在清空背包后重新进入服务器!");
                break;
            }

            playerInventory.addItem(item);
        }
    }

    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
            Player player = event.getPlayer();
            ItemStack handItem = player.getInventory().getItemInMainHand();
            org.bukkit.block.Block block = event.getClickedBlock();

            if (block == null) {
                for (ItemStack item : guiItems.keySet()) {
                    if (equalsMeta(handItem, item)) {
                        player.closeInventory();
                        guiItems.get(item).openInventory(player);
                        return;
                    }
                }
            }
    }

    @EventHandler
    public void onPlayerDropItemEvent(PlayerDropItemEvent event) {
        if (guiItems.containsKey(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    private boolean equalsMeta(ItemStack item1, ItemStack item2) {
        return !(item1 == null || item2 == null || item1.getType() == Material.AIR || item2.getType() == Material.AIR) && item1.getItemMeta().equals(item2.getItemMeta());

    }
}
