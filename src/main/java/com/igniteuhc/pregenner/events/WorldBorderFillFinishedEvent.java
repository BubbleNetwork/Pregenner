package com.igniteuhc.pregenner.events;

import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WorldBorderFillFinishedEvent
  extends Event
{
  private static final HandlerList handlers = new HandlerList();
  private World world;
  private long totalChunks;
  
  public WorldBorderFillFinishedEvent(World world, long totalChunks)
  {
    this.world = world;
    this.totalChunks = totalChunks;
  }
  
  public HandlerList getHandlers()
  {
    return handlers;
  }
  
  public static HandlerList getHandlerList()
  {
    return handlers;
  }
  
  public World getWorld()
  {
    return this.world;
  }
  
  public long getTotalChunks()
  {
    return this.totalChunks;
  }
}
