package de.diddiz.LogBlock.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

import de.diddiz.LogBlock.LogBlock;
import de.diddiz.util.BukkitUtils;

public class HangingLogging extends LoggingListener 
{

	private static HangingLogging instance;
	
	public HangingLogging(LogBlock lb) {
		super(lb);
		instance = this;
	}
	
	private Material getType (Entity entity) {
		Material type = Material.AIR;
		if (entity instanceof ItemFrame) {
			type = Material.ITEM_FRAME;
		} else if (entity instanceof Painting) {
			type = Material.PAINTING;
		}
		return type;
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onHangingPlace(HangingPlaceEvent event) {	
		consumer.queueBlockPlace(event.getPlayer().getName(), event.getEntity().getLocation(), 
				getType(event.getEntity()).getId(), BukkitUtils.BlockFaceToRotationByte(event.getEntity().getFacing()));
	}	
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onHangingBreak(HangingBreakEvent event) {
		if (event.getCause() == RemoveCause.ENTITY)
			return;
		
		String logName = "Unknown";
		switch (event.getCause()) {
		case EXPLOSION:
			logName = "Explosion";
			break;
		case PHYSICS:
			logName = "Physics";
			break;
		}
		
		consumer.queueBlockBreak(logName, event.getEntity().getLocation(),
				getType(event.getEntity()).getId(), BukkitUtils.BlockFaceToRotationByte(event.getEntity().getFacing()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onHangingBreak(HangingBreakByEntityEvent event) {
		String logName = "Unknown";
		if (event.getRemover() instanceof Player) {
			logName = ((Player) event.getRemover()).getName();
		} else {
			logName = event.getRemover().getType().toString().toLowerCase().replace('_', ' ');
		}
		if (event.getEntity().getType() == EntityType.ITEM_FRAME) {
			ItemFrame frame = (ItemFrame) event.getEntity();
			consumer.queueContainerBreak(logName, event.getEntity().getLocation(), getType(event.getEntity()).getId(), 
				BukkitUtils.BlockFaceToRotationByte(event.getEntity().getFacing()), new ItemStack[] {frame.getItem()});
			return;
		}
		consumer.queueBlockBreak(logName, event.getEntity().getLocation(),
				getType(event.getEntity()).getId(), BukkitUtils.BlockFaceToRotationByte(event.getEntity().getFacing()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerInteractWithEntity(PlayerInteractEntityEvent event) {
		if (event.getRightClicked() instanceof ItemFrame) {
			ItemFrame frame = (ItemFrame) event.getRightClicked();
			Player player = event.getPlayer();
			if (frame.getItem().getType() == Material.AIR && player.getItemInHand().getType() != Material.AIR) {
				consumer.queueChestAccess(player.getName(), frame.getLocation(), Material.ITEM_FRAME.getId(), (short) player.getItemInHand().getTypeId(),
						(short) 1, player.getItemInHand().getData().getData());
			}
		}
	}
	
	public static void logHangingBreak(HangingBreakByEntityEvent event) {
		if (instance != null) {
			instance.onHangingBreak(event);
		}
	}
	
}