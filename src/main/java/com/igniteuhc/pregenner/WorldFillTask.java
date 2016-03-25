package com.igniteuhc.pregenner;

import com.igniteuhc.pregenner.events.WorldBorderFillFinishedEvent;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.IChatBaseComponent.ChatSerializer;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import net.minecraft.server.v1_8_R3.PlayerConnection;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

public class WorldFillTask
  implements Runnable
{
  private transient Server server = null;
  private transient World world = null;
  private transient BorderData border = null;
  private transient WorldFileData worldData = null;
  private transient boolean readyToGo = false;
  private transient boolean paused = false;
  private transient boolean pausedForMemory = false;
  private transient int taskID = -1;
  private transient Player notifyPlayer = null;
  private transient int chunksPerRun = 1;
  private transient boolean continueNotice = false;
  private transient boolean forceLoad = false;
  private transient int fillDistance = 208;
  private transient int tickFrequency = 1;
  private transient int refX = 0;
  private transient int lastLegX = 0;
  private transient int refZ = 0;
  private transient int lastLegZ = 0;
  private transient int refLength = -1;
  private transient int refTotal = 0;
  private transient int lastLegTotal = 0;
  private transient int x = 0;
  private transient int z = 0;
  private transient boolean isZLeg = false;
  private transient boolean isNeg = false;
  private transient int length = -1;
  private transient int current = 0;
  private transient boolean insideBorder = true;
  private List<CoordXZ> storedChunks = new LinkedList();
  private Set<CoordXZ> originalChunks = new HashSet();
  private transient CoordXZ lastChunk = new CoordXZ(0, 0);
  private transient long lastReport = Config.Now();
  private transient long lastAutosave = Config.Now();
  private transient int reportTarget = 0;
  private transient int reportTotal = 0;
  private transient int reportNum = 0;
  
  public WorldFillTask(Server theServer, Player player, String worldName, int fillDistance, int chunksPerRun, int tickFrequency, boolean forceLoad)
  {
    this.server = theServer;
    this.notifyPlayer = player;
    this.fillDistance = fillDistance;
    this.tickFrequency = tickFrequency;
    this.chunksPerRun = chunksPerRun;
    this.forceLoad = forceLoad;
    
    this.world = this.server.getWorld(worldName);
    if (this.world == null)
    {
      if (worldName.isEmpty()) {
        sendMessage("You must specify a world!");
      } else {
        sendMessage("World \"" + worldName + "\" not found!");
      }
      stop();
      return;
    }
    this.border = (Config.Border(worldName) == null ? null : 
      Config.Border(worldName).copy());
    if (this.border == null)
    {
      sendMessage("No border found for world \"" + worldName + "\"!");
      stop();
      return;
    }
    this.worldData = WorldFileData.create(this.world, this.notifyPlayer);
    if (this.worldData == null)
    {
      stop();
      return;
    }
    this.border.setRadiusX(this.border.getRadiusX() + fillDistance);
    this.border.setRadiusZ(this.border.getRadiusZ() + fillDistance);
    this.x = CoordXZ.blockToChunk((int)this.border.getX());
    this.z = CoordXZ.blockToChunk((int)this.border.getZ());
    
    int chunkWidthX = 
      (int)Math.ceil((this.border.getRadiusX() + 16) * 2 / 16.0D);
    int chunkWidthZ = 
      (int)Math.ceil((this.border.getRadiusZ() + 16) * 2 / 16.0D);
    int biggerWidth = chunkWidthX > chunkWidthZ ? chunkWidthX : 
      chunkWidthZ;
    
    this.reportTarget = (biggerWidth * biggerWidth + biggerWidth + 1);
    
    Chunk[] originals = this.world.getLoadedChunks();
    Chunk[] arrayOfChunk1;
    int j = (arrayOfChunk1 = originals).length;
    for (int i = 0; i < j; i++)
    {
      Chunk original = arrayOfChunk1[i];
      this.originalChunks.add(new CoordXZ(original.getX(), original.getZ()));
    }
    this.readyToGo = true;
  }
  
  public WorldFillTask(Server theServer, Player player, String worldName, int fillDistance, int chunksPerRun, int tickFrequency)
  {
    this(theServer, player, worldName, fillDistance, chunksPerRun, tickFrequency, false);
  }
  
  public void setTaskID(int ID)
  {
    if (ID == -1) {
      stop();
    }
    this.taskID = ID;
  }
  
  public void run()
  {
    if (this.continueNotice)
    {
      this.continueNotice = false;
      sendMessage("World map generation task automatically continuing.");
      sendMessage("Reminder: you can cancel at any time with \"wb fill cancel\", or pause/unpause with \"wb fill pause\".");
    }
    if (this.pausedForMemory)
    {
      if (Config.AvailableMemoryTooLow()) {
        return;
      }
      this.pausedForMemory = false;
      this.readyToGo = true;
      sendMessage("Available memory is sufficient, automatically continuing.");
    }
    if ((this.server == null) || (!this.readyToGo) || (this.paused)) {
      return;
    }
    this.readyToGo = false;
    
    long loopStartTime = Config.Now();
    for (int loop = 0; loop < this.chunksPerRun; loop++)
    {
      if ((this.paused) || (this.pausedForMemory)) {
        return;
      }
      long now = Config.Now();
      if (now > this.lastReport + 5000L) {
        reportProgress();
      }
      double perc = (this.reportTotal + this.reportNum) / this.reportTarget * 100.0D;
      if (perc > 100.0D) {
        perc = 100.0D;
      }
      for (Player online : Bukkit.getOnlinePlayers())
      {
        CraftPlayer craft = (CraftPlayer)online;
        
        IChatBaseComponent actionJSON = ChatSerializer.a("{text:\"§7Pregenning '§a" + this.world.getName() + "§7' (§6" + (
          this.reportTotal + this.reportNum) + " §7total, §a~" + 
          Config.coord.format(perc) + "%" + "§7)\"}");
        
        PacketPlayOutChat actionPacket = new PacketPlayOutChat(actionJSON, (byte)2);
        craft.getHandle().playerConnection.sendPacket(actionPacket);
      }
      if (now > loopStartTime + 45L)
      {
        this.readyToGo = true;
        return;
      }
      while (!this.border.insideBorder(CoordXZ.chunkToBlock(this.x) + 8, CoordXZ.chunkToBlock(this.z) + 8)) {
        if (!moveToNext()) {
          return;
        }
      }
      this.insideBorder = true;
      if (!this.forceLoad) {
        while (this.worldData.isChunkFullyGenerated(this.x, this.z))
        {
          this.insideBorder = true;
          if (!moveToNext()) {
            return;
          }
        }
      }
      this.world.loadChunk(this.x, this.z, true);
      this.worldData.chunkExistsNow(this.x, this.z);
      
      int popX = !this.isZLeg ? this.x : this.x + (this.isNeg ? -1 : 1);
      int popZ = this.isZLeg ? this.z : this.z + (!this.isNeg ? -1 : 1);
      this.world.loadChunk(popX, popZ, false);
      if ((!this.storedChunks.contains(this.lastChunk)) && 
        (!this.originalChunks.contains(this.lastChunk)))
      {
        this.world.loadChunk(this.lastChunk.x, this.lastChunk.z, false);
        this.storedChunks.add(new CoordXZ(this.lastChunk.x, this.lastChunk.z));
      }
      this.storedChunks.add(new CoordXZ(popX, popZ));
      this.storedChunks.add(new CoordXZ(this.x, this.z));
      while (this.storedChunks.size() > 8)
      {
        CoordXZ coord = (CoordXZ)this.storedChunks.remove(0);
        if (!this.originalChunks.contains(coord)) {
          this.world.unloadChunkRequest(coord.x, coord.z);
        }
      }
      if (!moveToNext()) {
        return;
      }
    }
    this.readyToGo = true;
  }
  
  public boolean moveToNext()
  {
    if ((this.paused) || (this.pausedForMemory)) {
      return false;
    }
    this.reportNum += 1;
    if ((!this.isNeg) && (this.current == 0) && (this.length > 3)) {
      if (!this.isZLeg)
      {
        this.lastLegX = this.x;
        this.lastLegZ = this.z;
        this.lastLegTotal = (this.reportTotal + this.reportNum);
      }
      else
      {
        this.refX = this.lastLegX;
        this.refZ = this.lastLegZ;
        this.refTotal = this.lastLegTotal;
        this.refLength = (this.length - 1);
      }
    }
    if (this.current < this.length)
    {
      this.current += 1;
    }
    else
    {
      this.current = 0;
      this.isZLeg ^= true;
      if (this.isZLeg)
      {
        this.isNeg ^= true;
        this.length += 1;
      }
    }
    this.lastChunk.x = this.x;
    this.lastChunk.z = this.z;
    if (this.isZLeg) {
      this.z += (this.isNeg ? -1 : 1);
    } else {
      this.x += (this.isNeg ? -1 : 1);
    }
    if ((this.isZLeg) && (this.isNeg) && (this.current == 0))
    {
      if (!this.insideBorder)
      {
        finish();
        return false;
      }
      this.insideBorder = false;
    }
    return true;
  }
  
  public void finish()
  {
    this.paused = true;
    reportProgress();
    this.world.save();
    stop();
    Bukkit.getServer().getPluginManager().callEvent(new WorldBorderFillFinishedEvent(this.world, this.reportTotal));
    for (Player online : Bukkit.getOnlinePlayers())
    {
      CraftPlayer craft = (CraftPlayer)online;
      
      IChatBaseComponent actionJSON = ChatSerializer.a("{text:\"§7Pregenning '§a" + this.world.getName() + "§7' (§6" + 
        this.reportTotal + " §7total, §a~100.0%" + "§7)\"}");
      
      PacketPlayOutChat actionPacket = new PacketPlayOutChat(actionJSON, (byte)2);
      craft.getHandle().playerConnection.sendPacket(actionPacket);
    }
  }
  
  public void cancel()
  {
    stop();
  }
  
  private void stop()
  {
    if (this.server == null) {
      return;
    }
    this.readyToGo = false;
    if (this.taskID != -1) {
      this.server.getScheduler().cancelTask(this.taskID);
    }
    this.server = null;
    while (!this.storedChunks.isEmpty())
    {
      CoordXZ coord = (CoordXZ)this.storedChunks.remove(0);
      if (!this.originalChunks.contains(coord)) {
        this.world.unloadChunkRequest(coord.x, coord.z);
      }
    }
  }
  
  public boolean valid()
  {
    return this.server != null;
  }
  
  public void pause()
  {
    if (this.pausedForMemory) {
      pause(false);
    } else {
      pause(!this.paused);
    }
  }
  
  public void pause(boolean pause)
  {
    if ((this.pausedForMemory) && (!pause)) {
      this.pausedForMemory = false;
    } else {
      this.paused = pause;
    }
    if (this.paused)
    {
      Config.StoreFillTask();
      reportProgress();
    }
    else
    {
      Config.UnStoreFillTask();
    }
  }
  
  public boolean isPaused()
  {
    return (this.paused) || (this.pausedForMemory);
  }
  
  private void reportProgress()
  {
    this.lastReport = Config.Now();
    double perc = (this.reportTotal + this.reportNum) / this.reportTarget * 100.0D;
    if (perc > 100.0D) {
      perc = 100.0D;
    }
    sendMessage(this.reportNum + " more chunks processed (" + (
      this.reportTotal + this.reportNum) + " total, ~" + 
      Config.coord.format(perc) + "%" + ")");
    this.reportTotal += this.reportNum;
    this.reportNum = 0;
    if ((Config.FillAutosaveFrequency() > 0) && 
      (this.lastAutosave + Config.FillAutosaveFrequency() * 1000 < this.lastReport))
    {
      this.lastAutosave = this.lastReport;
      sendMessage("Saving the world to disk, just to be on the safe side.");
      this.world.save();
    }
  }
  
  private void sendMessage(String text)
  {
    int availMem = Config.AvailableMemory();
    
    Config.log(text + " (free mem: " + availMem + " MB)");
    if (availMem < 200)
    {
      this.pausedForMemory = true;
      Config.StoreFillTask();
      text = "Available memory is very low, task is pausing. A cleanup will be attempted now, and the task will automatically continue if/when sufficient memory is freed up.\n Alternatively, if you restart the server, this task will automatically continue once the server is back up.";
      Config.log(text);
      
      System.gc();
    }
  }
  
  public void continueProgress(int x, int z, int length, int totalDone)
  {
    this.x = x;
    this.z = z;
    this.length = length;
    this.reportTotal = totalDone;
    this.continueNotice = true;
  }
  
  public int refX()
  {
    return this.refX;
  }
  
  public int refZ()
  {
    return this.refZ;
  }
  
  public int refLength()
  {
    return this.refLength;
  }
  
  public int refTotal()
  {
    return this.refTotal;
  }
  
  public int refFillDistance()
  {
    return this.fillDistance;
  }
  
  public int refTickFrequency()
  {
    return this.tickFrequency;
  }
  
  public int refChunksPerRun()
  {
    return this.chunksPerRun;
  }
  
  public String refWorld()
  {
    return this.world.getName();
  }
  
  public boolean refForceLoad()
  {
    return this.forceLoad;
  }
}
