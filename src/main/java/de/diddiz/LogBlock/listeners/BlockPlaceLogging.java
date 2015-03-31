package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.WorldConfig;
import de.diddiz.util.BukkitUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;
import static de.diddiz.LogBlock.config.Config.isLogging;

public class BlockPlaceLogging extends LoggingListener {
    public BlockPlaceLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        final WorldConfig wcfg = getWorldConfig(event.getBlock().getWorld());
        if (wcfg != null && wcfg.isLogging(Logging.BLOCKPLACE)) {
            final Material type = event.getBlock().getType();
            final BlockState before = event.getBlockReplacedState();
            final BlockState after = event.getBlockPlaced().getState();
            final Actor actor = Actor.actorFromEntity(event.getPlayer());

            //Handle falling blocks
            if (BukkitUtils.getRelativeTopFallables().contains(type)) {

                // Catch placed blocks overwriting something
                if (before.getType() != Material.AIR) {
                    consumer.queueBlockBreak(actor, before);
                }

                Location loc = event.getBlock().getLocation();
                int x = loc.getBlockX();
                int y = loc.getBlockY();
                int z = loc.getBlockZ();
                // Blocks only fall if they have a chance to start a velocity
                if (event.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR) {
                    while (y > 0 && BukkitUtils.canFall(loc.getWorld(), x, (y - 1), z)) {
                        y--;
                    }
                }
                // If y is 0 then the sand block fell out of the world :(
                if (y != 0) {
                    Location finalLoc = new Location(loc.getWorld(), x, y, z);
                    // Run this check to avoid false positives
                    if (!BukkitUtils.getFallingEntityKillers().contains(finalLoc.getBlock().getType())) {
                        if (finalLoc.getBlock().getType() == Material.AIR || finalLoc.equals(event.getBlock().getLocation())) {
                            consumer.queueBlockPlace(actor, finalLoc, type.getId(), event.getBlock().getData());
                        } else {
                            consumer.queueBlockReplace(actor, finalLoc, finalLoc.getBlock().getTypeId(), finalLoc.getBlock().getData(), type.getId(), event.getBlock().getData());
                        }
                    }
                }
                return;
            }

            //Sign logging is handled elsewhere
            if (wcfg.isLogging(Logging.SIGNTEXT) && (type.getId() == 63 || type.getId() == 68)) {
                return;
            }

            //Delay queuing by one tick to allow data to be updated
            LogBlock.getInstance().getServer().getScheduler().scheduleSyncDelayedTask(LogBlock.getInstance(), new Runnable() {
                @Override
                public void run() {
                    if (before.getTypeId() == 0) {
                        consumer.queueBlockPlace(actor, after);
                    } else {
                        consumer.queueBlockReplace(actor, before, after);
                    }
                }
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (isLogging(event.getPlayer().getWorld(), Logging.BLOCKPLACE)) {
            consumer.queueBlockPlace(Actor.actorFromEntity(event.getPlayer()), event.getBlockClicked().getRelative(event.getBlockFace()).getLocation(), event.getBucket() == Material.WATER_BUCKET ? 9 : 11, (byte) 0);
        }
    }
}
