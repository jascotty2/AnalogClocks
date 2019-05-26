/**
 * Copyright (C) 2018 Jacob Scott <jascottytechie@gmail.com>
 * Description: Individual clock instance
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
import java.util.Arrays;
import java.util.List;
import me.jascotty2.libv3.io.NBT;
import me.jascotty2.libv3.util.FastMath;
import me.jascotty2.libv3.util.NBTList;
import me.jascotty2.libv3.util.NBTMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

public class Clock implements NBT.Compound {

	public final String name;
	boolean updateMinutes;
	int displayTime = -1; // value between 0 and 24000
	//Location locSEU, locNWD; 
	BlockVector bSEU, bNWD; // U = +, D = -
	/**
	 * Which direction the clock is facing
	 */
	BlockFace clockFace, clockFace_FlatBase = null;
	String worldName;
	World world;
	Material mMin, mHour, mCenter;
	// values stored locally (not saved)
	int cx = Integer.MAX_VALUE, cy = Integer.MAX_VALUE, cz = Integer.MAX_VALUE, cr = 3;
	boolean deepClock = false;

	public Clock(String name, Server s, boolean minutes) {
		this.name = name;
		updateMinutes = minutes;
	}

	public Clock(String name, Server s, World w, boolean minutes, Location locNWD, Location locSEU, BlockFace clockFace,
			Material mMin, Material mHour, Material mCenter) {
		this.name = name;
		updateMinutes = minutes;
		if (locSEU != null) {
			this.bSEU = new BlockVector(locSEU.getBlockX(), locSEU.getBlockY(), locSEU.getBlockZ());
		}
		if (locNWD != null) {
			this.bNWD = new BlockVector(locNWD.getBlockX(), locNWD.getBlockY(), locNWD.getBlockZ());
		}
		if ((world = w) != null) {
			worldName = w.getName();
		}
		this.clockFace = clockFace;
		this.mMin = mMin;
		this.mHour = mHour;
		this.mCenter = mCenter;
	}

	void update() {
		if (world != null && bSEU != null && bNWD != null
				&& (world.isChunkLoaded(bSEU.getBlockX() >> 4, bSEU.getBlockZ() >> 4)
				|| (((bNWD.getBlockX() >> 4) != (bSEU.getBlockX() >> 4) || (bNWD.getBlockZ() >> 4) != (bSEU.getBlockZ() >> 4))
				&& world.isChunkLoaded(bNWD.getBlockX() >> 4, bNWD.getBlockZ() >> 4)))) {
			// check to see if an update is needed
			// (minutes update every 5 minecraft minutes)
			int worldTime = (int) (updateMinutes ? (((int) (world.getTime() / 83.3333)) * 83.3333) : (((int) (world.getTime() / 1000)) * 1000));
			if (displayTime != worldTime) {
				forceUpdate(worldTime);
				displayTime = worldTime;
			}
		}
	}

	void calcCenter() {
		if (clockFace_FlatBase != null) {
			// flat clock
			cx = bNWD.getBlockX() + (cr = (bSEU.getBlockX() - bNWD.getBlockX()) / 2);
			cz = bNWD.getBlockZ() + cr;
		} else {
			cy = bNWD.getBlockY() + (cr = (bSEU.getBlockY() - bNWD.getBlockY()) / 2);
		}
		switch (clockFace) {
			case DOWN:
				cy = bSEU.getBlockY();
				deepClock = cy != bNWD.getBlockY();
				break;
			case UP:
				cy = bNWD.getBlockY();
				deepClock = cy != bSEU.getBlockY();
				break;
			case NORTH: // -z
				cx = bNWD.getBlockX() + (cr = (bSEU.getBlockX() - bNWD.getBlockX()) / 2);
				cz = bSEU.getBlockZ();
				deepClock = cz != bNWD.getBlockZ();
				break;
			case SOUTH: // +z
				cx = bNWD.getBlockX() + (cr = (bSEU.getBlockX() - bNWD.getBlockX()) / 2);
				cz = bNWD.getBlockZ();
				deepClock = cz != bSEU.getBlockZ();
				break;
			case WEST: // -x
				cz = bNWD.getBlockZ() + (cr = (bSEU.getBlockZ() - bNWD.getBlockZ()) / 2);
				cx = bSEU.getBlockX();
				deepClock = cx != bNWD.getBlockX();
				break;
			case EAST: // +x
				cz = bNWD.getBlockZ() + (cr = (bSEU.getBlockZ() - bNWD.getBlockZ()) / 2);
				cx = bNWD.getBlockX();
				deepClock = cx != bSEU.getBlockX();
		}
		// don't draw on the border
		--cr;
	}

	void forceUpdate(int time) {
		// first: grab the center-back of the clock
		if (cx == Integer.MAX_VALUE) {
			calcCenter();
		}
		if (cx == Integer.MAX_VALUE || cy == Integer.MAX_VALUE || cz == Integer.MAX_VALUE) {
			// should never happen, but just in case..
			return;
		}
		// remove anything not created or updated this round
		clearOldBlocks(time);

		// draw hour hand first
		int hourTime = ((int) Math.round(time / 1000)) * 1000;
		// time = 6000 = 12 noon
		double hour = (hourTime / 6000.) * Math.PI - Math.PI;
		drawHand(hour, cr * 2 / 3, false, time);
		// then minute hand
		double minute = ((time - hourTime) / 500.) * Math.PI;
		drawHand(minute, cr, true, time);

		// center piece is always one pixel
		// (may update in the future for larger clocks)
		Block b = world.getBlockAt(cx, cy, cz);
		b.setType(mCenter);
		b.setMetadata("AnalogClock", new FixedMetadataValue(AnalogClocks.plugin, time));
		if (deepClock) {
			b = b.getRelative(clockFace);
			b.setType(mCenter);
			b.setMetadata("AnalogClock", new FixedMetadataValue(AnalogClocks.plugin, time));
		}

	}

	void drawHand(double angle, double length, boolean front, int time) {
		double ax = FastMath.sin(angle);
		double ay = FastMath.cos(angle);
		double len = length + .6;//Math.sqrt(length * 2);
		int lastx = Integer.MIN_VALUE, lasty = Integer.MIN_VALUE;
		for (double i = .5; i <= len; ++i) {
			int x = (int) Math.round(i * ax);
			int y = (int) Math.round(i * ay);
			if (x != lastx || y != lasty) {
				setBlock(x, y, front, time);
				lastx = x;
				lasty = y;
			}
		}
	}

	void setBlock(int xn, int yn, boolean front, int time) {
		Block b = null;
		switch (clockFace) {
			case NORTH: // -z
				b = world.getBlockAt(cx - xn, cy + yn, cz);
				break;
			case SOUTH: // +z
				b = world.getBlockAt(cx + xn, cy + yn, cz);
				break;
			case WEST: // -x
				b = world.getBlockAt(cx, cy + yn, cz + xn);
				break;
			case EAST: // +x
				b = world.getBlockAt(cx, cy + yn, cz - xn);
				break;
			case UP:
				switch (clockFace_FlatBase) {
					case NORTH:
						b = world.getBlockAt(cx + xn, cy, cz - yn);
						break;
					case SOUTH:
						b = world.getBlockAt(cx - xn, cy, cz + yn);
						break;
					case WEST:
						b = world.getBlockAt(cx - yn, cy, cz - xn);
						break;
					case EAST:
						b = world.getBlockAt(cx + yn, cy, cz + xn);
						break;
				}
				break;
			case DOWN:
				switch (clockFace_FlatBase) {
					case NORTH:
						b = world.getBlockAt(cx + xn, cy, cz + yn);
						break;
					case SOUTH:
						b = world.getBlockAt(cx - xn, cy, cz - yn);
						break;
					case WEST:
						b = world.getBlockAt(cx + yn, cy, cz - xn);
						break;
					case EAST:
						b = world.getBlockAt(cx - yn, cy, cz + xn);
						break;
				}
				break;
		}
		if (b != null && b.getType() == Material.AIR) {
			if (front) {
				if (deepClock) {
					b = b.getRelative(clockFace);
				}
				b.setType(mMin);
			} else {
				b.setType(mHour);
			}
			b.setMetadata("AnalogClock", new FixedMetadataValue(AnalogClocks.plugin, time));
		}
	}

	void clearOldBlocks(int time) {
		int dx = clockFace == BlockFace.WEST || clockFace == BlockFace.EAST ? 0 : 1;
		int dz = clockFace == BlockFace.NORTH || clockFace == BlockFace.SOUTH ? 0 : 1;
		int dy = clockFace == BlockFace.UP || clockFace == BlockFace.DOWN ? 0 : 1;
		for (int x = bNWD.getBlockX() + dx; x <= bSEU.getBlockX() - dx; ++x) {
			for (int z = bNWD.getBlockZ() + dz; z <= bSEU.getBlockZ() - dz; ++z) {
				for (int y = bNWD.getBlockY() + dy; y <= bSEU.getBlockY() - dy; ++y) {
					Block b = world.getBlockAt(x, y, z);
					if (b.getType() != Material.AIR) {
						List<MetadataValue> mv = b.getMetadata("AnalogClock");
						if (mv.size() > 1) {
							// just in case some other plugin is trying to claim this namespace
							for (MetadataValue v : mv) {
								if (v.getOwningPlugin() == AnalogClocks.plugin && v.asInt() != time) {
									b.setType(Material.AIR);
									b.removeMetadata("AnalogClock", AnalogClocks.plugin);
									break;
								}
							}
						} else if (mv.size() == 1 && mv.get(0).asInt() != time) {
							b.setType(Material.AIR);
							b.removeMetadata("AnalogClock", AnalogClocks.plugin);
						}
					}
				}
			}
		}
	}
	void clear() {
		int dx = clockFace == BlockFace.WEST || clockFace == BlockFace.EAST ? 0 : 1;
		int dz = clockFace == BlockFace.NORTH || clockFace == BlockFace.SOUTH ? 0 : 1;
		int dy = clockFace == BlockFace.UP || clockFace == BlockFace.DOWN ? 0 : 1;
		for (int x = bNWD.getBlockX() + dx; x <= bSEU.getBlockX() - dx; ++x) {
			for (int z = bNWD.getBlockZ() + dz; z <= bSEU.getBlockZ() - dz; ++z) {
				for (int y = bNWD.getBlockY() + dy; y <= bSEU.getBlockY() - dy; ++y) {
					Block b = world.getBlockAt(x, y, z);
					if (b.getType() != Material.AIR) {
						if (!b.getMetadata("AnalogClock").isEmpty()) {
							b.setType(Material.AIR);
						}
					}
				}
			}
		}
	}
	
	void removeClock() {
		int dx = clockFace == BlockFace.WEST || clockFace == BlockFace.EAST ? 0 : 1;
		int dz = clockFace == BlockFace.NORTH || clockFace == BlockFace.SOUTH ? 0 : 1;
		int dy = clockFace == BlockFace.UP || clockFace == BlockFace.DOWN ? 0 : 1;
		for (int x = bNWD.getBlockX() + dx; x <= bSEU.getBlockX() - dx; ++x) {
			for (int z = bNWD.getBlockZ() + dz; z <= bSEU.getBlockZ() - dz; ++z) {
				for (int y = bNWD.getBlockY() + dy; y <= bSEU.getBlockY() - dy; ++y) {
					Block b = world.getBlockAt(x, y, z);
					List<MetadataValue> mv = b.getMetadata("AnalogClock");
					if (mv.size() > 1) {
						// just in case some other plugin is trying to claim this namespace
						for (MetadataValue v : mv) {
							if (v.getOwningPlugin() == AnalogClocks.plugin) {
								b.removeMetadata("AnalogClock", AnalogClocks.plugin);
								b.setType(Material.AIR);
								break;
							}
						}
					} else if (mv.size() == 1) {
						b.removeMetadata("AnalogClock", AnalogClocks.plugin);
						b.setType(Material.AIR);
					}
				}
			}
		}
	}

	@Override
	public String[] nbtKeys() {
		return new String[]{"n", "d", "w", "m", "seu", "nwd",
			"cf", "cb", "mM", "mH", "mC"};
	}

	@Override
	public Object nbtValue(int keyIndex) {
		switch (keyIndex) {
			case 0:
				return name;
			case 1:
				return displayTime;
			case 2:
				return worldName;
			case 3:
				return updateMinutes;
			case 4: // seu
				return bSEU == null ? (byte) 0 : Arrays.asList(bSEU.getBlockX(), bSEU.getBlockY(), bSEU.getBlockZ());
			case 5: // nwd
				return bNWD == null ? (byte) 0 : Arrays.asList(bNWD.getBlockX(), bNWD.getBlockY(), bNWD.getBlockZ());
			case 6: // cf
				return clockFace == null ? (byte) 0 : clockFace.name();
			case 7: // cb
				return clockFace_FlatBase == null ? (byte) 0 : clockFace_FlatBase.name();
			case 8: // "mM", "mH", "mC"
				return mMin == null ? (String) null : mMin.name();
			case 9:
				return mHour == null ? (String) null : mHour.name();
			case 10:
				return mCenter == null ? (String) null : mCenter.name();

		}
		return null;
	}

	static Clock fromNBT(NBTMap nbt, Server s) {
		if (!nbt.containsKey("n")) {
			return null;
		}
		Clock c = new Clock(nbt.getString("n"), s, nbt.getBoolean("m", false));
		c.world = s.getWorld(c.worldName = nbt.getString("w", ""));
		c.displayTime = nbt.getInteger("d", 0);
		NBTList l = nbt.getNBTList("seu");
		if (l != null && l.getTagType() == NBT.Tag.INT && l.size() == 3) {
			c.bSEU = new BlockVector((Integer) l.get(0), (Integer) l.get(1), (Integer) l.get(2));
		}
		if ((l = nbt.getNBTList("nwd")) != null && l.getTagType() == NBT.Tag.INT && l.size() == 3) {
			c.bNWD = new BlockVector((Integer) l.get(0), (Integer) l.get(1), (Integer) l.get(2));
		}
		String f = nbt.getString("cf");
		if (f != null) {
			c.clockFace = BlockFace.valueOf(f);
		}
		if ((f = nbt.getString("cb")) != null) {
			c.clockFace_FlatBase = BlockFace.valueOf(f);
		}// "mM", "dM", "mH", "dH", "mC", "dC"
		String m = nbt.getString("mM");
		if (m != null) {
			c.mMin = Material.getMaterial(m);
		}
		if ((m = nbt.getString("mH")) != null) {
			c.mHour = Material.getMaterial(m);
		}
		if ((m = nbt.getString("mC")) != null) {
			c.mCenter = Material.getMaterial(m);
		}
		return c;
	}

}
