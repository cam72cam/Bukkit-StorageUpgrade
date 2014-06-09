package me.cmesh.StorageUpgrade;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;

public class StorageListener implements Listener {
	@EventHandler(priority = EventPriority.LOW)
	public void onBreakBlock(BlockBreakEvent event) {
		Block block = event.getBlock();
		Location loc = block.getLocation();
		World w = loc.getWorld();
		if(block.getType() == Material.DIAMOND_BLOCK && StorageSerial.HasAt(loc)) {
			StorageItem items = StorageSerial.GetAt(loc);

			ItemStack drop = items.Item();
			drop.setAmount(64);
			for (long total = items.Count(); total > 64; total -= 64) {
				w.dropItem(loc, drop);
			}
			
			if (items.Count()%64 != 0) {
				drop.setAmount((int) (items.Count()%64));
				w.dropItem(loc, drop);
			}
			StopRunner(loc);
			StorageSerial.RemoveAt(loc);
		}
	}
	
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerRightClick(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Block block = event.getClickedBlock();
		if (block == null) {
			return;
		}
		
		Location loc = block.getLocation();
		ItemStack inhand = player.getItemInHand();
		if (block.getType() == Material.DIAMOND_BLOCK) {
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK)
			{
				//Add singe stack
				if (inhand.getType() != Material.AIR) {
					StorageItem current;
					boolean newstorage = false;
					if (player.isSneaking() && !StorageSerial.HasAt(loc)) {
						//Set type
						current = new StorageItem(inhand, player.getItemInHand().getAmount());
						newstorage = true;
					} else if (!player.isSneaking()) {
						//Add to
						current = StorageSerial.GetAt(loc);
						if (!current.IsType(inhand)) {
							//Wrong type
							return;
						}
						current.Add(player.getItemInHand().getAmount());
					} else {
						return;
					}
					
					event.setCancelled(true);
					player.setItemInHand(null);
					StorageSerial.PutAt(block.getLocation(), current);
					if (newstorage) {
						StartRunner(loc);
					}
					player.sendMessage(current.toString());
				} else if (StorageSerial.HasAt(loc) && player.isSneaking()) {
					StorageItem current = StorageSerial.GetAt(loc);
					for (ItemStack item : player.getInventory().getContents()) {
						if (item == null) {
							continue;
						}
						if (current.IsType(item)) {
							current.Add(item.getAmount());
						}
					}
					
					StorageSerial.PutAt(block.getLocation(), current);
					player.getInventory().remove(current.Item().getType());//RemoveAll
					player.sendMessage(current.toString());
				} else if (StorageSerial.HasAt(loc)) {
					StorageItem current = StorageSerial.GetAt(loc);
					player.sendMessage(current.toString());
				}
			}
			else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
				if (StorageSerial.HasAt(block.getLocation())) {
					StorageItem current = StorageSerial.GetAt(block.getLocation());
					long count = Math.min(current.Count(), 64);
					if (count != 0) {
						ItemStack item = current.Item();
						item.setAmount((int) count);
						player.getLocation().getWorld().dropItem(block.getLocation(), item);
						current.Add(-count);
						player.sendMessage(current.toString());
					}
				}
			}
		} 
	}

	private static final BlockFace[] hopperMap = {
		BlockFace.DOWN,		//0
		BlockFace.SELF, 	//none
		BlockFace.NORTH,	//2
		BlockFace.SOUTH,	//3
		BlockFace.WEST,		//4
		BlockFace.EAST,		//5
	};
	private Block getHopperBlock(Hopper h) {
		int i = h.getData().toString().charAt(7) - '0';
		if (i <= 5) {
			return h.getBlock().getRelative(hopperMap[i]);
		} else {
			//Something strange happened, I keep getting the number 8  when I look inside a chest above a hopper?
			return h.getBlock();
		}
	}
	
	private static final BlockFace[] surrounding = {
		BlockFace.UP,
		BlockFace.NORTH,
		BlockFace.EAST,
		BlockFace.SOUTH,
		BlockFace.WEST,
	};
	
	private class InventoryUpdater implements Runnable {
		private Location loc;
		private int task;
		
		public InventoryUpdater(Location loc) {
			this.loc = loc;
			
			BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
			task = scheduler.scheduleSyncRepeatingTask(StorageUpgrade.Instance, this, 0l, 1l);
		}
		
		public void Cancel() {
			BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
			scheduler.cancelTask(task);
		}
		
		@Override
		public void run() {

			StorageItem current = StorageSerial.GetAt(loc);
			long count = Math.min(current.Count(), 64);
			long leftover = 0;
			
			/*
			 * From enderchest to hopper
			 */
			if (count != 0) {
				Block down = loc.getBlock().getRelative(BlockFace.DOWN);
				if (down.getType() == Material.HOPPER) {
					Hopper h = (Hopper)down.getState();
					HashMap<Integer, ItemStack> res = h.getInventory().addItem(new ItemStack(current.Item().getType(), (int)count));
					for (ItemStack stack : res.values()) {
						if (stack != null) {
							leftover = stack.getAmount();
						}
					}
				}
				current.Add(leftover - count);
			}
			
			
			/*
			 * From Hopper to enderchest
			 */
			for (BlockFace face : surrounding) {
				Block block = loc.getBlock().getRelative(face);
				if (block.getType() == Material.HOPPER) {
					Hopper h = (Hopper)block.getState();
					Block output = getHopperBlock(h);
					if (output.getLocation().equals(loc)) {
						for (ItemStack item : h.getInventory()) {
							if (item == null) {
								continue;
							}
							if (current.IsType(item)) {
								current.Add(item.getAmount());
							}
						}
						
						h.getInventory().remove(current.Item().getType());//RemoveAll
					}
				}
			}
			
			StorageSerial.PutAt(loc, current);
		}
	}
	
	
	private HashMap<Location, InventoryUpdater> runners;
	
	public void StartRunners() {
		runners = new HashMap<Location, InventoryUpdater>();
		for(Location loc : StorageSerial.GetAllLocations()) {
			StartRunner(loc);
		}
	}
	public void StopRunners() {
		for(InventoryUpdater inv : runners.values()) {
			inv.Cancel();
		}
		runners = null;
	}
	
	private void StartRunner(Location loc) {
		runners.put(loc, new InventoryUpdater(loc));
	}
	private void StopRunner(Location loc) {
		if (runners.containsKey(loc)) {
			runners.get(loc).Cancel();
			runners.remove(loc);
		}
	}
}
