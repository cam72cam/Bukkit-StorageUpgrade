package me.cmesh.StorageUpgrade;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

public class StorageItem {
	private ItemStack item;
	private long count;
	
	public StorageItem(ItemStack item, long count) {
		this.item = item.clone();
		this.count = count;
	}
	public StorageItem(byte[] data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            
            this.count = dataInput.readLong();
            this.item = (ItemStack) dataInput.readObject();
            
		    dataInput.close();
		} catch (ClassNotFoundException | IOException e) {
		    throw new IllegalStateException("Unable to decode class type.", e);
	    }
	}
	public byte[] toByteArray() {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            
            dataOutput.writeLong(count);
            dataOutput.writeObject(item);
            
            dataOutput.close();
            
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }
	
	public void Add(long count) {
		this.count += count;
	}
	
	public long Count() {
		return count;
	}
	
	public ItemStack Item() {
		return item.clone();
	}
	public boolean IsType(ItemStack type) {
		return type.getType() == item.getType();
		//TODO check other stuff
	}
	public String toString() {
		return String.format("%s: %s", item.getType(), count);
	}
}