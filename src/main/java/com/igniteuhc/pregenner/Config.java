package com.igniteuhc.pregenner;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

public class Config
{
  private static Main plugin;
  private static FileConfiguration cfg = null;
  private static Logger wbLog = null;
  public static volatile DecimalFormat coord = new DecimalFormat("#.#");
  private static int borderTask = -1;
  public static volatile WorldFillTask fillTask = null;
  private static Runtime rt = Runtime.getRuntime();
  private static boolean shapeRound = true;
  private static Map<String, BorderData> borders = Collections.synchronizedMap(new LinkedHashMap());
  private static Set<UUID> bypassPlayers = Collections.synchronizedSet(new LinkedHashSet());
  private static String message;
  private static String messageFmt;
  private static String messageClean;
  private static boolean DEBUG = false;
  private static double knockBack = 3.0D;
  private static int timerTicks = 4;
  private static boolean whooshEffect = true;
  private static boolean portalRedirection = true;
  private static boolean dynmapEnable = true;
  private static String dynmapMessage;
  private static int remountDelayTicks = 0;
  private static boolean killPlayer = false;
  private static boolean denyEnderpearl = false;
  private static int fillAutosaveFrequency = 30;
  private static int fillMemoryTolerance = 500;
  private static boolean preventBlockPlace = false;
  private static boolean preventMobSpawn = false;
  private static final int currentCfgVersion = 11;
  
  public static long Now()
  {
    return System.currentTimeMillis();
  }
  
  public static void setBorder(String world, BorderData border, boolean logIt)
  {
    borders.put(world, border);
    save(true);
  }
  
  public static void setBorder(String world, BorderData border)
  {
    setBorder(world, border, true);
  }
  
  public static void setBorder(String world, int radiusX, int radiusZ, double x, double z, Boolean shapeRound)
  {
    BorderData old = Border(world);
    boolean oldWrap = (old != null) && (old.getWrapping());
    setBorder(world, new BorderData(x, z, radiusX, radiusZ, shapeRound, 
      oldWrap), true);
  }
  
  public static void setBorder(String world, int radiusX, int radiusZ, double x, double z)
  {
    BorderData old = Border(world);
    Boolean oldShape = old == null ? null : old.getShape();
    boolean oldWrap = (old != null) && (old.getWrapping());
    setBorder(world, new BorderData(x, z, radiusX, radiusZ, oldShape, 
      oldWrap), true);
  }
  
  public static void setBorder(String world, int radius, double x, double z, Boolean shapeRound)
  {
    setBorder(world, new BorderData(x, z, radius, radius, shapeRound), true);
  }
  
  public static void setBorder(String world, int radius, double x, double z)
  {
    setBorder(world, radius, radius, x, z);
  }
  
  public static void setBorderCorners(String world, double x1, double z1, double x2, double z2, Boolean shapeRound, boolean wrap)
  {
    double radiusX = Math.abs(x1 - x2) / 2.0D;
    double radiusZ = Math.abs(z1 - z2) / 2.0D;
    double x = (x1 < x2 ? x1 : x2) + radiusX;
    double z = (z1 < z2 ? z1 : z2) + radiusZ;
    setBorder(world, new BorderData(x, z, (int)Math.round(radiusX), 
      (int)Math.round(radiusZ), shapeRound, wrap), true);
  }
  
  public static void setBorderCorners(String world, double x1, double z1, double x2, double z2, Boolean shapeRound)
  {
    setBorderCorners(world, x1, z1, x2, z2, shapeRound, false);
  }
  
  public static void setBorderCorners(String world, double x1, double z1, double x2, double z2)
  {
    BorderData old = Border(world);
    Boolean oldShape = old == null ? null : old.getShape();
    boolean oldWrap = (old != null) && (old.getWrapping());
    setBorderCorners(world, x1, z1, x2, z2, oldShape, oldWrap);
  }
  
  public static void removeBorder(String world)
  {
    borders.remove(world);
    save(true);
  }
  
  public static void removeAllBorders()
  {
    borders.clear();
    save(true);
  }
  
  public static String BorderDescription(String world)
  {
    BorderData border = (BorderData)borders.get(world);
    if (border == null) {
      return "No border was found for the world \"" + world + "\".";
    }
    return "World \"" + world + "\" has border " + border.toString();
  }
  
