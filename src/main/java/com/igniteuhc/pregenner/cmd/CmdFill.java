package com.igniteuhc.pregenner.cmd;

import com.igniteuhc.pregenner.Config;
import com.igniteuhc.pregenner.CoordXZ;
import com.igniteuhc.pregenner.Main;
import com.igniteuhc.pregenner.WorldFillTask;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

public class CmdFill
  extends WBCmd
{
  public CmdFill()
  {
    this.name = (this.permission = "fill");
    this.hasWorldNameInput = true;
    this.consoleRequiresWorldName = false;
    this.minParams = 0;
    this.maxParams = 3;
    
    addCmdExample(nameEmphasizedW() + "[freq] [pad] [force] - fill world to border.");
  }
  
  public void execute(CommandSender sender, Player player, List<String> params, String worldName)
  {
    boolean confirm = false;
    if (params.size() >= 1)
    {
      String check = ((String)params.get(0)).toLowerCase();
      if ((check.equals("cancel")) || (check.equals("stop")))
      {
        if (!makeSureFillIsRunning(sender)) {
          return;
        }
        fillDefaults();
        Config.StopFillTask();
        return;
      }
      if (check.equals("pause"))
      {
        if (!makeSureFillIsRunning(sender)) {
          return;
        }
        Config.fillTask.pause();
        return;
      }
      confirm = check.equals("confirm");
    }
    if ((worldName == null) && (!confirm)) {
      if (player != null)
      {
        worldName = player.getWorld().getName();
      }
      else
      {
        sendErrorAndHelp(sender, "You must specify a world!");
        return;
      }
    }
    if ((Config.fillTask != null) && (Config.fillTask.valid())) {
      return;
    }
    try
    {
      if ((params.size() >= 1) && (!confirm)) {
        this.fillFrequency = Math.abs(Integer.parseInt((String)params.get(0)));
      }
      if ((params.size() >= 2) && (!confirm)) {
        this.fillPadding = Math.abs(Integer.parseInt((String)params.get(1)));
      }
    }
    catch (NumberFormatException ex)
    {
      sendErrorAndHelp(sender, "The frequency and padding values must be integers.");
      fillDefaults();
      return;
    }
    if (this.fillFrequency <= 0)
    {
      sendErrorAndHelp(sender, "The frequency value must be greater than zero.");
      fillDefaults();
      return;
    }
    if (params.size() == 3) {
      this.fillForceLoad = strAsBool((String)params.get(2));
    }
    if (worldName != null) {
      this.fillWorld = worldName;
    }
    if (confirm)
    {
      if (this.fillWorld.isEmpty())
      {
        sendErrorAndHelp(sender, "You must first use this command successfully without confirming.");
        return;
      }
      int ticks = 1;int repeats = 1;
      if (this.fillFrequency > 20) {
        repeats = this.fillFrequency / 20;
      } else {
        ticks = 20 / this.fillFrequency;
      }
      Config.fillTask = new WorldFillTask(Bukkit.getServer(), player, this.fillWorld, this.fillPadding, repeats, ticks, this.fillForceLoad);
      if (Config.fillTask.valid())
      {
        int task = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(Main.plugin, Config.fillTask, ticks, ticks);
        Config.fillTask.setTaskID(task);
      }
      fillDefaults();
    }
    else if (this.fillWorld.isEmpty())
    {
      sendErrorAndHelp(sender, "You must first specify a valid world.");
      return;
    }
  }
  
  private final int defaultPadding = CoordXZ.chunkToBlock(13);
  private String fillWorld = "";
  private int fillFrequency = 20;
  private int fillPadding = this.defaultPadding;
  private boolean fillForceLoad = false;
  
  private void fillDefaults()
  {
    this.fillWorld = "";
    this.fillFrequency = 20;
    this.fillPadding = this.defaultPadding;
    this.fillForceLoad = false;
  }
  
  private boolean makeSureFillIsRunning(CommandSender sender)
  {
    if ((Config.fillTask != null) && (Config.fillTask.valid())) {
      return true;
    }
    sendErrorAndHelp(sender, "The world map generation task is not currently running.");
    return false;
  }
}
