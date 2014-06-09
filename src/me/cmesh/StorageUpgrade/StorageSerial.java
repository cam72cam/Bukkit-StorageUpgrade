package me.cmesh.StorageUpgrade;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class StorageSerial {
	
	private static HashMap<Location, StorageItem> storage = new HashMap<Location, StorageItem>();
	
	private static Path GetFile(Location loc) {
		//folder/world-x-y-z.loc
		return Paths.get(String.format("%s%s_%d_%d_%d.loc", StorageUpgrade.Instance.DataPath, loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
	}
	private static Location FromPath(String s) {
		Location loc;
		Scanner scanner = new Scanner (s);
		scanner.useDelimiter("[_.]");
		
		World w = Bukkit.getServer().getWorld(scanner.next());
		int x = scanner.nextInt();
		int y = scanner.nextInt();
		int z = scanner.nextInt();
		
		loc = new Location(w,x,y,z);
		
		scanner.close();
		return loc;
	}
	
	private static StorageItem loadFile(Location loc) {
		try {
			return new StorageItem(Files.readAllBytes(GetFile(loc)));
		} catch (IOException e) {
			//I am lazy
			e.printStackTrace();
			return null;
		}
	}
	private static void saveFile(Location loc, StorageItem item) {
		try {
			Files.write(GetFile(loc), item.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			//I am lazy
			e.printStackTrace();
		}
	}
	public static void RemoveAt(Location loc) {
		File f = GetFile(loc).toFile();
		if (f.isFile()) {
			f.delete();
		}
		storage.remove(loc);
	}
	
	public static void PutAt(Location loc, StorageItem i) {
		storage.put(loc, i);
		saveFile(loc, i);
	}
	
	public static StorageItem GetAt(Location loc) {
		StorageItem item;
		if (!storage.containsKey(loc)) {
			item = loadFile(loc);
			if (item != null) {
				storage.put(loc, item);
			}
		} else {
			item = storage.get(loc);
		}
		
		return item;
	}
	
	public static boolean HasAt(Location loc) {
		return storage.containsKey(loc) || GetFile(loc).toFile().exists();
	}
	
	public static List<Location> GetAllLocations() {
		List<Location> list = new ArrayList<Location>();
		
		File folder = new File(StorageUpgrade.Instance.DataPath);
		for (File fileEntry : folder.listFiles()) {
			list.add(FromPath(fileEntry.getName()));
		}
		
		return list;
	}
}
