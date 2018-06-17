package me.jascotty2.libv3.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NBTMap extends HashMap<String, Object> {
	protected String name;
//
//	public NBTMap(String name) {
//		this.name = name;
//	}
//	
//	public NBTMap(String name, Map map) {
//		super(map);
//		this.name = name;
//	}
//	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name == null ? "" : name;
	}
	public String getString(String key) {
		final Object o = get(key);
		return o instanceof String ? (String) o : null;
	}
	public String getString(String key, String def) {
		final Object o = get(key);
		return o instanceof String ? (String) o : def;
	}
	
	public Byte getByte(String key) {
		final Object o = get(key);
		return o instanceof Byte ? (Byte) o : null;
	}
	public Byte getByte(String key, Byte def) {
		final Object o = get(key);
		return o instanceof Byte ? (Byte) o : def;
	}
	
	public Boolean getBoolean(String key) {
		final Object o = get(key);
		return o instanceof Byte ? ((Byte) o) != 0 : null;
	}
	public Boolean getBoolean(String key, Boolean def) {
		final Object o = get(key);
		return o instanceof Byte ? ((Byte) o) != 0 : def;
	}
	
	public Short getShort(String key) {
		final Object o = get(key);
		return o instanceof Short ? (Short) o : null;
	}
	public Short getShort(String key, Short def) {
		final Object o = get(key);
		return o instanceof Short ? (Short) o : def;
	}
	
	public Integer getInteger(String key) {
		final Object o = get(key);
		return o instanceof Integer ? (Integer) o : null;
	}
	public Integer getInteger(String key, Integer def) {
		final Object o = get(key);
		return o instanceof Integer ? (Integer) o : def;
	}
	
	public Long getLong(String key) {
		final Object o = get(key);
		return o instanceof Long ? (Long) o : null;
	}
	public Long getLong(String key, Long def) {
		final Object o = get(key);
		return o instanceof Long ? (Long) o : def;
	}
	
	public Float getFloat(String key) {
		final Object o = get(key);
		return o instanceof Float ? (Float) o : null;
	}
	public Float getFloat(String key, Float def) {
		final Object o = get(key);
		return o instanceof Float ? (Float) o : def;
	}
	
	public Double getDouble(String key) {
		final Object o = get(key);
		return o instanceof Double ? (Double) o : null;
	}
	public Double getDouble(String key, Double def) {
		final Object o = get(key);
		return o instanceof Double ? (Double) o : def;
	}
	
	public Byte[] getByteArray(String key) {
		final Object o = get(key);
		return o instanceof Byte[] ? (Byte[]) o : null;
	}
	public Byte[] getByteArray(String key, Byte[] def) {
		final Object o = get(key);
		return o instanceof Byte[] ? (Byte[]) o : def;
	}
	
	public Integer[] getIntegerArray(String key) {
		final Object o = get(key);
		return o instanceof Integer[] ? (Integer[]) o : null;
	}
	public Integer[] getIntegerArray(String key, Integer[] def) {
		final Object o = get(key);
		return o instanceof Integer[] ? (Integer[]) o : def;
	}
	
	public List getList(String key) {
		final Object o = get(key);
		return o instanceof List ? (List) o : null;
	}
	public List getList(String key, List def) {
		final Object o = get(key);
		return o instanceof List ? (List) o : def;
	}
	
	public NBTList getNBTList(String key) {
		final Object o = get(key);
		return o instanceof NBTList ? (NBTList) o : null;
	}
	public NBTList getNBTList(String key, NBTList def) {
		final Object o = get(key);
		return o instanceof NBTList ? (NBTList) o : def;
	}
	
	public Map getMap(String key) {
		final Object o = get(key);
		return o instanceof Map ? (Map) o : null;
	}
	public Map getMap(String key, Map def) {
		final Object o = get(key);
		return o instanceof Map ? (Map) o : def;
	}
	
	public NBTMap getNBTMap(String key) {
		final Object o = get(key);
		return o instanceof NBTMap ? (NBTMap) o : null;
	}
	public NBTMap getNBTMap(String key, NBTMap def) {
		final Object o = get(key);
		return o instanceof NBTMap ? (NBTMap) o : def;
	}
	
}
