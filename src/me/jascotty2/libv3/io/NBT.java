/**
 * Copyright (C) 2014 Jacob Scott <jascottytechie@gmail.com> 
 * 
 * Description:
 * Simple Implementation of notch's NBT format, as described here
 * http://web.archive.org/web/20110723210920/http://www.minecraft.net/docs/NBT.txt
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
package me.jascotty2.libv3.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import me.jascotty2.libv3.util.NBTList;
import me.jascotty2.libv3.util.NBTMap;

public class NBT {

	public static final int MAX_DEPTH = 512;

	public static enum Tag {

		/**
		 * This tag is used to mark the end of a list.
		 */
		END(null),
		/**
		 * A single signed byte (8 bits)
		 */
		BYTE(Byte.class),
		/**
		 * A signed short (16 bits, big endian)
		 */
		SHORT(Short.class),
		/**
		 * A signed short (32 bits, big endian)
		 */
		INT(Integer.class),
		/**
		 * A signed long (64 bits, big endian)
		 */
		LONG(Long.class),
		/**
		 * A floating point value (32 bits, big endian, IEEE 754-2008, binary32)
		 */
		FLOAT(Float.class),
		/**
		 * A floating point value (64 bits, big endian, IEEE 754-2008, binary64)
		 */
		DOUBLE(Double.class),
		/**
		 * An array of bytes of unspecified format
		 */
		BYTE_ARRAY(Byte[].class),
		/**
		 * An array of bytes defining a string in UTF-8 format.
		 */
		STRING(String.class),
		/**
		 * A sequential list of Identical Unnamed Tags
		 */
		LIST(List.class),
		/**
		 * A sequential list of Named Tags.
		 */
		COMPOUND(Map.class),
		/**
		 * An array of signed shorts (32 bits, big endian)
		 */
		INT_ARRAY(Integer[].class), //		// 
		//		// - everything past this point is non-official
		//		//
		//		// future use? eg, allow maps Integer -> Object
		//		MULTI_COMPOUND(Map.class, 64),
		//		CHARACTER(Character.class, 65),
		//		NULL(null, 66),
		//		BOOLEAN(Boolean.class, 67), // not really needed - just makes easier to cast

					// todo? add a new type for mixed lists?
		/**
		 * For saving only: a class that is of NBT.Compound
		 */
		CUSTOM_COMPOUND(Compound.class, 10), // 127), //		// 
		/**
		 * First of my custom save formats: this allows strings greater than
		 * 65535 bytes long to be stored
		 */
		LONG_STRING(String.class, 65),
		// there are no unsigned bytes in java - values range from -127 to 127
		;
		protected final Class type;
		public final byte value;

		private Tag(Class typ) {
			type = typ;
			value = (byte) ordinal();
		}

		private Tag(Class typ, int b) {
			type = typ;
			value = (byte) b;
		}

		public Class getType() {
			return type;
		}

		public static Tag getTag(byte type) {
			if (type >= 0 && type < values().length) {
				return values()[type];
			} else {
				// ok, long way 'round
				for(Tag t : Tag.values()) {
					if(t.value == type) {
						return t;
					}
				}
			}
			return null;
		}

		public static Tag getType(Object o) {
			if (o != null) {
				Class type = o instanceof Class ? (Class) o : o.getClass();
				if (Map.class.isAssignableFrom(type)) {
					return Tag.COMPOUND;
				} else if (List.class.isAssignableFrom(type)) {
					return Tag.LIST;
				} else if (Byte.class == type || byte.class == type
					|| Boolean.class == type || boolean.class == type) {
					return Tag.BYTE;
				} else if (Short.class == type || short.class == type) {
					return Tag.SHORT;
				} else if (Integer.class == type || int.class == type) {
					return Tag.INT;
				} else if (Long.class == type || long.class == type) {
					return Tag.LONG;
				} else if (Float.class == type || float.class == type) {
					return Tag.FLOAT;
				} else if (Double.class == type || double.class == type) {
					return Tag.DOUBLE;
				} else if (String.class == type) {
					//System.out.println(o.toString().substring(0, Math.min(100, o.toString().length())).replace("\n", ""));
					//return (o.toString()).length() > 65535 ? Tag.LONG_STRING : Tag.STRING;
					//return ((String)o).length() > 65535 ? Tag.LONG_STRING : Tag.STRING;
					return utf_length(o.toString()) > 65535 ? Tag.LONG_STRING : Tag.STRING;
				} else if (Byte[].class == type || byte[].class == type) {
					return Tag.BYTE_ARRAY;
				} else if (Integer[].class == type || int[].class == type) {
					return Tag.INT_ARRAY;
				} // Custom values
				else if (Compound.class.isAssignableFrom(type)) {
					return Tag.CUSTOM_COMPOUND;
				}
			}
			// this could cause problems? 
			// should make sure that lists don't contain null values..
			return null;
		}
	}
	
	static int utf_length(CharSequence sequence) {
		int count = 0;
		for (int i = 0, len = sequence.length(); i < len; ++i) {
			char ch = sequence.charAt(i);
			if (ch <= 0x7F) {
				++count;
			} else if (ch <= 0x7FF) {
				count += 2;
			} else if (Character.isHighSurrogate(ch)) {
				count += 4;
				++i;
			} else {
				count += 3;
			}
		}
		return count;
	}

	public static interface Compound {

		public String[] nbtKeys();

		public Object nbtValue(int keyIndex);

		//public Object nbtValue(String key);
	}

	public static NBTMap load(File toLoad) throws IOException {
		DataInputStream in;
		// test if this is compressed first
		RandomAccessFile file = new RandomAccessFile(toLoad, "r");
		long start = file.getFilePointer();
        // Check header magic
		int b1 = file.read();
		int b2 = file.read();
		if(b1 >= -1 && b2 >= -1 && b1 <= 255 && b2 <= 255 && ((b2 << 8) | b1) == GZIPInputStream.GZIP_MAGIC) {
			// gzip
			file.seek(start);
			in = new DataInputStream(new GZIPInputStream(new RandomInputStream(file)));
		} else {
			// not gzip
			file.seek(start);
			in = new DataInputStream(new RandomInputStream(file));
		}
		// could theoretically load as a list, but in practice, the root should be a map
		NBTMap base = load(in);
		in.close();
		file.close();
		return base;
	}

	public static NBTMap load(DataInputStream in) throws IOException {
		NBTMap root = new NBTMap();
		try {
			byte type = in.readByte();
			// should be a map
			if (type == Tag.COMPOUND.value) {
				// throw out implied name..
				root.setName(in.readUTF());
				while ((type = in.readByte()) != Tag.END.value) {
					root.put(in.readUTF(), loadObject(in, type, 0));

//					final String n = in.readUTF();
//					debug("Loading Map Object <" + Tag.getTag(type) + "> '" + n + "'", 0);
//					root.put(n, loadObject(in, type, 0));
				}
			}
		} catch (EOFException e) {
		}
		return root;
	}

	protected static Object loadObject(DataInputStream in, byte type, int depth) throws IOException {
		if (type == Tag.BYTE.value) {
			return in.readByte();
		} else if (type == Tag.SHORT.value) {
			return in.readShort();
		} else if (type == Tag.INT.value) {
			return in.readInt();
		} else if (type == Tag.LONG.value) {
			return in.readLong();
		} else if (type == Tag.FLOAT.value) {
			return in.readFloat();
		} else if (type == Tag.DOUBLE.value) {
			return in.readDouble();
		} else if (type == Tag.STRING.value) {
			return in.readUTF();
		} else if (type == Tag.LONG_STRING.value) {
			byte[] data = new byte[in.readInt()];
			in.readFully(data);
			return new String(data);
		} else if (type == Tag.BYTE_ARRAY.value) {
			byte[] data = new byte[in.readInt()];
			in.readFully(data);
			return data;
		} else if (type == Tag.INT_ARRAY.value) {
			final int size = in.readInt();
			int[] data = new int[size];
			for (int i = 0; i < size; ++i) {
				data[i] = in.readInt();
			}
			return data;
		} else // at this point, only list and map (recursive-capable types) are left
		if (depth > MAX_DEPTH) {
			throw new RuntimeException("Tried to read NBT tag with too high complexity, depth > " + MAX_DEPTH);
		} else if (type == Tag.LIST.value) {
			type = in.readByte();
			final int size = in.readInt();
			Tag t = Tag.getTag(type);
			NBTList data = t == null || size == 0 ? new NBTList() : new NBTList(t.type);
			if (t == null && size > 0) {
				throw new RuntimeException("Tried to read unknown NBT tag " + type);
			}
//			debug("Loading List<" + t + ">[" + size + "]", depth + 1);
			for (int i = 0; i < size; ++i) {
				data.add(loadObject(in, type, depth + 1));
//				if (type == Tag.LIST.value || type == Tag.COMPOUND.value) {
//					debug("List Object [" + i + "]", depth + 1);
//				} else {
//					debugPrint("List Object [" + i + "]", depth + 1);
//				}
//				Object o = loadObject(in, type, depth + 1);
//				if (type != Tag.LIST.value && type != Tag.COMPOUND.value) {
//					System.out.println("  " + o);
//				}
//				data.add(o);
			}
			return data;
		} else if (type == Tag.COMPOUND.value) {
			NBTMap data = new NBTMap();
			while ((type = in.readByte()) != Tag.END.value) {
				data.put(in.readUTF(), loadObject(in, type, depth + 1));
//				final String n = in.readUTF();
//				if (type == Tag.LIST.value || type == Tag.COMPOUND.value) {
//					debug("Loading Map Object <" + Tag.getTag(type) + "> '" + n + "'", depth + 1);
//				} else {
//					debugPrint("Loading Map Object <" + Tag.getTag(type) + "> '" + n + "'", depth + 1);
//				}
//				Object o = loadObject(in, type, depth + 1);
//				if (type != Tag.LIST.value && type != Tag.COMPOUND.value) {
//					System.out.println("  " + o);
//				}
//				data.put(n, o);
			}
			return data;
		} else {
			throw new RuntimeException("Tried to read unknown NBT tag " + type);
		}
	}

