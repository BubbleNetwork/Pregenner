package com.igniteuhc.pregenner.cmd;

import com.igniteuhc.pregenner.Main;
import com.igniteuhc.pregenner.PLCommand;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CmdHelp
  extends WBCmd
{
  public CmdHelp()
  {
    this.name = (this.permission = "help");
    this.minParams = 0;
    this.maxParams = 10;
    
    addCmdExample(nameEmphasized() + "[command] - get help on command usage.");
  }
  
  public void cmdStatus(CommandSender sender)
  {
    String commands = Main.wbCommand.getCommandNames().toString().replace(", ", C_DESC + ", " + C_CMD);
    sender.sendMessage(C_HEAD + "Commands: " + C_CMD + commands.substring(1, commands.length() - 1));
    sender.sendMessage("Example, for info on \"set\" command: " + cmd(sender) + nameEmphasized() + C_CMD + "set");
    sender.sendMessage(C_HEAD + "For a full command example list, simply run the root " + cmd(sender) + C_HEAD + "command by itself with nothing specified.");
  }
  
  public void execute(CommandSender sender, Player player, List<String> params, String worldName)
  {
    if (params.isEmpty())
    {
      sendCmdHelp(sender);
      return;
    }
    Set<String> commands = Main.wbCommand.getCommandNames();
    for (String param : params) {
      if (commands.contains(param.toLowerCase()))
      {
        ((WBCmd)Main.wbCommand.subCommands.get(param.toLowerCase())).sendCmdHelp(sender);
        return;
      }
    }
    sendErrorAndHelp(sender, "No command recognized.");
  }
}
