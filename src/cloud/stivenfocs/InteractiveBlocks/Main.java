package cloud.stivenfocs.InteractiveBlocks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

public class Main extends JavaPlugin implements Listener {

    private static Field bukkitCommandMap = null;

    static {
        try {
            bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
        } catch (NoSuchFieldException noSuchFieldException) {
            noSuchFieldException.printStackTrace();
        }
    }

    //////////////////////////////////////

    File dataFile = new File(getDataFolder() + "/" + "data.yml");
    FileConfiguration dataConfig = YamlConfiguration.loadConfiguration(dataFile);

    File pdataFile = new File(getDataFolder() + "/" + "pdata.yml");
    FileConfiguration pdataConfig = YamlConfiguration.loadConfiguration(pdataFile);

    String prefix = "";
    String configuration_reloaded = "";
    String block_registered = "";
    String block_unregistered = "";
    String an_error_occurred = "";
    String insufficient_permissions = "";
    String only_players = "";
    String incomplete_command = "";
    String unknow_subcommand = "";
    String block_already_registered = "";
    String block_already_unregistered = "";
    String id_busy = "";
    List<String> help_admin = new ArrayList<>();
    List<String> help_user = new ArrayList<>();
    
    public ItemStack getWand() {
        ItemStack i = new ItemStack(Material.MAGMA_CREAM);
        return i;
    }

    public void onEnable() {
        try {
            getCommand("interactiveblocks").setExecutor(this);
            Bukkit.getPluginManager().registerEvents(this, this);
            reloadVars();
        } catch (Exception ex) {
            getLogger().severe("An error occurred while trying to enable the plugin");
            ex.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        } finally {
            for (String puid : pdataConfig.getKeys(false)) {
                if (pdataConfig.getStringList(puid + ".delay") != null) {
                    for(String block : pdataConfig.getStringList(puid + ".delay")) {
                        executeDelay(puid, block, dataConfig.getInt("blocks." + block + ".type.delay"));
                    }
                }
            }
        }
    }
    