//	private static void debug(String mes, int pad) {
//		StringBuilder b = new StringBuilder();
//		System.out.println(pad(b, pad).append(mes).toString());
//	}
//
//	private static void debugPrint(String mes, int pad) {
//		StringBuilder b = new StringBuilder();
//		System.out.print(pad(b, pad).append(mes).toString());
//	}

	public static void save(Map<String, Object> data, File saveFile) throws IOException {
		DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(saveFile)));
		write(data, out);
		out.close();
	}

	public static void save(Compound data, File saveFile) throws IOException {
		DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(saveFile)));
		out.writeByte(Tag.COMPOUND.value);
		out.writeUTF(data instanceof NBTMap ? ((NBTMap) data).getName() : ""); // the root is unnamed, but has a name string
		// only one root entry to save
		writeObject(out, "", data, 0, false, "");
		// end root map
		out.writeByte(0);
		out.close();
	}

	public static void saveUncompressed(Map<String, Object> data, File saveFile) throws IOException {
		DataOutputStream out = new DataOutputStream(new FileOutputStream(saveFile));
		write(data, out);
		out.close();
	}

	public static void saveUncompressed(Compound data, File saveFile) throws IOException {
		DataOutputStream out = new DataOutputStream(new FileOutputStream(saveFile));
		out.writeByte(Tag.COMPOUND.value);
		out.writeUTF(data instanceof NBTMap ? ((NBTMap) data).getName() : ""); // the root is unnamed, but has a name string
		// only one root entry to save
		writeObject(out, "", data, 0, false, "");
		// end root map
		out.writeByte(0);
		out.close();
	}

	public static void write(Map<String, Object> data, DataOutputStream out) throws IOException {
		// funny, but even though NBT is technically a map,
		//  the format is more like a compound list
		out.writeByte(Tag.COMPOUND.value);
		out.writeUTF(data instanceof NBTMap && ((NBTMap)data).getName() != null ? ((NBTMap) data).getName() : ""); // the root is unnamed, but has a name string

		for (Map.Entry<String, Object> e : data.entrySet()) {
			writeObject(out, e.getKey(), e.getValue(), 0, true, "");
		}
		// end root map
		out.writeByte(0);
	}

	protected static void writeObject(DataOutputStream out, String name, Object obj, int depth, boolean includeTag, String path) throws IOException {
		Tag type = Tag.getType(obj);
		if (type == null) {
			throw new IOException("Tried to save an invalid data type at " + path + "." + name + ": "
				+ (obj == null ? "null" : obj.getClass().getName()) + (obj == null ? "" : " (" + obj.toString() + ")"));
		}
		//System.out.println(path + "." + name + ": " + type);
		if (includeTag) {
			out.writeByte(type.value);
			out.writeUTF(name);
			path += "." + name; // mapped object
		} else {
			path += name; // list object
		}
//		for(int i = 0; i < depth; ++i) {
//			System.out.print("  ");
//		}
//		System.out.println("write " + name + ": " + type);

		if (type == Tag.BYTE) {
			out.writeByte((Byte) (obj instanceof Boolean || (obj != null && obj.getClass() == boolean.class) ? (byte) ((Boolean) obj == true ? 1 : 0) : obj));
		} else if (type == Tag.SHORT) {
			out.writeShort((Short) obj);
		} else if (type == Tag.INT) {
			out.writeInt((Integer) obj);
		} else if (type == Tag.LONG) {
			out.writeLong((Long) obj);
		} else if (type == Tag.FLOAT) {
			out.writeFloat((Float) obj);
		} else if (type == Tag.DOUBLE) {
			out.writeDouble((Double) obj);
		} else if (type == Tag.STRING) {
			out.writeUTF((String) obj);
		} else if (type == Tag.LONG_STRING) {
			final byte[] sb = ((String) obj).getBytes();
			out.writeInt(sb.length);
			out.write(sb);
		} else if (type == Tag.BYTE_ARRAY) {
			byte[] arr = (byte[]) obj;
			out.writeInt(arr.length);
			out.write(arr);
		} else if (type == Tag.INT_ARRAY) {
			int[] arr = (int[]) obj;
			out.writeInt(arr.length);
			for (int i = 0; i < arr.length; ++i) {
				out.writeInt(arr[i]);
			}
		} else // at this point, only list and map (recursive-capable types) are left
		if (depth > MAX_DEPTH) {
			throw new RuntimeException("Tried to write NBT tag with too high complexity, depth > " + MAX_DEPTH);
		} else if (type == Tag.LIST) {
			Tag typ = null, typ2;
			List l = (List) obj;
			int size = l.size();
			// check that all values are the same class (ignore nulls)
			// i have no idea what to do to catch a corrupted LinkedList, so i'll just try to catch it
			try {
				for (Object o : l) {
				}
			} catch (Throwable t) {
				return;
			}
			for (Object o : l) {
				if (o == null) {
					--size;
				} else if (typ == null) {
					typ = Tag.getType(o);
				} else if (typ != (typ2 = Tag.getType(o))) {
					// change string to long_string, if applicable
					if(typ == Tag.STRING && typ2 == Tag.LONG_STRING) {
						typ = typ2;
					} else {
						// todo? add a new type for mixed lists?
						throw new RuntimeException("Cannot Save a Mixed List!");
					}
				}
			}
			out.writeByte(typ != null ? typ.value : Tag.BYTE.value);
			out.writeInt(size);
			int i = 0;
			for (Object o : l) {
				if (o != null) {
					writeObject(out, String.format("[%d]", i++), o, depth + 1, false, path);
//							// (for debugging)
//							includeTag 
//							? String.format("%s.%s[%d]", path, name, i++)
//							: String.format("%s[%d]", path, i++));
				}
			}
		} else if (type == Tag.COMPOUND) {
			Map<String, Object> m = (Map) obj;

			for (Map.Entry e : m.entrySet()) {
				writeObject(out, e.getKey().toString(), e.getValue(), depth + 1, true, path);
			}

			out.writeByte(0); // end tag
		} else if (type == Tag.CUSTOM_COMPOUND) {
			Compound m = (Compound) obj;
			final String[] keys = m.nbtKeys();
			if (keys != null && keys.length > 0) {
				for (int i = 0; i < keys.length; ++i) {
					final Object o = m.nbtValue(i);
					if (o != null) {
						writeObject(out, keys[i], o, depth + 1, true, path);
					}
				}
			}
			out.writeByte(0); // end tag
		}
	}

	public static String debugFile(File toLoad) throws IOException {
		// could theoretically load as a list, but in practice, the root should be a map
		DataInputStream in = new DataInputStream(new GZIPInputStream(new FileInputStream(toLoad)));
//		{
//			DataOutputStream out = new DataOutputStream(new FileOutputStream(new File(toLoad.getAbsolutePath() + "2")));
//			byte wrDat[] = new byte[2048];
//			int len;
//			while ((len = in.read(wrDat)) > 0) {
//				out.write(wrDat, 0, len);
//			}
//			out.close();
//			in.reset();
//		}

		StringBuilder str = new StringBuilder();
		try {
			byte type = in.readByte();
			// should be a map
			if (type == Tag.COMPOUND.value) {
				// throw out implied name..
				in.readUTF();
				str.append("[MAP=ROOT]\n");
				while ((type = in.readByte()) != Tag.END.value) {
					str.append(" [").append(Tag.getTag(type).name()).append("=").append(in.readUTF()).append("] - ");
					debugFileObject(in, str, type, 0);
					str.append("\n");
				}
			}
		} catch (Throwable t) {
			str.append("\n\n").append(t.getClass().getSimpleName()).append(": ").append(t.getMessage());
			//str.append(Str.getStackStr(t));
		}
		in.close();
		return str.append("\n").toString();
	}

	protected static void debugFileObject(DataInputStream in, StringBuilder str, byte type, int depth) throws IOException {
		if (type == Tag.BYTE.value) {
			str.append(String.valueOf(in.readByte()));
		} else if (type == Tag.SHORT.value) {
			str.append(String.valueOf(in.readShort()));
		} else if (type == Tag.INT.value) {
			str.append(String.valueOf(in.readInt()));
		} else if (type == Tag.LONG.value) {
			str.append(String.valueOf(in.readLong()));
		} else if (type == Tag.FLOAT.value) {
			str.append(String.valueOf(in.readFloat()));
		} else if (type == Tag.DOUBLE.value) {
			str.append(String.valueOf(in.readDouble()));
		} else if (type == Tag.LONG_STRING.value) {
			byte[] data = new byte[in.readInt()];
			in.readFully(data);
			str.append(new String(data));
		} else if (type == Tag.STRING.value) {
			str.append(String.valueOf(in.readUTF()));
		} else if (type == Tag.BYTE_ARRAY.value) {
			byte[] data = new byte[in.readInt()];
			in.readFully(data);
			str.append("[").append(data.length).append("]{");
			for (int i = 0; i < data.length; ++i) {
				str.append(data[i]);
				if (i + 1 < data.length) {
					str.append(", ");
				}
			}
			str.append("}");
		} else if (type == Tag.INT_ARRAY.value) {
			final int size = in.readInt();
			str.append("[").append(size).append("]{");
			for (int i = 0; i < size; ++i) {
				str.append(in.readInt());
				if (i + 1 < size) {
					str.append(", ");
				}
			}
			str.append("}");
		} else // at this point, only list and map (recursive-capable types) are left
		if (depth > MAX_DEPTH) {
			throw new RuntimeException("Tried to read NBT tag with too high complexity, depth > " + MAX_DEPTH);
		} else if (type == Tag.LIST.value) {
			type = in.readByte();
			final int size = in.readInt();
			str.append("[").append(size);
			Tag t = Tag.getTag(type);
			if (t == null && size > 0) {
				throw new RuntimeException("Tried to read unknown NBT tag " + type);
			}
			str.append(":").append(t == null ? "?" : t.name()).append("]");
			String spacer = "\n ";
			for (int i = 0; i < depth; ++i) {
				spacer += " ";
			}
			for (int i = 0; i < size; ++i) {
				str.append(spacer);
				str.append("[").append(String.valueOf(i)).append("] ");
				debugFileObject(in, str, type, depth + 1);
			}
		} else if (type == Tag.COMPOUND.value) {
			String spacer = "\n ";
			for (int i = 0; i < depth; ++i) {
				spacer += " ";
			}
			while ((type = in.readByte()) != Tag.END.value) {
				str.append(spacer);
				str.append(" [").append(Tag.getTag(type).name()).append("=").append(in.readUTF()).append("] - ");
				debugFileObject(in, str, type, depth + 1);
			}
		} else {
			throw new RuntimeException("Tried to read unknown NBT tag " + type);
		}
	}

	public static String debugContents(Map<String, Object> data) {
		StringBuilder contents = new StringBuilder();
		contents.append(Tag.COMPOUND.toString()).append(": ").append(data.size()).append(data.size() == 1 ? " entry {\n" : " entries {\n");
		for (Map.Entry<String, Object> e : data.entrySet()) {
			debugContents(contents, e.getKey(), e.getValue(), 1);
		}
		return contents.append("}").toString();
	}

	protected static String debugContents(StringBuilder str, String key, Object obj, int depth) {
		pad(str, depth);
		Tag type = Tag.getType(obj);

		
		if(type == null) {
			str.append("(\"").append(key).append("\"): ").append("NULL");
			return str.append("\n").toString();
		} else if (key == null) {
			str.append(type.toString()).append(": ");
		} else {
			str.append(type.toString()).append("(\"").append(key).append("\"): ");
		}

		// just in case, ensure not going too deep
		if (depth - 1 > MAX_DEPTH) {
			throw new RuntimeException("Tried to write NBT tag with too high complexity, depth > " + MAX_DEPTH);
		}

		switch (type) {
			case END:
				str.delete(str.length() - 3, str.length() - 1);
				break;
			case BYTE:
			case SHORT:
			case INT:
			case LONG:
			case FLOAT:
			case DOUBLE:
			case STRING:
			case LONG_STRING:
				str.append(obj);
				break;
			case BYTE_ARRAY:
				byte[] arr = (byte[]) obj;
				str.append("[").append(arr.length).append("]{");
				for (int i = 0; i < arr.length; ++i) {
					str.append(arr[i]);
					if (i + 1 < arr.length) {
						str.append(", ");
					}
				}
				str.append("}");
				break;
			case INT_ARRAY:
				int[] arr2 = (int[]) obj;
				str.append("[").append(arr2.length).append("]{");
				for (int i = 0; i < arr2.length; ++i) {
					str.append(arr2[i]);
					if (i + 1 < arr2.length) {
						str.append(", ");
					}
				}
				str.append("}");
				break;
			case LIST:
				List l = (List) obj;
				Tag t = (obj instanceof NBTList ? Tag.getType(((NBTList) obj).getType())
					: (!l.isEmpty() ? Tag.getType(l.get(0)) : null));
				str.append(l.size()).append(l.size() == 1 ? " entry of " : " entries of ")
					.append(t == null ? "null" : t.toString()).append(" {\n");
				for (Object o : l) {
					debugContents(str, null, o, depth + 1);
				}
				pad(str, depth);
				str.append("}");
				break;
			case COMPOUND:
				Map<String, Object> m = (Map) obj;
				str.append(m.size()).append(m.size() == 1 ? " entry {\n" : " entries {\n");
				for (Map.Entry<String, Object> e : m.entrySet()) {
					debugContents(str, e.getKey(), e.getValue(), depth + 1);
				}
				pad(str, depth);
				str.append("}");
				break;
			case CUSTOM_COMPOUND: 
				Compound cc = (Compound) obj;
				final String[] keys = cc.nbtKeys();
				if (keys != null && keys.length > 0) {
					str.append(keys.length).append(keys.length == 1 ? " entry {\n" : " entries {\n");
					for (int i = 0; i < keys.length; ++i) {
						final Object o = cc.nbtValue(i);
						if (o != null) {
							debugContents(str, keys[i], o, depth + 1);
						}
					}
					str.append("}");
				} else {
					str.append("{}");
				}
		}

		return str.append("\n").toString();
	}

	private static StringBuilder pad(StringBuilder str, int len) {
		for (int d = 0; d < len; ++d) {
			str.append("  ");
		}
		return str;
	}

	public static void main(String[] args) throws IOException {
		System.out.println(debugFile(new File("data.dat")));
	}

	public static void main2(String[] args) throws IOException {
////		System.out.println(double.class.isAssignableFrom(Double.class));
////		System.out.println(double.class.isAssignableFrom(double.class)); // true
////		System.out.println(Double.class.isAssignableFrom(double.class));
////		System.out.println("-");
////		System.out.println(Double.class.isInstance(double.class));
////		System.out.println(double.class.isInstance(double.class));
////		System.out.println(double.class.isInstance(Double.class));
////		System.out.println("-");
////		System.out.println(double.class.isInstance(0.1));
////		System.out.println(Double.class.isInstance(0.1)); // true
////		System.out.println(double.class.isInstance(0.1F));
////		System.out.println(Double.class.isInstance(0.1F));
////		System.out.println("-");
//		
//		Map m = NBT.load(new File("jascotty2.dat"));
//		System.out.println(debugContents(m));
//	}
		Map<String, Object> testData = new java.util.HashMap<String, Object>();
		testData.put("shortTest", (short) 32767);
		testData.put("longTest", 9223372036854775807L);
		testData.put("floatTest", 0.49823147F);
		testData.put("stringTest", "HELLO WORLD THIS IS A TEST STRING ÅÄÖ!");
		StringBuilder superlong = new StringBuilder();
		for (int i = 0; i < 100000; ++i) {
			superlong.append("1234567890");
		}
		testData.put("superLongString", superlong.toString());
		testData.put("intTest", 2147483647);
		testData.put("listTest", java.util.Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
		testData.put("compoundTest", new java.util.HashMap<String, Object>(testData));
		Map<String, Object> test2 = new java.util.HashMap<String, Object>();
		Map<String, Object> test2_1 = new java.util.HashMap<String, Object>();
		Map<String, Object> test2_2 = new java.util.HashMap<String, Object>(testData);
		test2_1.put("map2", test2_2);
		test2_1.put("strList", Arrays.asList("str1", "str2", "str3", "str4", "str5"));
		Map<String, Object> test2_3 = new java.util.HashMap<String, Object>();
		test2_3.put("3", 3);
		test2_1.put("mapList", Arrays.asList(test2_3, test2_3, test2_3, test2_3));
		test2.put("map1", test2_1);
		List l1 = new ArrayList();
		l1.add(test2_1);
		l1.add(new java.util.HashMap<String, Object>() {
			{
				put("none", new ArrayList());
			}
		});
		l1.add(new java.util.HashMap<String, Object>());
		l1.add(test2_1);
		l1.add(new java.util.HashMap<String, Object>() {
			{
				put("ints", java.util.Arrays.asList(1, 2, 3));
			}
		});
		testData.put("empty", l1);
		testData.put("compoundTest2", test2);
		save(testData, new File("test.nbt"));
		Map m = NBT.load(new File("test.nbt"));
		System.out.println(debugContents(m));
	}
//	public static void main(String[] args) throws IOException {
//		NBT testLoad = new NBT();
//		testLoad.load(new File("jascotty2.nbt"));
//		System.out.println(testLoad.debugContents());
//		
//		
////		DataInputStream in = new DataInputStream(new FileInputStream(new File("jascotty2.nbt")));
////		int len = in.read();
////		byte[] dat = new byte[len];
////		in.readFully(dat);
////		in.close();
////		
////		InputStream is = new ByteArrayInputStream(dat);
////		DataInputStream in2 = new DataInputStream(new GZIPInputStream(is));
////		NBT testLoad2 = new NBT();
////		testLoad2.load(in2);
////		in2.close();
////		in.close();
////		System.out.println(testLoad2.debugContents());
//	}
}
