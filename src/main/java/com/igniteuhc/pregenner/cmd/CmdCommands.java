package com.igniteuhc.pregenner.cmd;

import com.igniteuhc.pregenner.Main;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;

public class CmdCommands
  extends WBCmd
{
  private static int pageSize = 8;
  
  public CmdCommands()
  {
    this.name = "commands";
    this.permission = "help";
    this.hasWorldNameInput = false;
  }
  
  public void execute(CommandSender sender, Player player, List<String> params, String worldName)
  {
    int page = player == null ? 0 : 1;
    if (!params.isEmpty()) {
      try
      {
        page = Integer.parseInt((String)params.get(0));
      }
      catch (NumberFormatException localNumberFormatException) {}
    }
    List<String> examples = player == null ? cmdExamplesConsole : 
      cmdExamplesPlayer;
    int pageCount = (int)Math.ceil(examples.size() / pageSize);
    if ((page < 0) || (page > pageCount)) {
      page = player == null ? 0 : 1;
    }
    sender.sendMessage(C_HEAD + 
      Main.plugin.getDescription().getFullName() + 
      "  -  key: " + commandEmphasized("command") + C_REQ + 
      "<required> " + C_OPT + "[optional]");
    int count;
    if (page > 0)
    {
      int first = (page - 1) * pageSize;
      count = Math.min(pageSize, examples.size() - first);
      for (int i = first; i < first + count; i++) {
        sender.sendMessage((String)examples.get(i));
      }
      String footer = C_HEAD + " (Page " + page + "/" + pageCount + 
        ")              " + cmd(sender);
      if (page < pageCount) {
        sender.sendMessage(footer + Integer.toString(page + 1) + C_DESC + 
          " - view next page of commands.");
      } else if (page > 1) {
        sender.sendMessage(footer + C_DESC + 
          "- view first page of commands.");
      }
    }
    else
    {
      for (String example : examples) {
        sender.sendMessage(example);
      }
    }
  }
}