    public boolean reloadVars() {
        try {
            reloadConfig();

            getConfig().options().header("Plugin developed by StivenFocs with LOV");
            getConfig().options().copyDefaults(true);

            getConfig().addDefault("messages.prefix", "");
            getConfig().addDefault("messages.configuration_reloaded", "&aConfiguration file reloaded");
            getConfig().addDefault("messages.block_registered", "&aBlock added");
            getConfig().addDefault("messages.block_unregistered", "&eBlock removed");
            getConfig().addDefault("messages.an_error_occurred", "&cAn error occurred during this task, please contact an admin.");
            getConfig().addDefault("messages.insufficient_permissions", "&cYou do not have enough permissions.");
            getConfig().addDefault("messages.only_players", "&cOnly a player can execute this command.");
            getConfig().addDefault("messages.incomplete_command", "&cIncomplete command, some argument is missing.");
            getConfig().addDefault("messages.unknow_subcommand", "&cUnknow subcommand");
            getConfig().addDefault("messages.block_already_registered", "&cThis block has been already registered.");
            List<String> new_help_admin = new ArrayList<>();
            new_help_admin.add("&8&m*=======================*");
            new_help_admin.add("&bInteractive&3Blocks &7%version%");
            new_help_admin.add("");
            new_help_admin.add("&7* /iblocks reload");
            new_help_admin.add("&7* /iblocks getwand");
            new_help_admin.add("");
            new_help_admin.add("&8&m*=======================*");
            getConfig().addDefault("messages.help_admin", new_help_admin);
            List<String> new_help_user = new ArrayList<>();
            new_help_user.add("&8&m*=======================*");
            new_help_user.add("&bInteractive&3Blocks");
            new_help_user.add("");
            new_help_user.add("&7* Made by StivenFocs");
            new_help_user.add("&7* Free on SpigotMC");
            new_help_user.add("");
            new_help_user.add("&8&m*=======================*");
            getConfig().addDefault("messages.help_user", new_help_user);

            saveConfig();
            reloadConfig();
            reloadDataConfiguration();
            reloadpDataConfiguration();

            dataConfig.options().copyDefaults(true);

            if (dataConfig.get("blocks") == null) {
                dataConfig.set("blocks", new Object());
                String[] str = dataConfig.getString("blocks").split(" ");
                dataConfig.set("blocks",str[1]);
            }

            saveDataConfiguration();
            reloadDataConfiguration();

            prefix = getConfig().getString("messages.prefix", "");
            configuration_reloaded = getConfig().getString("messages.configuration_reloaded", "&aConfiguration file reloaded");
            block_registered = getConfig().getString("messages.block_registered", "&aBlock added");
            block_unregistered = getConfig().getString("messages.block_unregistered", "&eBlock removed");
            an_error_occurred = getConfig().getString("messages.an_error_occurred", "&cAn error occurred during this task, please contact an admin.");
            insufficient_permissions = getConfig().getString("messages.insufficient_permissions", "&cYou do not have enough permissions.");
            only_players = getConfig().getString("messages.only_players", "&cOnly a player can execute this command.");
            incomplete_command = getConfig().getString("messages.incomplete_command", "&cIncomplete command, some argument is missing.");
            unknow_subcommand = getConfig().getString("messages.unknow_subcommand", "&cUnknow subcommand");
            block_already_registered = getConfig().getString("messages.block_already_registered", "&cThis block has been already registered.");
            block_already_unregistered = getConfig().getString("messages.block_already_unregistered", "&cThis block is already unregistered");
            id_busy = getConfig().getString("messages.id_busy", "&cThis block id has been already registered, please choose another name.");
            help_admin = getConfig().getStringList("messages.help_admin");
            help_user = getConfig().getStringList("messages.help_user");

            getLogger().info("Configuration reloaded successfully.");

            return true;
        } catch (Exception ex) {
            getLogger().severe("An error occurred while trying to reload the whole configuration");
            ex.printStackTrace();
            return false;
        }
    }

    public void reloadDataConfiguration() {
        try {
            if (!dataFile.exists()) {
                dataFile.createNewFile();
            }
        } catch (Exception ex) {
            getLogger().severe("An error occurred while trying to get and/or create the data configuration file");
            ex.printStackTrace();
            return;
        }

        try {
            dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        } catch (Exception ex) {
            getLogger().severe("An error occurred while trying to reload the data configuration file");
            ex.printStackTrace();
        }
    }

    public void saveDataConfiguration() {
        try {
            if (!dataFile.exists()) {
                dataFile.createNewFile();
            }
        } catch (Exception ex) {
            getLogger().severe("An error occurred while trying to get and/or create the data configuration file");
            ex.printStackTrace();
            return;
        }

        try {
            dataConfig.save(dataFile);
        } catch (Exception ex) {
            getLogger().severe("An error occurred while trying to save into the data configuration file");
            ex.printStackTrace();
        }
    }

    public void reloadpDataConfiguration() {
        try {
            if (!pdataFile.exists()) {
                pdataFile.createNewFile();
            }
        } catch (Exception ex) {
            getLogger().severe("An error occurred while trying to get and/or create the pdata configuration file");
            ex.printStackTrace();
            return;
        }

        try {
            pdataConfig = YamlConfiguration.loadConfiguration(pdataFile);
        } catch (Exception ex) {
            getLogger().severe("An error occurred while trying to reload the pdata configuration file");
            ex.printStackTrace();
        }
    }

    public void savepDataConfiguration() {
        try {
            if (!pdataFile.exists()) {
                pdataFile.createNewFile();
            }
        } catch (Exception ex) {
            getLogger().severe("An error occurred while trying to get and/or create the pdata configuration file");
            ex.printStackTrace();
            return;
        }

        try {
            pdataConfig.save(pdataFile);
        } catch (Exception ex) {
            getLogger().severe("An error occurred while trying to save into the data configuration file");
            ex.printStackTrace();
        }
    }

