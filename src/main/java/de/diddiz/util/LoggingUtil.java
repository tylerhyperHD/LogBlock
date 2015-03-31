package de.diddiz.util;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.Consumer;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.WorldConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.material.*;

import java.util.List;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;
import static de.diddiz.LogBlock.config.Config.mb4;

public class LoggingUtil {

    public static void smartLogFallables(Consumer consumer, Actor actor, Block origin) {

        WorldConfig wcfg = getWorldConfig(origin.getWorld());
        if (wcfg == null) {
            return;
        }

        //Handle falling blocks
        Block checkBlock = origin.getRelative(BlockFace.UP);
        int up = 0;
        final int highestBlock = checkBlock.getWorld().getHighestBlockYAt(checkBlock.getLocation());
        while (BukkitUtils.getRelativeTopFallables().contains(checkBlock.getType())) {

            // Record this block as falling
            consumer.queueBlockBreak(actor, checkBlock.getState());

            // Guess where the block is going (This could be thrown of by explosions, but it is better than nothing)
            Location loc = origin.getLocation();
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();
            while (y > 0 && BukkitUtils.canFall(loc.getWorld(), x, (y - 1), z)) {
                y--;
            }
            // If y is 0 then the sand block fell out of the world :(
            if (y != 0) {
                Location finalLoc = new Location(loc.getWorld(), x, y, z);
                // Run this check to avoid false positives
                if (!BukkitUtils.getFallingEntityKillers().contains(finalLoc.getBlock().getType())) {
                    finalLoc.add(0, up, 0); // Add this here after checking for block breakers
                    if (finalLoc.getBlock().getType() == Material.AIR || BukkitUtils.getRelativeTopFallables().contains(finalLoc.getBlock().getType())) {
                        consumer.queueBlockPlace(actor, finalLoc, checkBlock.getTypeId(), checkBlock.getData());
                    } else {
                        consumer.queueBlockReplace(actor, finalLoc, finalLoc.getBlock().getTypeId(), finalLoc.getBlock().getData(), checkBlock.getTypeId(), checkBlock.getData());
                    }
                    up++;
                }
            }
            if (checkBlock.getY() >= highestBlock) {
                break;
            }
            checkBlock = checkBlock.getRelative(BlockFace.UP);
        }
    }

