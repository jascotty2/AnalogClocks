package me.jascotty2.libv3.util;

import java.util.Collection;
import java.util.LinkedList;
import me.jascotty2.libv3.io.NBT.Tag;

public class NBTList extends LinkedList<Object> {

	private Class type = null;
	private Tag t = null;
	private boolean enforce = false;

	public NBTList() {
	}
	
	public NBTList(Class type) {
		if(type == null) {
			throw new IllegalArgumentException("Class cannot be null!");
		}
		enforce = true;
		this.type = type;
	}
	
	public Class getType() {
		return type;
	}
	
	public Tag getTagType() {
		return t != null ? t : (type == null ? null : (t = Tag.getType(type)));
	}

	@Override
	public void addFirst(Object e) {
		validate(e);
		super.addFirst(e);
	}

	@Override
	public void addLast(Object e) {
		validate(e);
		super.addLast(e);
	}

	@Override
	public boolean add(Object e) {
		validate(e);
		return super.add(e);
	}

	@Override
	public boolean addAll(Collection<? extends Object> clctn) {
		for (Object o : clctn) {
			validate(o);
		}
		return super.addAll(clctn);
	}

	@Override
	public boolean addAll(int i, Collection<? extends Object> clctn) {
		for (Object o : clctn) {
			validate(o);
		}
		return super.addAll(i, clctn);
	}

	@Override
	public Object set(int i, Object e) {
		validate(e);
		return super.set(i, e);
	}

	@Override
	public void add(int i, Object e) {
		validate(e);
		super.add(i, e);
	}

	@Override
	public void push(Object e) {
		validate(e);
		super.push(e);
	}

	protected void validate(Object o) {
		if(o == null || ((!isEmpty() || enforce) && (o.getClass() != type && !type.isAssignableFrom(o.getClass())))) {
			throw new IllegalArgumentException(String.format("This list can only contain type %s (Tried to add %s '%s')", 
					type.getSimpleName(), o == null ? "null" : o.getClass().getSimpleName(), o == null ? "" : o.toString()));
		} else if(isEmpty() && !enforce) {
			type = o.getClass();
			t = null;
		}
	}
}