    /////////////////////////////////////////

    public static boolean isdigit(String string) {
        int intValue;

        if(string == null || string.equals("")) {
            return false;
        }

        try {
            intValue = Integer.parseInt(string);
            return true;
        } catch (NumberFormatException e) {}
        return false;
    }

    public void sendString(String text, CommandSender sender) {
        try {
            if (getConfig().getString(text) != null) {
                if (getConfig().getString(text).length() > 0) {
                    text = getConfig().getString(text);
                    if (prefix.length() > 0) {
                        text = prefix + text;
                    }
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', text.replaceAll("%version%", getDescription().getVersion())));
                }
            } else {
                if (text.length() > 0) {
                    if (prefix.length() > 0) {
                        text = prefix + text;
                    }
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', text.replaceAll("%version%", getDescription().getVersion())));
                }
            }
        } catch (Exception ex) {
            getLogger().info("An error occurred while trying to send a message");
            ex.printStackTrace();
        }
    }

    public List<String> colorlist(List<String> uncoloredList) {
        List<String> coloredList = new ArrayList<>();
        for(String line : uncoloredList) {
            coloredList.add(ChatColor.translateAlternateColorCodes('&', line.replaceAll("%version%", getDescription().getVersion())));
        }
        return coloredList;
    }

    public boolean isPermittedAdmin(CommandSender sender) {
        if (sender.hasPermission("interactiveblocks.admin")) {
            return true;
        }
        if (sender.hasPermission("interactiveblocks.*")) {
            return true;
        }
        return false;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (sender.hasPermission("interactiveblocks.admin")) {
                sender.sendMessage(colorlist(help_admin).toArray(new String[0]));
            } else {
                sender.sendMessage(colorlist(help_user).toArray(new String[0]));
            }
        } else {
            if (args[0].equalsIgnoreCase("reload")) {
                if (isPermittedAdmin(sender)) {
                    if (reloadVars()) {
                        sendString(configuration_reloaded, sender);
                    } else {
                        sendString(an_error_occurred, sender);
                    }
                } else {
                    sendString(insufficient_permissions, sender);
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("getwand")) {
                if (isPermittedAdmin(sender)) {
                    if (sender instanceof Player) {
                        Player p = (Player) sender;
                        int emptySlots = 0;
                        for (ItemStack item : p.getInventory().getContents()) {
                            if (item == null || item.isSimilar(getWand())) {
                                emptySlots++;
                            }
                        }

                        if (emptySlots > 0) {
                            p.getInventory().addItem(getWand());
                        }
                    } else {
                        sendString(only_players, sender);
                    }
                } else {
                    sendString(insufficient_permissions, sender);
                }
                return true;
            }
            /*if (args[0].equalsIgnoreCase("addblock")) {
                if (isPermittedAdmin(sender)) {
                    if (sender instanceof Player) {
                        Player p = (Player) sender;
                        if (args.length > 1) {
                            if (dataConfig.get("blocks." + args[1]) == null) {
                                print(p.getTargetBlock(new HashSet<Material>(), 0));
                                if (p.getTargetBlock(new HashSet<Material>(), 4) != null) {
                                    Location blockLoc = p.getTargetBlock(new HashSet<Material>(), 4).getLocation();

                                    if (!doesBlockExists(blockLoc)) {
                                        reloadDataConfiguration();

                                        String path = "blocks." + args[1];
                                        dataConfig.set(path + ".world", blockLoc.getWorld().getName());
                                        dataConfig.set(path + ".x", blockLoc.getBlockX());
                                        dataConfig.set(path + ".y", blockLoc.getBlockY());
                                        dataConfig.set(path + ".z", blockLoc.getBlockZ());
                                        dataConfig.set(path + ".commands", new ArrayList<>());
                                        dataConfig.set(path + ".permission", "interactiveblocks." + args[1] + ".interact");

                                        saveDataConfiguration();
                                        reloadDataConfiguration();

                                        sendString(block_registered, sender);
                                    } else {
                                        sendString(block_already_registered, sender);
                                    }
                                }
                            } else {
                                sendString(id_busy, sender);
                            }
                        } else {
                            sendString(incomplete_command, sender);
                        }
                    } else {
                        sendString(only_players, sender);
                    }
                } else {
                    sendString(insufficient_permissions, sender);
                }
                return true;
            }*/
            sendString(unknow_subcommand, sender);
        }
        return false;
    }