    public static void smartLogBlockBreak(Consumer consumer, Actor actor, Block origin) {

        WorldConfig wcfg = getWorldConfig(origin.getWorld());
        if (wcfg == null) {
            return;
        }

        Block checkBlock = origin.getRelative(BlockFace.UP);
        if (BukkitUtils.getRelativeTopBreakabls().contains(checkBlock.getType())) {
            if (wcfg.isLogging(Logging.SIGNTEXT) && checkBlock.getType() == Material.SIGN_POST) {
                consumer.queueSignBreak(actor, (Sign) checkBlock.getState());
            } else if (checkBlock.getType() == Material.IRON_DOOR_BLOCK || checkBlock.getType() == Material.WOODEN_DOOR) {
                Block doorBlock = checkBlock;
                // If the doorBlock is the top half a door the player simply punched a door
                // this will be handled later.
                if (!BukkitUtils.isTop(doorBlock.getType(), doorBlock.getData())) {
                    doorBlock = doorBlock.getRelative(BlockFace.UP);
                    // Fall back check just in case the top half wasn't a door
                    if (doorBlock.getType() == Material.IRON_DOOR_BLOCK || doorBlock.getType() == Material.WOODEN_DOOR) {
                        consumer.queueBlockBreak(actor, doorBlock.getState());
                    }
                    consumer.queueBlockBreak(actor, checkBlock.getState());
                }
            } else if (checkBlock.getType() == Material.DOUBLE_PLANT) {
                Block plantBlock = checkBlock;
                // If the plantBlock is the top half of a double plant the player simply
                // punched the plant this will be handled later.
                if (!BukkitUtils.isTop(plantBlock.getType(), plantBlock.getData())) {
                    plantBlock = plantBlock.getRelative(BlockFace.UP);
                    // Fall back check just in case the top half wasn't a plant
                    if (plantBlock.getType() == Material.DOUBLE_PLANT) {
                        consumer.queueBlockBreak(actor, plantBlock.getState());
                    }
                    consumer.queueBlockBreak(actor, checkBlock.getState());
                }
            } else {
                consumer.queueBlockBreak(actor, checkBlock.getState());
            }
        }

        List<Location> relativeBreakables = BukkitUtils.getBlocksNearby(origin, BukkitUtils.getRelativeBreakables());
        if (relativeBreakables.size() != 0) {
            for (Location location : relativeBreakables) {
                final Material blockType = location.getBlock().getType();
                final BlockState blockState = location.getBlock().getState();
                final MaterialData data = blockState.getData();
                switch (blockType) {
                    case REDSTONE_TORCH_ON:
                    case REDSTONE_TORCH_OFF:
                        if (blockState.getBlock().getRelative(((RedstoneTorch) data).getAttachedFace()).equals(origin)) {
                            consumer.queueBlockBreak(actor, blockState);
                        }
                        break;
                    case TORCH:
                        if (blockState.getBlock().getRelative(((Torch) data).getAttachedFace()).equals(origin)) {
                            consumer.queueBlockBreak(actor, blockState);
                        }
                        break;
                    case COCOA:
                        if (blockState.getBlock().getRelative(((CocoaPlant) data).getAttachedFace().getOppositeFace()).equals(origin)) {
                            consumer.queueBlockBreak(actor, blockState);
                        }
                        break;
                    case LADDER:
                        if (blockState.getBlock().getRelative(((Ladder) data).getAttachedFace()).equals(origin)) {
                            consumer.queueBlockBreak(actor, blockState);
                        }
                        break;
                    case LEVER:
                        if (blockState.getBlock().getRelative(((Lever) data).getAttachedFace()).equals(origin)) {
                            consumer.queueBlockBreak(actor, blockState);
                        }
                        break;
                    case TRIPWIRE_HOOK:
                        if (blockState.getBlock().getRelative(((TripwireHook) data).getAttachedFace()).equals(origin)) {
                            consumer.queueBlockBreak(actor, blockState);
                        }
                        break;
                    case WOOD_BUTTON:
                    case STONE_BUTTON:
                        if (blockState.getBlock().getRelative(((Button) data).getAttachedFace()).equals(origin)) {
                            consumer.queueBlockBreak(actor, blockState);
                        }
                        break;
                    case WALL_SIGN:
                        if (blockState.getBlock().getRelative(((org.bukkit.material.Sign) data).getAttachedFace()).equals(origin)) {
                            if (wcfg.isLogging(Logging.SIGNTEXT)) {
                                consumer.queueSignBreak(actor, (Sign) blockState);
                            } else {
                                consumer.queueBlockBreak(actor, blockState);
                            }
                        }
                        break;
                    case TRAP_DOOR:
                        if (blockState.getBlock().getRelative(((TrapDoor) data).getAttachedFace()).equals(origin)) {
                            consumer.queueBlockBreak(actor, blockState);
                        }
                        break;
                    default:
                        consumer.queueBlockBreak(actor, blockState);
                        break;
                }
            }
        }

        // Special door check
        if (origin.getType() == Material.IRON_DOOR_BLOCK || origin.getType() == Material.WOODEN_DOOR) {
            Block doorBlock = origin;

            // Up or down?
            if (!BukkitUtils.isTop(doorBlock.getType(), doorBlock.getData())) {
                doorBlock = doorBlock.getRelative(BlockFace.UP);
            } else {
                doorBlock = doorBlock.getRelative(BlockFace.DOWN);
            }

            if (doorBlock.getType() == Material.IRON_DOOR_BLOCK || doorBlock.getType() == Material.WOODEN_DOOR) {
                consumer.queueBlockBreak(actor, doorBlock.getState());
            }
        } else if (origin.getType() == Material.DOUBLE_PLANT) { // Special double plant check
            Block plantBlock = origin;

            // Up or down?
            if (!BukkitUtils.isTop(origin.getType(), origin.getData())) {
                plantBlock = plantBlock.getRelative(BlockFace.UP);
            } else {
                plantBlock = plantBlock.getRelative(BlockFace.DOWN);
            }

            if (plantBlock.getType() == Material.DOUBLE_PLANT) {
                consumer.queueBlockBreak(actor, plantBlock.getState());
            }
        }

        // Do this down here so that the block is added after blocks sitting on it
        consumer.queueBlockBreak(actor, origin.getState());
    }

    public static String checkText(String text) {
        if (text == null) {
            return text;
        }
        if (mb4) {
            return text;
        }
        return text.replaceAll("[^\\u0000-\\uFFFF]", "?");
    }
}
