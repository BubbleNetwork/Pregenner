package com.igniteuhc.pregenner;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class Main
  extends JavaPlugin
{
  public static volatile Main plugin = null;
  public static volatile PLCommand wbCommand = null;
  
  public void onEnable()
  {
    if (plugin == null) {
      plugin = this;
    }
    if (wbCommand == null) {
      wbCommand = new PLCommand();
    }
    Config.load(this, false);
    getCommand("preload").setExecutor(wbCommand);
  }
  
  public void onDisable()
  {
    Config.StopBorderTimer();
    Config.StoreFillTask();
    Config.StopFillTask();
  }
  
  public BorderData getWorldBorder(String worldName)
  {
    return Config.Border(worldName);
  }
}
