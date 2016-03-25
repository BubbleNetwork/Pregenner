package com.igniteuhc.pregenner.cmd;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class WBCmd
{
  public String name = "";
  public String permission = null;
  public boolean hasWorldNameInput = false;
  public boolean consoleRequiresWorldName = true;
  public int minParams = 0;
  public int maxParams = 9999;
  public String helpText = null;
  public static final String C_CMD = ChatColor.AQUA.toString();
  public static final String C_DESC = ChatColor.WHITE.toString();
  public static final String C_ERR = ChatColor.RED.toString();
  public static final String C_HEAD = ChatColor.YELLOW.toString();
  public static final String C_OPT = ChatColor.DARK_GREEN.toString();
  public static final String C_REQ = ChatColor.GREEN.toString();
  public static final String CMD_C = C_CMD + "wb ";
  public static final String CMD_P = C_CMD + "/wb ";
  public List<String> cmdExamplePlayer = new ArrayList();
  public List<String> cmdExampleConsole = new ArrayList();
  public static final List<String> cmdExamplesConsole = new ArrayList(48);
  public static final List<String> cmdExamplesPlayer = new ArrayList(48);
  
  public abstract void execute(CommandSender paramCommandSender, Player paramPlayer, List<String> paramList, String paramString);
  
  public void cmdStatus(CommandSender sender) {}
  
  public void addCmdExample(String example)
  {
    addCmdExample(example, true, true, true);
  }
  
  public void addCmdExample(String example, boolean forPlayer, boolean forConsole, boolean prefix)
  {
    example = example.replace("<", C_REQ + "<").replace("[", C_OPT + "[").replace("^", C_CMD).replace("- ", C_DESC + "- ");
    if (forPlayer)
    {
      String exampleP = (prefix ? CMD_P : "") + example.replace("{", new StringBuilder(String.valueOf(C_OPT)).append("[").toString()).replace("}", "]");
      this.cmdExamplePlayer.add(exampleP);
      cmdExamplesPlayer.add(exampleP);
    }
    if (forConsole)
    {
      String exampleC = (prefix ? CMD_C : "") + example.replace("{", new StringBuilder(String.valueOf(C_REQ)).append("<").toString()).replace("}", ">");
      this.cmdExampleConsole.add(exampleC);
      cmdExamplesConsole.add(exampleC);
    }
  }
  
  public String cmd(CommandSender sender)
  {
    return (sender instanceof Player) ? CMD_P : CMD_C;
  }
  
  public String commandEmphasized(String text)
  {
    return C_CMD + ChatColor.UNDERLINE + text + ChatColor.RESET + " ";
  }
  
  public String enabledColored(boolean enabled)
  {
    return C_ERR + "disabled";
  }
  
  public String nameEmphasized()
  {
    return commandEmphasized(this.name);
  }
  
  public String nameEmphasizedW()
  {
    return "{world} " + nameEmphasized();
  }
  
  public void sendCmdHelp(CommandSender sender)
  {
    for (String example : (sender instanceof Player) ? this.cmdExamplePlayer : this.cmdExampleConsole) {
      sender.sendMessage(example);
    }
    cmdStatus(sender);
    if ((this.helpText != null) && (!this.helpText.isEmpty())) {
      sender.sendMessage(C_DESC + this.helpText);
    }
  }
  
  public void sendErrorAndHelp(CommandSender sender, String error)
  {
    sender.sendMessage(C_ERR + error);
    sendCmdHelp(sender);
  }
  
  public boolean strAsBool(String str)
  {
    str = str.toLowerCase();
    return (str.startsWith("y")) || (str.startsWith("t")) || (str.startsWith("on")) || (str.startsWith("+")) || (str.startsWith("1"));
  }
}