  public static Set<String> BorderDescriptions()
  {
    Set<String> output = new HashSet();
    for (String worldName : borders.keySet()) {
      output.add(BorderDescription(worldName));
    }
    return output;
  }
  
  public static BorderData Border(String world)
  {
    return (BorderData)borders.get(world);
  }
  
  public static Map<String, BorderData> getBorders()
  {
    return new LinkedHashMap(borders);
  }
  
  public static void setMessage(String msg)
  {
    updateMessage(msg);
    save(true);
  }
  
  public static void updateMessage(String msg)
  {
    message = msg;
    messageFmt = replaceAmpColors(msg);
    messageClean = stripAmpColors(msg);
  }
  
  public static String Message()
  {
    return messageFmt;
  }
  
  public static String MessageRaw()
  {
    return message;
  }
  
  public static String MessageClean()
  {
    return messageClean;
  }
  
  public static void setShape(boolean round)
  {
    shapeRound = round;
    log("Set default border shape to " + ShapeName() + ".");
    save(true);
  }
  
  public static boolean ShapeRound()
  {
    return shapeRound;
  }
  
  public static String ShapeName()
  {
    return ShapeName(Boolean.valueOf(shapeRound));
  }
  
  public static String ShapeName(Boolean round)
  {
    if (round == null) {
      return "default";
    }
    return round.booleanValue() ? "elliptic/round" : "rectangular/square";
  }
  
  public static void setDebug(boolean debugMode)
  {
    DEBUG = debugMode;
    log("Debug mode " + (DEBUG ? "enabled" : "disabled") + ".");
    save(true);
  }
  
  public static boolean Debug()
  {
    return DEBUG;
  }
  
  public static void setWhooshEffect(boolean enable)
  {
    whooshEffect = enable;
    log("\"Whoosh\" knockback effect " + (enable ? "enabled" : "disabled") + 
      ".");
    save(true);
  }
  
  public static boolean whooshEffect()
  {
    return whooshEffect;
  }
  
  public static void showWhooshEffect(Location loc)
  {
    if (!whooshEffect()) {
      return;
    }
    World world = loc.getWorld();
    world.playEffect(loc, Effect.ENDER_SIGNAL, 0);
    world.playEffect(loc, Effect.ENDER_SIGNAL, 0);
    world.playEffect(loc, Effect.SMOKE, 4);
    world.playEffect(loc, Effect.SMOKE, 4);
    world.playEffect(loc, Effect.SMOKE, 4);
    world.playEffect(loc, Effect.GHAST_SHOOT, 0);
  }
  
  public static boolean getIfPlayerKill()
  {
    return killPlayer;
  }
  
  public static boolean getDenyEnderpearl()
  {
    return denyEnderpearl;
  }
  
  public static void setDenyEnderpearl(boolean enable)
  {
    denyEnderpearl = enable;
    log("Direct cancellation of ender pearls thrown past the border " + (
      enable ? "enabled" : "disabled") + ".");
    save(true);
  }
  
  public static void setPortalRedirection(boolean enable)
  {
    portalRedirection = enable;
    log("Portal redirection " + (enable ? "enabled" : "disabled") + ".");
    save(true);
  }
  
  public static boolean portalRedirection()
  {
    return portalRedirection;
  }
  
  public static void setKnockBack(double numBlocks)
  {
    knockBack = numBlocks;
    log("Knockback set to " + knockBack + " blocks inside the border.");
    save(true);
  }
  
  public static double KnockBack()
  {
    return knockBack;
  }
  
  public static int TimerTicks()
  {
    return timerTicks;
  }
  
  public static void setRemountTicks(int ticks)
  {
    remountDelayTicks = ticks;
    if (remountDelayTicks == 0)
    {
      log("Remount delay set to 0. Players will be left dismounted when knocked back from the border while on a vehicle.");
    }
    else
    {
      log(
      
        "Remount delay set to " + remountDelayTicks + " tick(s). That is roughly " + remountDelayTicks * 50 + "ms / " + remountDelayTicks * 50.0D / 1000.0D + " seconds.");
      if (ticks < 10) {
        logWarn("setting the remount delay to less than 10 (and greater than 0) is not recommended. This can lead to nasty client glitches.");
      }
    }
    save(true);
  }
  
  public static int RemountTicks()
  {
    return remountDelayTicks;
  }
  
