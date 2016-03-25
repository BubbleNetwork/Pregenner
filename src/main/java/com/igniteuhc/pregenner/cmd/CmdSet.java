package com.igniteuhc.pregenner.cmd;

import com.igniteuhc.pregenner.Config;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CmdSet
  extends WBCmd
{
  public CmdSet()
  {
    this.name = (this.permission = "set");
    this.hasWorldNameInput = true;
    this.consoleRequiresWorldName = false;
    this.minParams = 1;
    this.maxParams = 4;
    
    addCmdExample(nameEmphasizedW() + "<radiusX> [radiusZ] <x> <z> - use x/z coords.");
    addCmdExample(nameEmphasizedW() + "<radiusX> [radiusZ] ^spawn - use spawn point.");
    addCmdExample(nameEmphasized() + "<radiusX> [radiusZ] - set border, centered on you.", true, false, true);
    addCmdExample(nameEmphasized() + "<radiusX> [radiusZ] ^player <name> - center on player.");
    this.helpText = "Set a border for a world, with several options for defining the center location. [world] is optional for players and defaults to the world the player is in. If [radiusZ] is not specified, the radiusX value will be used for both. The <x> and <z> coordinates can be decimal values (ex. 1.234).";
  }
  
  public void execute(CommandSender sender, Player player, List<String> params, String worldName)
  {
    if ((params.size() == 1) && (player == null))
    {
      sendErrorAndHelp(sender, "You have not provided a sufficient number of parameters.");
      return;
    }
    if (worldName != null)
    {
      if ((params.size() == 2) && (!((String)params.get(params.size() - 1)).equalsIgnoreCase("spawn")))
      {
        sendErrorAndHelp(sender, "You have not provided a sufficient number of arguments.");
        return;
      }
      World world = sender.getServer().getWorld(worldName);
      if (world == null) {
        if (((String)params.get(params.size() - 1)).equalsIgnoreCase("spawn")) {
          sendErrorAndHelp(sender, "The world you specified (\"" + worldName + "\") could not be found on the server, so the spawn point cannot be determined.");
        }
      }
    }
    else
    {
      if (player == null)
      {
        if (!((String)params.get(params.size() - 2)).equalsIgnoreCase("player"))
        {
          sendErrorAndHelp(sender, "You must specify a world name from console if not specifying a player name.");
          return;
        }
        player = Bukkit.getPlayer((String)params.get(params.size() - 1));
        if ((player == null) || (!player.isOnline()))
        {
          sendErrorAndHelp(sender, "The player you specified (\"" + (String)params.get(params.size() - 1) + "\") does not appear to be online.");
          return;
        }
      }
      worldName = player.getWorld().getName();
    }
    int radiusCount = params.size();
    try
    {
      double z;
      if (((String)params.get(params.size() - 1)).equalsIgnoreCase("spawn"))
      {
        Location loc = sender.getServer().getWorld(worldName).getSpawnLocation();
        double x = loc.getX();
        double z = loc.getZ();
        radiusCount--;
      }
      else if ((params.size() > 2) && (((String)params.get(params.size() - 2)).equalsIgnoreCase("player")))
      {
        Player playerT = Bukkit.getPlayer((String)params.get(params.size() - 1));
        if ((playerT == null) || (!playerT.isOnline()))
        {
          sendErrorAndHelp(sender, "The player you specified (\"" + (String)params.get(params.size() - 1) + "\") does not appear to be online.");
          return;
        }
        worldName = playerT.getWorld().getName();
        double x = playerT.getLocation().getX();
        double z = playerT.getLocation().getZ();
        radiusCount -= 2;
      }
      else if ((player == null) || (radiusCount > 2))
      {
        double x = Double.parseDouble((String)params.get(params.size() - 2));
        double z = Double.parseDouble((String)params.get(params.size() - 1));
        radiusCount -= 2;
      }
      else
      {
        double x = player.getLocation().getX();
        z = player.getLocation().getZ();
      }
      int radiusX = Integer.parseInt((String)params.get(0));
      int radiusZ;
      int radiusZ;
      if (radiusCount < 2) {
        radiusZ = radiusX;
      } else {
        radiusZ = Integer.parseInt((String)params.get(1));
      }
      if ((radiusX < Config.KnockBack()) || (radiusZ < Config.KnockBack()))
      {
        sendErrorAndHelp(sender, "Radius value(s) must be more than the knockback distance.");
        return;
      }
    }
    catch (NumberFormatException ex)
    {
      sendErrorAndHelp(sender, "Radius value(s) must be integers and x and z values must be numerical."); return;
    }
    double z;
    double x;
    int radiusZ;
    int radiusX;
    Config.setBorder(worldName, radiusX, radiusZ, x, z);
  }
}
