package me.cmesh.StorageUpgrade;

import java.io.File;

import org.bukkit.plugin.java.JavaPlugin;

public class StorageUpgrade extends JavaPlugin {
	private StorageListener listener;
	public String DataPath;
	protected static StorageUpgrade Instance; 
	
	public StorageUpgrade () {
		Instance = this;
		
		SetupFolder();
		listener = new StorageListener();
	}
	
	public void onEnable() {
		this.getServer().getPluginManager().registerEvents(listener, this);
		listener.StartRunners();
	}
	
	public void onDisable() {
		listener.StopRunners();
	}
	
	private void SetupFolder() {
		File folder = new File(this.getDataFolder().getAbsolutePath());
		
		if (! folder.isDirectory()) {
			if(folder.exists()) {
				folder.delete();
			}
			folder.mkdir();
		}
		DataPath = folder.getAbsolutePath() + File.separator;
	}
}