  public static void setFillAutosaveFrequency(int seconds)
  {
    fillAutosaveFrequency = seconds;
    if (fillAutosaveFrequency == 0) {
      log("World autosave frequency during Fill process set to 0, disabling it. Note that much progress can be lost this way if there is a bug or crash in the world generation process from Bukkit or any world generation plugin you use.");
    } else {
      log(
      
        "World autosave frequency during Fill process set to " + fillAutosaveFrequency + " seconds (rounded to a multiple of 5). New chunks generated by the Fill process will be forcibly saved to disk this often to prevent loss of progress due to bugs or crashes in the world generation process.");
    }
    save(true);
  }
  
  public static int FillAutosaveFrequency()
  {
    return fillAutosaveFrequency;
  }
  
  public static void setDynmapBorderEnabled(boolean enable)
  {
    dynmapEnable = enable;
    log("DynMap border display is now " + (enable ? "enabled" : "disabled") + 
      ".");
    save(true);
  }
  
  public static boolean DynmapBorderEnabled()
  {
    return dynmapEnable;
  }
  
  public static void setDynmapMessage(String msg)
  {
    dynmapMessage = msg;
    log("DynMap border label is now set to: " + msg);
    save(true);
  }
  
  public static String DynmapMessage()
  {
    return dynmapMessage;
  }
  
  public static void setPlayerBypass(UUID player, boolean bypass)
  {
    if (bypass) {
      bypassPlayers.add(player);
    } else {
      bypassPlayers.remove(player);
    }
    save(true);
  }
  
  public static boolean isPlayerBypassing(UUID player)
  {
    return bypassPlayers.contains(player);
  }
  
  public static ArrayList<UUID> getPlayerBypassList()
  {
    return new ArrayList(bypassPlayers);
  }
  
  private static void importBypassStringList(List<String> strings)
  {
    for (String string : strings) {
      bypassPlayers.add(UUID.fromString(string));
    }
  }
  
  private static ArrayList<String> exportBypassStringList()
  {
    ArrayList<String> strings = new ArrayList();
    for (UUID uuid : bypassPlayers) {
      strings.add(uuid.toString());
    }
    return strings;
  }
  
  public static boolean isBorderTimerRunning()
  {
    if (borderTask == -1) {
      return false;
    }
    return (plugin.getServer().getScheduler().isQueued(borderTask)) || 
      (plugin.getServer().getScheduler().isCurrentlyRunning(borderTask));
  }
  
  public static void StopBorderTimer()
  {
    if (borderTask == -1) {
      return;
    }
    plugin.getServer().getScheduler().cancelTask(borderTask);
    borderTask = -1;
    logConfig("Border-checking timed task stopped.");
  }
  
  public static void StopFillTask()
  {
    if ((fillTask != null) && (fillTask.valid())) {
      fillTask.cancel();
    }
  }
  
  public static void StoreFillTask()
  {
    save(false, true);
  }
  
  public static void UnStoreFillTask()
  {
    save(false);
  }
  
  public static void RestoreFillTask(String world, int fillDistance, int chunksPerRun, int tickFrequency, int x, int z, int length, int total, boolean forceLoad)
  {
    fillTask = new WorldFillTask(plugin.getServer(), null, world, 
      fillDistance, chunksPerRun, tickFrequency, forceLoad);
    if (fillTask.valid())
    {
      fillTask.continueProgress(x, z, length, total);
      int task = plugin
        .getServer()
        .getScheduler()
        .scheduleSyncRepeatingTask(plugin, fillTask, 20L, 
        tickFrequency);
      fillTask.setTaskID(task);
    }
  }
  
  public static void RestoreFillTask(String world, int fillDistance, int chunksPerRun, int tickFrequency, int x, int z, int length, int total)
  {
    RestoreFillTask(world, fillDistance, chunksPerRun, tickFrequency, x, z, 
      length, total, false);
  }
  
  public static int AvailableMemory()
  {
    return (int)((rt.maxMemory() - rt.totalMemory() + rt.freeMemory()) / 1048576L);
  }
  
  public static boolean AvailableMemoryTooLow()
  {
    return AvailableMemory() < fillMemoryTolerance;
  }
  
  public static boolean HasPermission(Player player, String request)
  {
    return HasPermission(player, request, true);
  }
  
  public static boolean HasPermission(Player player, String request, boolean notify)
  {
    if (player == null) {
      return true;
    }
    if (player.hasPermission("worldborder." + request)) {
      return true;
    }
    return false;
  }
  
