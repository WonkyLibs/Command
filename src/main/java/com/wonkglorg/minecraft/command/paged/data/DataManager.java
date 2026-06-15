package com.wonkglorg.minecraft.command.paged.data;

import org.bukkit.Bukkit;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager<T>{
	private final Map<UUID, DataHolder<T>> userData = new ConcurrentHashMap<>();
	
	/**
	 * How long to keep the entries before they count as "expired"
	 */
	private final long timeBeforeExpiry;
	
	public DataManager(long timeBeforeExpiry) {
		this.timeBeforeExpiry = timeBeforeExpiry;
	}
	
	private void verifyEntries() {
		if(userData.isEmpty()) return;
		userData.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
		if(timeBeforeExpiry == -1){
			return;
		}
		userData.values().removeIf(d -> d.getLastModified() < System.currentTimeMillis() - timeBeforeExpiry);
	}
	
	public void add(UUID user, T data) {
		verifyEntries();
		userData.put(user, new DataHolder<>(data));
	}
	
	/**
	 *
	 * @param user the user to lookup
	 * @return the newest entry of the user
	 */
	public Optional<T> get(UUID user) {
		verifyEntries();
		DataHolder<T> dataHolder = userData.getOrDefault(user, null);
		if(dataHolder == null){
			return Optional.empty();
		}
		return Optional.ofNullable(dataHolder.getData());
	}
}
