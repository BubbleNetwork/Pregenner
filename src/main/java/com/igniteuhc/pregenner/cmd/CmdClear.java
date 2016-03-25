package com.igniteuhc.pregenner.cmd;

import com.igniteuhc.pregenner.BorderData;
import com.igniteuhc.pregenner.Config;
import java.util.List;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CmdClear
  extends WBCmd
{
  public CmdClear()
  {
    this.name = (this.permission = "clear");
    this.hasWorldNameInput = true;
    this.consoleRequiresWorldName = false;
    this.minParams = 0;
    this.maxParams = 1;
    
    addCmdExample(nameEmphasizedW() + "- remove border for this world.");
    addCmdExample(nameEmphasized() + "^all - remove border for all worlds.");
    this.helpText = "If run by an in-game player and [world] or \"all\" isn't specified, the world you are currently in is used.";
  }
  
  public void execute(CommandSender sender, Player player, List<String> params, String worldName)
  {
    if ((params.size() == 1) && (((String)params.get(0)).equalsIgnoreCase("all")))
    {
      if (worldName != null)
      {
        sendErrorAndHelp(sender, "You should not specify a world with \"clear all\".");
        return;
      }
      Config.removeAllBorders();
      return;
    }
    if (worldName == null)
    {
      if (player == null)
      {
        sendErrorAndHelp(sender, "You must specify a world name from console if not using \"clear all\".");
        return;
      }
      worldName = player.getWorld().getName();
    }
    BorderData border = Config.Border(worldName);
    if (border == null)
    {
      sendErrorAndHelp(sender, "This world (\"" + worldName + "\") does not have a border set.");
      return;
    }
    Config.removeBorder(worldName);
  }
}