  public static String replaceAmpColors(String message)
  {
    return ChatColor.translateAlternateColorCodes('&', message);
  }
  
  public static String stripAmpColors(String message)
  {
    return message.replaceAll("(?i)&([a-fk-or0-9])", "");
  }
  
  public static void log(Level lvl, String text)
  {
    wbLog.log(lvl, text);
  }
  
  public static void log(String text)
  {
    log(Level.INFO, text);
  }
  
  public static void logWarn(String text)
  {
    log(Level.WARNING, text);
  }
  
  public static void logConfig(String text)
  {
    log(Level.INFO, "[CONFIG] " + text);
  }
  
  public static void load(Main master, boolean logIt)
  {
    plugin = master;
    wbLog = plugin.getLogger();
    
    plugin.reloadConfig();
    cfg = plugin.getConfig();
    
    int cfgVersion = cfg.getInt("cfg-version", 11);
    
    String msg = cfg.getString("message");
    shapeRound = cfg.getBoolean("round-border", true);
    DEBUG = cfg.getBoolean("debug-mode", false);
    whooshEffect = cfg.getBoolean("whoosh-effect", true);
    portalRedirection = cfg.getBoolean("portal-redirection", true);
    knockBack = cfg.getDouble("knock-back-dist", 3.0D);
    timerTicks = cfg.getInt("timer-delay-ticks", 5);
    remountDelayTicks = cfg.getInt("remount-delay-ticks", 0);
    dynmapEnable = cfg.getBoolean("dynmap-border-enabled", true);
    dynmapMessage = cfg.getString("dynmap-border-message", 
      "The border of the world.");
    logConfig("Using " + ShapeName() + " border, knockback of " + 
      knockBack + " blocks, and timer delay of " + timerTicks + ".");
    killPlayer = cfg.getBoolean("player-killed-bad-spawn", false);
    denyEnderpearl = cfg.getBoolean("deny-enderpearl", true);
    fillAutosaveFrequency = cfg.getInt("fill-autosave-frequency", 30);
    importBypassStringList(cfg.getStringList("bypass-list-uuids"));
    fillMemoryTolerance = cfg.getInt("fill-memory-tolerance", 500);
    preventBlockPlace = cfg.getBoolean("prevent-block-place");
    preventMobSpawn = cfg.getBoolean("prevent-mob-spawn");
    
    borders.clear();
    if ((msg == null) || (msg.isEmpty()))
    {
      logConfig("Configuration not present, creating new file.");
      msg = "&cYou have reached the edge of this world.";
      updateMessage(msg);
      save(false);
      return;
    }
    if ((cfgVersion < 8) && (!msg.substring(0, 1).equals("&"))) {
      updateMessage("&c" + msg);
    } else {
      updateMessage(msg);
    }
    if (cfgVersion < 10) {
      denyEnderpearl = true;
    }
    if (cfgVersion < 11) {
      cfg.set("bypass-list", null);
    }
    ConfigurationSection worlds = cfg.getConfigurationSection("worlds");
    if (worlds != null)
    {
      Set<String> worldNames = worlds.getKeys(false);
      for (String worldName : worldNames)
      {
        ConfigurationSection bord = worlds
          .getConfigurationSection(worldName);
        if (cfgVersion > 3) {
          worldName = worldName.replace("<", ".");
        }
        if ((bord.isSet("radius")) && (!bord.isSet("radiusX")))
        {
          int radius = bord.getInt("radius");
          bord.set("radiusX", Integer.valueOf(radius));
          bord.set("radiusZ", Integer.valueOf(radius));
        }
        Boolean overrideShape = (Boolean)bord.get("shape-round");
        boolean wrap = bord.getBoolean("wrapping", false);
        BorderData border = new BorderData(bord.getDouble("x", 0.0D), 
          bord.getDouble("z", 0.0D), bord.getInt("radiusX", 0), 
          bord.getInt("radiusZ", 0), overrideShape, wrap);
        borders.put(worldName, border);
        logConfig(BorderDescription(worldName));
      }
    }
    ConfigurationSection storedFillTask = cfg
      .getConfigurationSection("fillTask");
    if (storedFillTask != null)
    {
      String worldName = storedFillTask.getString("world");
      int fillDistance = storedFillTask.getInt("fillDistance", 176);
      int chunksPerRun = storedFillTask.getInt("chunksPerRun", 5);
      int tickFrequency = storedFillTask.getInt("tickFrequency", 20);
      int fillX = storedFillTask.getInt("x", 0);
      int fillZ = storedFillTask.getInt("z", 0);
      int fillLength = storedFillTask.getInt("length", 0);
      int fillTotal = storedFillTask.getInt("total", 0);
      boolean forceLoad = storedFillTask.getBoolean("forceLoad", false);
      RestoreFillTask(worldName, fillDistance, chunksPerRun, 
        tickFrequency, fillX, fillZ, fillLength, fillTotal, 
        forceLoad);
      save(false);
    }
    if (logIt) {
      logConfig("Configuration loaded.");
    }
    if (cfgVersion < 11) {
      save(false);
    }
  }
  