    /////////////////////////////////////////

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            Location blockLoc = event.getClickedBlock().getLocation();

            if (isPermittedAdmin(p)) {
                if (p.getItemInHand().isSimilar(getWand())) {
                    event.setCancelled(true);

                    String new_block_id = "";

                    for(int i = 0; new_block_id == ""; i++) {
                        if (dataConfig.get("blocks." + i) == null) {
                            new_block_id = String.valueOf(i);
                            break;
                        }
                    }

                    //Location blockLoc = p.getTargetBlock(new HashSet<Material>(), 4).getLocation();

                    if (doesBlockExists(blockLoc) == null) {
                        //reloadDataConfiguration(); - possibile fonte di errori

                        String path = "blocks." + new_block_id;
                        dataConfig.set(path + ".world", blockLoc.getWorld().getName());
                        dataConfig.set(path + ".x", blockLoc.getBlockX());
                        dataConfig.set(path + ".y", blockLoc.getBlockY());
                        dataConfig.set(path + ".z", blockLoc.getBlockZ());
                        dataConfig.set(path + ".commands", new ArrayList<>());
                        dataConfig.set(path + ".permission", "interactiveblocks." + new_block_id + ".interact");
                        dataConfig.set(path + ".type.name", "INFINITE");

                        saveDataConfiguration();
                        reloadDataConfiguration();

                        sendString(block_registered, p);
                    } else {
                        sendString(block_already_registered, p);
                    }
                    return;
                }
            }

