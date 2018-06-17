/**
 * Copyright (C) 2018 Jacob Scott <jascottytechie@gmail.com>
 * Description: Bukkit plugin to create and update analog clock displays
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package me.jascotty2.analogclock;

// using a local copy of world edit's BlockVector, so the plugin still functions in case WE is removed from the server
import com.sk89q.we.BlockVector;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.blocks.ClothColor;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import me.jascotty2.libv3.io.NBT;
import me.jascotty2.libv3.util.NBTMap;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class AnalogClocks extends JavaPlugin implements Runnable {

	File dataFile;
	String prefix = ChatColor.GOLD.toString() + "[" + ChatColor.DARK_AQUA + "AnalogClocks" + ChatColor.GOLD + "] ";
	HashMap<String, Clock> clocks = new HashMap<String, Clock>();
	boolean dirty = false;
	int taskID = -1;
	public WorldEditPlugin worldEdit = null;
	final static int MIN_CLOCK_SIZE = 5;
	final static int MAX_CLOCK_SIZE = 150;
	final static Material DEFAULT_HAND_MATERIAL = Material.WOOL;
	final static byte DEFAULT_HAND_MATERIAL_D = 15;
	protected static AnalogClocks plugin = null;

	@Override
	public void onLoad() {
		plugin = this;
	}

	@Override
	public void onEnable() {
		// w.e.
		Plugin we = getServer().getPluginManager().getPlugin("WorldEdit");
		if (we != null && we instanceof WorldEditPlugin) {
			worldEdit = (WorldEditPlugin) we;
		} else {
			getLogger().log(Level.INFO, "Failed to find WorldEdit: new clocks cannot be defined");
		}

		dataFile = new File(getDataFolder(), "data.nbt");

		if (dataFile.exists()) {
			try {
				NBTMap dat = NBT.load(dataFile);
				final Server s = getServer();
				for (String k : dat.keySet()) {
					clocks.put(k.toLowerCase(), Clock.fromNBT(dat.getNBTMap(k), s));
				}
			} catch (IOException ex) {
				getLogger().log(Level.SEVERE, "Failed to load clocks", ex);
			}
		} else if (!dataFile.getParentFile().exists()) {
			dataFile.getParentFile().mkdirs();
		}
		// each minecraft hour takes 1000 ticks (50 seconds). 
		taskID = getServer().getScheduler().scheduleSyncRepeatingTask(this, this, 100, 20);
	}

	@Override
	public void onDisable() {
		if (taskID != -1) {
			getServer().getScheduler().cancelTask(taskID);
			taskID = -1;
		}
		save();
	}

	@Override
	public void run() {
		final Server s = getServer();
		for (Clock c : clocks.values()) {
			if (c != null) {
				c.update();
			}
		}
	}

	void save() {
		if (dirty) {
			try {
				NBT.save((Map) clocks, dataFile);
				//System.out.println(NBT.debugContents((Map)clocks));
			} catch (IOException ex) {
				getLogger().log(Level.SEVERE, "Failed to save clocks", ex);
				for (Player p : getServer().getOnlinePlayers()) {
					if (p.hasPermission("analogclock.admin")) {
						p.sendMessage(prefix + ChatColor.RED + "Error saving data - check the console for more information");
					}
				}
			}
			dirty = false;
		}
	}

	/*
    usage: |
      /analogclock create <name> [minute-block] [hour-block] [center-block] [hours-only]
      /analogclock delete <name>
      /analogclock list
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		if (command.getName().equalsIgnoreCase("analogclock") && args.length > 0) {
			if (args[0].equalsIgnoreCase("create") && args.length > 1) {
				if (!(sender instanceof Player)) {
					sender.sendMessage(prefix + ChatColor.RED + "Must be executed as a player");
				} else {
					addClock((Player) sender, args);
				}
			} else if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("remove")) {
				if (args.length == 2) {
					removeClock(sender, args[1]);
				} else if (!(sender instanceof Player)) {
					sender.sendMessage(prefix + ChatColor.RED + "Selection remove must be executed as a player");
				} else {
					removeClock((Player) sender);
				}
			} else if (args[0].equalsIgnoreCase("list")) {
				listClocks(sender);
			}
		} else {
			return false;
		}
		return true;
	}

	void listClocks(CommandSender p) {
		p.sendMessage(prefix + ChatColor.AQUA + clocks.size() + " Clocks:");
		for (Clock c : clocks.values()) {
			p.sendMessage(String.format("%s (%s %d, %d, %d)", prefix + ChatColor.WHITE + c.name,
					c.worldName, c.bSEU.getBlockX(), c.bSEU.getBlockY(), c.bSEU.getBlockZ()));
		}
	}

	void removeClock(CommandSender p, String clock) {
		Clock c = clocks.remove(clock.toLowerCase());
		if (c != null) {
			c.removeClock();
			p.sendMessage(prefix + ChatColor.GREEN + "Clock removed!");
			dirty = true;
			save();
		} else {
			p.sendMessage(prefix + ChatColor.RED + "Clock not found");
		}
	}

	void removeClock(Player p) {
		if (worldEdit == null) {
			p.sendMessage(prefix + ChatColor.RED + "Missing WorldEdit!");
		} else {
			com.sk89q.worldedit.bukkit.selections.Selection sel = worldEdit.getSelection(p);

			final com.sk89q.worldedit.BlockVector min;
			final com.sk89q.worldedit.BlockVector max;

			if (sel == null) {
				p.sendMessage(prefix + ChatColor.RED + "Select a region for region remove, or specify the name of the clock to remove");
				return;
			} else if (sel instanceof com.sk89q.worldedit.bukkit.selections.CuboidSelection) {

				min = sel.getNativeMinimumPoint().toBlockVector();
				max = sel.getNativeMaximumPoint().toBlockVector();

			} else {
				p.sendMessage(prefix + ChatColor.RED + "The type of region selected in WorldEdit is unsupported!");
				return;
			}

			int removed = 0;
			for (Map.Entry<String, Clock> e : clocks.entrySet().toArray(new Map.Entry[0])) {
				final BlockVector l1 = e.getValue().bSEU, l2 = e.getValue().bNWD;
				if (l1 != null && l2 != null) {
					final int x1 = l1.getBlockX();
					final int y1 = l1.getBlockY();
					final int z1 = l1.getBlockZ();
					final int x2 = l2.getBlockX();
					final int y2 = l2.getBlockY();
					final int z2 = l2.getBlockZ();

					if ((x1 >= min.getBlockX() && x1 <= max.getBlockX()
							&& y1 >= min.getBlockY() && y1 <= max.getBlockY()
							&& z1 >= min.getBlockZ() && z1 <= max.getBlockZ())
							|| (x2 >= min.getBlockX() && x2 <= max.getBlockX()
							&& y2 >= min.getBlockY() && y2 <= max.getBlockY()
							&& z2 >= min.getBlockZ() && z2 <= max.getBlockZ())) {
						// one of these points are contained in this region
						final String n = e.getValue().name;
						clocks.remove(e.getKey());
						p.sendMessage(prefix + ChatColor.GREEN + "Clock " + ChatColor.DARK_AQUA + n + ChatColor.GREEN + " Removed");
						++removed;
					}
				}
			}
			if (removed == 0) {
				p.sendMessage(prefix + ChatColor.RED + "No clocks are contained within this selection");
			} else {
				dirty = true;
				save();
			}
		}
	}

	void addClock(Player p, String[] args) {
		if (worldEdit == null) {
			p.sendMessage(prefix + ChatColor.RED + "Missing WorldEdit!");
		} else {
			// args start at 1 here
			final String clockName = args[1].toLowerCase();
			if (clocks.containsKey(clockName)) {
				p.sendMessage(prefix + ChatColor.RED + "A clock by that name already exists");
				return;
			}

			com.sk89q.worldedit.bukkit.selections.Selection sel = worldEdit.getSelection(p);

			Location min = null;
			Location max = null;
			boolean ok = false;
			BlockFace clockFace = null, clockFace2 = null;

			if (sel == null) {
				p.sendMessage(prefix + ChatColor.RED + "Select a region to define first");
			} else if (!sel.getMinimumPoint().getWorld().getUID().equals(p.getLocation().getWorld().getUID())) {
				p.sendMessage(prefix + ChatColor.RED + "Cannot define a clock in a different world!");
			} else if (sel instanceof com.sk89q.worldedit.bukkit.selections.CuboidSelection) {

				min = sel.getMinimumPoint();
				max = sel.getMaximumPoint();

				// sanity check
				if (max.getBlockY() - min.getBlockY() <= 2) {
					// floor clock?
					final int dx = max.getBlockX() - min.getBlockX();
					final int dz = max.getBlockZ() - min.getBlockZ();
					if (dx < MIN_CLOCK_SIZE || dz < MIN_CLOCK_SIZE) {
						p.sendMessage(prefix + ChatColor.RED + "Space for clock is too small");
					} else if (dx > MAX_CLOCK_SIZE || dz > MAX_CLOCK_SIZE) {
						p.sendMessage(prefix + ChatColor.RED + "Space for clock is too large");
					} else if (dx != dz) {
						p.sendMessage(prefix + ChatColor.RED + String.format("Clock selection must be square! (Selected %dx%d)", dx + 1, dz + 1));
					} else if (dx % 2 != 0) {
						p.sendMessage(prefix + ChatColor.RED + String.format("Clock selection must be odd-sized! (Selected %d-squared)", dx));
					} else {
						ok = true;
						final Location pLoc = p.getLocation();
						if (pLoc.getBlockY() < min.getBlockY()) {
							clockFace = BlockFace.DOWN;
						} else {
							clockFace = BlockFace.UP;
						}
						// where is the 'bottom' facing?
						float yaw = pLoc.getYaw();
						yaw = yaw - (((int) (yaw / 360)) * 360);
						if (yaw < 0) {
							yaw += 360;
						}
						if (yaw >= 315 || yaw <= 45) {
							clockFace2 = BlockFace.SOUTH;
						} else if (yaw >= 135 && yaw <= 225) {
							clockFace2 = BlockFace.NORTH;
						} else if (yaw > 225) {
							clockFace2 = BlockFace.EAST;
						} else {
							clockFace2 = BlockFace.WEST;
						}
					}
				} else {
					// wall clock?
					final int dy = max.getBlockY() - min.getBlockY();
					// this gets tricky : need to determine where the face is
					final int dx = max.getBlockX() - min.getBlockX();
					final int dz = max.getBlockZ() - min.getBlockZ();
					if (dx <= 2) {
						// x-axis?
						if (dz < MIN_CLOCK_SIZE || dy < MIN_CLOCK_SIZE) {
							p.sendMessage(prefix + ChatColor.RED + "Space for clock is too small");
						} else if (dz > MAX_CLOCK_SIZE || dy > MAX_CLOCK_SIZE) {
							p.sendMessage(prefix + ChatColor.RED + "Space for clock is too large");
						} else if (dz != dy) {
							p.sendMessage(prefix + ChatColor.RED + String.format("Clock selection must be square! (Selected %dx%d)", dz + 1, dy + 1));
						} else if (dy % 2 != 0) {
							p.sendMessage(prefix + ChatColor.RED + String.format("Clock selection must be odd-sized! (Selected %d-squared)", dy + 1));
						} else {
							ok = true;
							final Location pLoc = p.getLocation();
							if (pLoc.getBlockX() < min.getBlockX()) {
								clockFace = BlockFace.WEST;
							} else {
								clockFace = BlockFace.EAST;
							}
						}
					} else if (dz <= 2) {
						// x-axis?
						if (dx < MIN_CLOCK_SIZE || dy < MIN_CLOCK_SIZE) {
							p.sendMessage(prefix + ChatColor.RED + "Space for clock is too small");
						} else if (dx > MAX_CLOCK_SIZE || dy > MAX_CLOCK_SIZE) {
							p.sendMessage(prefix + ChatColor.RED + "Space for clock is too large");
						} else if (dx != dy) {
							p.sendMessage(prefix + ChatColor.RED + String.format("Clock selection must be square! (Selected %dx%d)", dx, dy));
						} else if (dy % 2 != 0) {
							p.sendMessage(prefix + ChatColor.RED + String.format("Clock selection must be odd-sized! (Selected %d-squared)", dy + 1));
						} else {
							ok = true;
							final Location pLoc = p.getLocation();
							if (pLoc.getBlockZ() < min.getBlockZ()) {
								clockFace = BlockFace.NORTH;
							} else {
								clockFace = BlockFace.SOUTH;
							}
						}
					} else {
						p.sendMessage(prefix + ChatColor.RED + "Clock can only be 1 or 2 blocks thick!");
					}
				}
			} else {
				p.sendMessage(prefix + ChatColor.RED + "The type of region selected in WorldEdit is unsupported!");
			}

			if (ok) {
				// let's set up this clock, then!
				// /analogclock create <name> [minute-block] [hour-block] [center-block] [hours-only]
				Material mMin, mHour, mCenter;
				byte dMin = 0, dHour = 0, dCenter = 0;
				boolean minuteClock = true;

				if (args.length >= 3) {
					// todo? :data
					BlockType t = BlockType.lookup(args[2]);
					if (t == null) {
						// is it a wool color?
						ClothColor col = ClothColor.lookup(args[2]);
						if (col == null) {
							p.sendMessage(prefix + ChatColor.RED + "Unknown material type: " + args[2]);
							return;
						}
						mMin = Material.WOOL;
						dMin = (byte) col.getID();
					} else {
						mMin = Material.getMaterial(t.getID());
					}
				} else {
					mMin = DEFAULT_HAND_MATERIAL;
					dMin = DEFAULT_HAND_MATERIAL_D;
				}

				if (args.length >= 4) {
					BlockType t = BlockType.lookup(args[3]);
					if (t == null) {
						// is it a wool color?
						ClothColor col = ClothColor.lookup(args[3]);
						if (col == null) {
							p.sendMessage(prefix + ChatColor.RED + "Unknown material type: " + args[3]);
							return;
						}
						mHour = Material.WOOL;
						dHour = (byte) col.getID();
					} else {
						mHour = Material.getMaterial(t.getID());
					}
				} else {
					mHour = mMin;
					dHour = dMin;
				}

				if (args.length >= 5) {
					BlockType t = BlockType.lookup(args[4]);
					if (t == null) {
						// is it a wool color?
						ClothColor col = ClothColor.lookup(args[4]);
						if (col == null) {
							p.sendMessage(prefix + ChatColor.RED + "Unknown material type: " + args[4]);
							return;
						}
						mCenter = Material.WOOL;
						dCenter = (byte) col.getID();
					} else {
						mCenter = Material.getMaterial(t.getID());
					}
				} else {
					mCenter = mHour;
					dCenter = dHour;
				}

				if (args.length >= 6) {
					minuteClock = !(args[5].equalsIgnoreCase("true") || args[5].equalsIgnoreCase("t") || args[5].equals("1"));
				}

				Clock c = new Clock(args[1], getServer(), p.getWorld(), minuteClock, min, max, clockFace, mMin, dMin, mHour, dHour, mCenter, dCenter);
				c.clockFace_FlatBase = clockFace2;

				clocks.put(clockName, c);
				dirty = true;
				save();
				c.update();
				p.sendMessage(prefix + ChatColor.GREEN + "Clock created!");
			}
		}
	}

}