  public static void save(boolean logIt)
  {
    save(logIt, false);
  }
  
  public static void save(boolean logIt, boolean storeFillTask)
  {
    if (cfg == null) {
      return;
    }
    cfg.set("cfg-version", Integer.valueOf(11));
    cfg.set("message", message);
    cfg.set("round-border", Boolean.valueOf(shapeRound));
    cfg.set("debug-mode", Boolean.valueOf(DEBUG));
    cfg.set("whoosh-effect", Boolean.valueOf(whooshEffect));
    cfg.set("portal-redirection", Boolean.valueOf(portalRedirection));
    cfg.set("knock-back-dist", Double.valueOf(knockBack));
    cfg.set("timer-delay-ticks", Integer.valueOf(timerTicks));
    cfg.set("remount-delay-ticks", Integer.valueOf(remountDelayTicks));
    cfg.set("dynmap-border-enabled", Boolean.valueOf(dynmapEnable));
    cfg.set("dynmap-border-message", dynmapMessage);
    cfg.set("player-killed-bad-spawn", Boolean.valueOf(killPlayer));
    cfg.set("deny-enderpearl", Boolean.valueOf(denyEnderpearl));
    cfg.set("fill-autosave-frequency", Integer.valueOf(fillAutosaveFrequency));
    cfg.set("bypass-list-uuids", exportBypassStringList());
    cfg.set("fill-memory-tolerance", Integer.valueOf(fillMemoryTolerance));
    cfg.set("prevent-block-place", Boolean.valueOf(preventBlockPlace));
    cfg.set("prevent-mob-spawn", Boolean.valueOf(preventMobSpawn));
    
    cfg.set("worlds", null);
    
    Iterator localIterator = borders.entrySet().iterator();
    while (localIterator.hasNext())
    {
      Entry<String, BorderData> stringBorderDataEntry = (Entry)localIterator.next();
      Entry<String, BorderData> wdata = stringBorderDataEntry;
      String name = ((String)wdata.getKey()).replace(".", "<");
      BorderData bord = (BorderData)wdata.getValue();
      
      cfg.set("worlds." + name + ".x", Double.valueOf(bord.getX()));
      cfg.set("worlds." + name + ".z", Double.valueOf(bord.getZ()));
      cfg.set("worlds." + name + ".radiusX", Integer.valueOf(bord.getRadiusX()));
      cfg.set("worlds." + name + ".radiusZ", Integer.valueOf(bord.getRadiusZ()));
      cfg.set("worlds." + name + ".wrapping", Boolean.valueOf(bord.getWrapping()));
      if (bord.getShape() != null) {
        cfg.set("worlds." + name + ".shape-round", bord.getShape());
      }
    }
    if ((storeFillTask) && (fillTask != null) && (fillTask.valid()))
    {
      cfg.set("fillTask.world", fillTask.refWorld());
      cfg.set("fillTask.fillDistance", Integer.valueOf(fillTask.refFillDistance()));
      cfg.set("fillTask.chunksPerRun", Integer.valueOf(fillTask.refChunksPerRun()));
      cfg.set("fillTask.tickFrequency", Integer.valueOf(fillTask.refTickFrequency()));
      cfg.set("fillTask.x", Integer.valueOf(fillTask.refX()));
      cfg.set("fillTask.z", Integer.valueOf(fillTask.refZ()));
      cfg.set("fillTask.length", Integer.valueOf(fillTask.refLength()));
      cfg.set("fillTask.total", Integer.valueOf(fillTask.refTotal()));
      cfg.set("fillTask.forceLoad", Boolean.valueOf(fillTask.refForceLoad()));
    }
    else
    {
      cfg.set("fillTask", null);
    }
    plugin.saveConfig();
  }
}