            for(String block : dataConfig.getConfigurationSection("blocks").getKeys(false)) {
                String path = "blocks." + block;
                Boolean Continue = true;

                if (dataConfig.get(path + ".world") == null) Continue = false;
                if (dataConfig.get(path + ".x") == null) Continue = false;
                if (dataConfig.get(path + ".y") == null) Continue = false;
                if (dataConfig.get(path + ".z") == null) Continue = false;
                if (dataConfig.getStringList(path + ".commands") == null) Continue = false;
                if (dataConfig.get(path + ".permission") != null) {
                    if (dataConfig.get(path + ".permission-message") == null) {
                        dataConfig.set(path + ".permission-message", "&cYou aren't permitted to interact with this block.");

                        saveDataConfiguration();
                        reloadDataConfiguration();
                    }
                }

                String type = "";
                String answer = "";
                Integer delay = 0;
                String cmd = "";
                Boolean ignore_perms = false;

                if (dataConfig.getString(path + ".type.name") == null) {
                    dataConfig.set(path + ".type.name", "INFINITE");

                    saveDataConfiguration();
                    reloadDataConfiguration();
                }
                if (dataConfig.getString(path + ".type.name").equalsIgnoreCase("INFINITE")) {
                    if (dataConfig.get(path + ".type.answer") != null) {
                        dataConfig.set(path + ".type.answer", null);
                        saveDataConfiguration();
                        reloadDataConfiguration();
                    }
                    if (dataConfig.get(path + ".type.delay") != null) {
                        dataConfig.set(path + ".type.delay", null);
                        saveDataConfiguration();
                        reloadDataConfiguration();
                    }
                    if (dataConfig.get(path + ".type.command") != null) {
                        dataConfig.set(path + ".type.command", null);
                        saveDataConfiguration();
                        reloadDataConfiguration();
                    }
                    if (dataConfig.get(path + ".type.ignore_perms") != null) {
                        dataConfig.set(path + ".type.ignore_perms", null);
                        saveDataConfiguration();
                        reloadDataConfiguration();
                    }
                } else if (dataConfig.getString(path + ".type.name").equalsIgnoreCase("SINGLE_USE")) {
                    if (dataConfig.get(path + ".type.answer") == null) {
                        dataConfig.set(path + ".type.answer","&cYou can use this one time only.");
                        saveDataConfiguration();
                        reloadDataConfiguration();
                    }
                    answer = dataConfig.getString(path + ".type.answer");

                    if (dataConfig.get(path + ".type.delay") != null) {
                        dataConfig.set(path + ".type.delay", null);
                        saveDataConfiguration();
                        reloadDataConfiguration();
                    }
                    if (dataConfig.get(path + ".type.ignore_perms") != null) {
                        dataConfig.set(path + ".type.ignore_perms", null);
                        saveDataConfiguration();
                        reloadDataConfiguration();
                    }
                } else if (dataConfig.getString(path + ".type.name").equalsIgnoreCase("DELAY")) {
                    if (dataConfig.getString(path + ".type.answer") == null) {
                        dataConfig.set(path + ".type.answer","&cYou have to wait before using this again.");
                        saveDataConfiguration();
                        reloadDataConfiguration();
                    }
                    answer = dataConfig.getString(path + ".type.answer");

                    if (dataConfig.get(path + ".type.delay") == null) {
                        dataConfig.set(path + ".type.delay", 30);
                        saveDataConfiguration();
                        reloadDataConfiguration();
                    }
                    delay = dataConfig.getInt(path + ".type.delay");
                    if (dataConfig.get(path + ".type.ignore_perms") != null) {
                        dataConfig.set(path + ".type.ignore_perms", null);
                        saveDataConfiguration();
                        reloadDataConfiguration();
                    }
                } else if (dataConfig.getString(path + ".type.name").equalsIgnoreCase("SUDO")) {
                    if (dataConfig.get(path + ".type.command") == null) {
                        dataConfig.set(path + ".type.command", "");
                        saveDataConfiguration();
                        reloadDataConfiguration();
                    }
                    cmd = dataConfig.getString(path + ".type.command");
                    if (dataConfig.get(path + ".type.ignore_perms") == null) {
                        dataConfig.set(path + ".type.ignore_perms", false);
                        saveDataConfiguration();
                        reloadDataConfiguration();
                    }
                    ignore_perms = dataConfig.getBoolean(path +".type.ignore_perms");


                    if (dataConfig.get(path + ".type.answer") != null) {
                        dataConfig.set(path + ".type.answer", null);
                        saveDataConfiguration();
                        reloadDataConfiguration();
                    }
                    if (dataConfig.get(path + ".type.delay") != null) {
                        dataConfig.set(path + ".type.delay", null);
                        saveDataConfiguration();
                        reloadDataConfiguration();
                    }
                } else {
                    dataConfig.set(path + ".type.name", "INFINITE");
                    saveDataConfiguration();
                    reloadDataConfiguration();
                }

                type = dataConfig.getString(path + ".type.name");

                ////////////////////////////////////

                Boolean hasPermission = false;
                if (dataConfig.getString(path + ".permission") != null) {
                    if (p.hasPermission(dataConfig.getString(path + ".permission"))) {
                        hasPermission = true;
                    }
                    if (isPermittedAdmin(p)) {
                        hasPermission = true;
                    }
                } else {
                    hasPermission = true;
                }
                Boolean used = false;

                ////////////////////////////////////

                if (!type.equalsIgnoreCase("INFINITE")) {
                    addDefaultPlayer(p.getUniqueId());

                    if (type.equalsIgnoreCase("SINGLE_USE")) {
                        if (pdataConfig.getStringList(p.getUniqueId().toString() + ".used").contains(block)) {
                            used = true;
                        }
                    } else if (type.equalsIgnoreCase("DELAY")) {
                        if (pdataConfig.getStringList(p.getUniqueId().toString() + ".delay").contains(block)) {
                            used = true;
                        }
                    }
                }

                if (Continue) {
                    Location blockLoc2 = new Location(Bukkit.getWorld(dataConfig.getString(path + ".world")), dataConfig.getDouble(path + ".x"), dataConfig.getDouble(path + ".y"), dataConfig.getDouble(path + ".z"));

                    if (blockLoc.equals(blockLoc2)) {
                        event.setCancelled(true);

                        if (hasPermission) {
                            if (!used) {
                                try {
                                    for (String command : dataConfig.getStringList(path + ".commands")) {
                                        if (command.startsWith("tell:")) {
                                            sendString(command.replaceAll("tell:", "").replaceAll("%player%", p.getName()), p);
                                        } else {
                                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replaceAll("%player%", p.getName()));
                                        }
                                    }
                                } catch (Exception ex) {
                                    getLogger().severe("An error occurred while executing commands for " + p.getName() + " in block: " + block);
                                    ex.printStackTrace();
                                }

                                if (type.equalsIgnoreCase("SINGLE_USE")) {
                                    addDefaultPlayer(p.getUniqueId());

                                    if (!pdataConfig.getStringList(p.getUniqueId().toString() + ".used").contains(block)) {
                                        List<String> p_used = pdataConfig.getStringList(p.getUniqueId().toString() + ".used");
                                        p_used.add(block);
                                        pdataConfig.set(p.getUniqueId().toString() + ".used", p_used);

                                        savepDataConfiguration();
                                        reloadpDataConfiguration();
                                    }
                                } else if (type.equalsIgnoreCase("DELAY")) {
                                    addDefaultPlayer(p.getUniqueId());

                                    if (!pdataConfig.getStringList(p.getUniqueId().toString() + ".delay").contains(block)) {
                                        List<String> p_delay = pdataConfig.getStringList(p.getUniqueId().toString() + ".delay");
                                        p_delay.add(block);
                                        pdataConfig.set(p.getUniqueId().toString() + ".delay", p_delay);
                                        executeDelay(p.getUniqueId().toString(), block, delay);

                                        savepDataConfiguration();
                                        reloadpDataConfiguration();
                                    }
                                } else if (type.equalsIgnoreCase("SUDO")) {
                                    Boolean op = p.isOp();
                                    if (ignore_perms) {
                                        p.setOp(true);
                                    }
                                    try {
                                        if (isBukkitCommand(cmd)) {
                                            p.performCommand(cmd);
                                        } else {
                                            //Bukkit.getPluginManager().callEvent((Event) new PlayerCommandPreprocessEvent(p, "/" + cmd));
                                            p.chat("/" + cmd);
                                        }
                                    } catch (Exception exception) {
                                        exception.printStackTrace();
                                    }
                                    if (ignore_perms) {
                                        p.setOp(op);
                                    }
                                }
                            } else {
                                sendString(answer.replaceAll("%player%", p.getName()), p);
                            }
                        } else {
                            sendString(dataConfig.getString(path + ".permission-message", ""), p);
                        }
                    }
                } else {
                    getLogger().warning("The InteractiveBlock with id: '" + block + "' does have unvalid configuration.");
                }
            }
        } else if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            Location blockLoc = event.getClickedBlock().getLocation();

            if (isPermittedAdmin(p)) {
                if (p.getItemInHand().isSimilar(getWand())) {
                    event.setCancelled(true);

                    String new_block_id = "";

                    for(int i = 0; new_block_id == ""; i++) {
                        if (dataConfig.get("blocks." + i) == null) {
                            new_block_id = String.valueOf(i);
                            break;
                        }
                    }

                    //Location blockLoc = p.getTargetBlock(new HashSet<Material>(), 4).getLocation();

                    if (doesBlockExists(blockLoc) != null) {
                        //reloadDataConfiguration(); - possibile fonte di errori

                        dataConfig.set("blocks." + doesBlockExists(blockLoc), null);

                        saveDataConfiguration();
                        reloadDataConfiguration();

                        sendString(block_unregistered, p);
                    } else {
                        sendString(block_already_unregistered, p);
                    }
                    return;
                }
            }

        }
    }

    /////////////////////////////////////////

    public String doesBlockExists(Location blockLoc) {
        for(String block : dataConfig.getConfigurationSection("blocks").getKeys(false)) {
            String path = "blocks." + block;
            Boolean Continue = true;

            if (dataConfig.get(path + ".world") == null) Continue = false;
            if (dataConfig.get(path + ".x") == null) Continue = false;
            if (dataConfig.get(path + ".y") == null) Continue = false;
            if (dataConfig.get(path + ".z") == null) Continue = false;
            if (dataConfig.getStringList(path + ".commands") == null) Continue = false;

            if (Continue) {
                Location blockLoc2 = new Location(Bukkit.getWorld(dataConfig.getString(path + ".world")), dataConfig.getDouble(path + ".x"), dataConfig.getDouble(path + ".y"), dataConfig.getDouble(path + ".z"));
                if (blockLoc.equals(blockLoc2)) {
                    return block;
                }
            } else {
                getLogger().warning("The InteractiveBlock with id: '" + block + "' does have unvalid configuration.");
            }
        }
        return null;
    }

    public HashMap<String, Integer> delayTasks = new HashMap<>();
    public HashMap<String, Boolean> inStartup = new HashMap<>();
    public void executeDelay(String puid, String block, Integer seconds) {
        String taskName = puid + block;
        inStartup.put(taskName, true);
        delayTasks.put(taskName,Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                if (!inStartup.containsKey(taskName)) {
                    if (removeDelay(puid, block)) {
                        int meId = delayTasks.get(taskName);
                        delayTasks.remove(taskName);
                        Bukkit.getScheduler().cancelTask(meId);
                    }
                } else {
                    inStartup.remove(taskName);
                }
            }
        }, 0L, 20L * seconds).getTaskId());
    }

    public boolean removeDelay(String puid, String block) {
        try {
            reloadpDataConfiguration();
            List<String> user_delay = pdataConfig.getStringList(puid + ".delay");
            List<String> new_user_delay = new ArrayList<>();
            for (String delayName : user_delay) {
                    if (!delayName.equals(block)) {
                    new_user_delay.add(delayName);
                }
            }
            pdataConfig.set(puid + ".delay", new_user_delay);

            savepDataConfiguration();
            reloadpDataConfiguration();
            return true;
        } catch (Exception ex) {
            getLogger().severe("An error occurred while trying to reset a player delay.");
            ex.printStackTrace();
            return false;
        }
    }

    public void addDefaultPlayer(UUID uuid) {
        reloadpDataConfiguration();

        String puid = uuid.toString();
        if (pdataConfig.get(puid) == null) {
            pdataConfig.set(puid + ".used", new ArrayList<>());
            pdataConfig.set(puid + ".delay", new ArrayList<>());

            savepDataConfiguration();
            reloadpDataConfiguration();
        }
    }

    private boolean isBukkitCommand(String paramString) {
        paramString = paramString.split(" ")[0];
        try {
            SimpleCommandMap simpleCommandMap = (SimpleCommandMap) bukkitCommandMap.get(Bukkit.getServer());
            for (Command command : simpleCommandMap.getCommands()) {
                if (command.getName().equalsIgnoreCase(paramString) || command.getAliases().contains(paramString))
                    return true;
            }
        } catch (IllegalAccessException ex) {
            getLogger().severe("An exception occurred while trying to retrieve and use a commandMap");
            ex.printStackTrace();
        }
        return false;
    }

    public void print(Object obj) {
        System.out.println(obj);
    }

}
