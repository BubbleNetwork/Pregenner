package com.igniteuhc.pregenner;

import com.igniteuhc.pregenner.cmd.CmdClear;
import com.igniteuhc.pregenner.cmd.CmdCommands;
import com.igniteuhc.pregenner.cmd.CmdFill;
import com.igniteuhc.pregenner.cmd.CmdHelp;
import com.igniteuhc.pregenner.cmd.CmdSet;
import com.igniteuhc.pregenner.cmd.WBCmd;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PLCommand
  implements CommandExecutor
{
  public Map<String, WBCmd> subCommands = new LinkedHashMap();
  private Set<String> subCommandsWithWorldNames = new LinkedHashSet();
  
  public PLCommand()
  {
    addCmd(new CmdHelp());
    addCmd(new CmdSet());
    addCmd(new CmdClear());
    addCmd(new CmdCommands());
    addCmd(new CmdFill());
  }
  
  private void addCmd(WBCmd cmd)
  {
    this.subCommands.put(cmd.name, cmd);
    if (cmd.hasWorldNameInput) {
      this.subCommandsWithWorldNames.add(cmd.name);
    }
  }
  
  public boolean onCommand(CommandSender sender, Command command, String label, String[] split)
  {
    Player player = (sender instanceof Player) ? (Player)sender : null;
    
    List<String> params = concatenateQuotedWorldName(split);
    
    String worldName = null;
    if ((this.wasWorldQuotation) || (
      (params.size() > 1) && 
      (!this.subCommands.containsKey(params.get(0))) && 
      (this.subCommandsWithWorldNames.contains(params.get(1))))) {
      worldName = (String)params.get(0);
    }
    if (params.isEmpty()) {
      params.add(0, "commands");
    }
    String cmdName = worldName == null ? ((String)params.get(0)).toLowerCase() : 
      ((String)params.get(1)).toLowerCase();
    
    params.remove(0);
    if (worldName != null) {
      params.remove(0);
    }
    if (!this.subCommands.containsKey(cmdName))
    {
      int page = player == null ? 0 : 1;
      try
      {
        page = Integer.parseInt(cmdName);
      }
      catch (NumberFormatException ignored)
      {
        sender.sendMessage(WBCmd.C_ERR + 
          "Command not recognized. Showing command list.");
      }
      cmdName = "commands";
      params.add(0, Integer.toString(page));
    }
    WBCmd subCommand = (WBCmd)this.subCommands.get(cmdName);
    if (!sender.hasPermission("worldborder.pregen")) {
      return true;
    }
    if ((player == null) && (subCommand.hasWorldNameInput) && 
      (subCommand.consoleRequiresWorldName) && (worldName == null))
    {
      sender.sendMessage(WBCmd.C_ERR + 
        "This command requires a world to be specified if run by the console.");
      subCommand.sendCmdHelp(sender);
      return true;
    }
    if ((params.size() < subCommand.minParams) || 
      (params.size() > subCommand.maxParams))
    {
      if (subCommand.maxParams == 0) {
        sender.sendMessage(WBCmd.C_ERR + 
          "This command does not accept any parameters.");
      } else {
        sender.sendMessage(WBCmd.C_ERR + 
          "You have not provided a valid number of parameters.");
      }
      subCommand.sendCmdHelp(sender);
      return true;
    }
    subCommand.execute(sender, player, params, worldName);
    
    return true;
  }
  
  private boolean wasWorldQuotation = false;
  
  private List<String> concatenateQuotedWorldName(String[] split)
  {
    this.wasWorldQuotation = false;
    List<String> args = new ArrayList(Arrays.asList(split));
    
    int startIndex = -1;
    for (int i = 0; i < args.size(); i++) {
      if (((String)args.get(i)).startsWith("\""))
      {
        startIndex = i;
        break;
      }
    }
    if (startIndex == -1) {
      return args;
    }
    if (((String)args.get(startIndex)).endsWith("\""))
    {
      args.set(
        startIndex, 
        ((String)args.get(startIndex)).substring(1, 
        ((String)args.get(startIndex)).length() - 1));
      if (startIndex == 0) {
        this.wasWorldQuotation = true;
      }
    }
    else
    {
      List<String> concat = new ArrayList(args);
      Iterator<String> concatI = concat.iterator();
      for (int i = 1; i < startIndex + 1; i++) {
        concatI.next();
      }
      StringBuilder quote = new StringBuilder((String)concatI.next());
      while (concatI.hasNext())
      {
        String next = (String)concatI.next();
        concatI.remove();
        quote.append(" ");
        quote.append(next);
        if (next.endsWith("\""))
        {
          concat.set(startIndex, 
            quote.substring(1, quote.length() - 1));
          args = concat;
          if (startIndex != 0) {
            break;
          }
          this.wasWorldQuotation = true;
          break;
        }
      }
    }
    return args;
  }
  
  public Set<String> getCommandNames()
  {
    Set<String> commands = new TreeSet(this.subCommands.keySet());
    
    commands.remove("commands");
    return commands;
  }
}
