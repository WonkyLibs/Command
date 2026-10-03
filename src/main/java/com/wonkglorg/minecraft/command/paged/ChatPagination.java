package com.wonkglorg.minecraft.command.paged;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

@SuppressWarnings("unused")
public abstract class ChatPagination<T>{
	/**
	 * When this pagination was requested
	 */
	@Getter
	protected final long requestTime;
	/**
	 * Function to evaluate entries based on the provided page number
	 */
	private final IntFunction<List<T>> entries;
	/**
	 * If caching is enabled the entries will be stored in the cache instead of being recomputed
	 */
	private final Map<Integer, List<T>> cachedEntries = new ConcurrentHashMap<>();
	/**
	 * The maximum entries being displayed on one page
	 */
	private final int pageSize;
	/**
	 * Current page the pagination is on
	 */
	@Getter
	private int page = 0;
	
	/**
	 * If resulting entries should be cached once computed
	 */
	@Getter
	@Setter
	private boolean cacheEntries = true;
	
	/**
	 * @param pageSize the page size of each page to show
	 * @param entries all entries contained within this pagination
	 */
	protected ChatPagination(int pageSize, List<T> entries) {
		this(pageSize, page -> {
			int from = page * pageSize;
			int to = Math.min(from + pageSize, entries.size());
			return entries.subList(from, to);
		});
	}
	
	/**
	 * @param pageSize the page size of each page to show
	 * @param entries all entries contained within this pagination
	 * @param cacheEntries if entries should be cached right away
	 */
	protected ChatPagination(int pageSize, List<T> entries, boolean cacheEntries) {
		this(pageSize, entries);
		this.cacheEntries = cacheEntries;
		if(cacheEntries){
			IntStream.range(0, Math.ceilDiv(entries.size(), pageSize)).forEach(i -> cachedEntries.put(i, this.entries.apply(i)));
		}
	}
	
	/**
	 * @param pageSize the page size of each page to show
	 * @param entries a lazy populated result of the entries to show on a specific page
	 * @param totalPages the precomputed maximum known page size, a value below 0 means unknown maximum page size
	 */
	protected ChatPagination(int pageSize, IntFunction<List<T>> entries) {
		if(pageSize <= 0){
			throw new IllegalArgumentException("Page size must be greater than 0");
		}
		this.requestTime = System.currentTimeMillis();
		this.pageSize = pageSize;
		this.entries = entries;
	}
	
	/**
	 * Sends the current selected page to the specified audience
	 */
	public void sendToAudience(Audience audience) {
		List<T> pageResults = getEntries(page);
		List<Component> header = pageResults.isEmpty() ? constructHeaderEmpty() : constructHeader();
		if(!header.isEmpty()){
			header.forEach(audience::sendMessage);
		}
		
		int entryCount = page * pageSize;
		for(var entry : pageResults){
			List<Component> message = constructEntry(entryCount++, entry);
			if(message.isEmpty()){
				continue;
			}
			message.forEach(audience::sendMessage);
		}
		
		List<Component> footer = pageResults.isEmpty() ? constructFooterEmpty() : constructFooter();
		if(!footer.isEmpty()){
			footer.forEach(audience::sendMessage);
		}
		
		List<Component> pageControls = pageControls();
		if(!pageControls.isEmpty()){
			pageControls.forEach(audience::sendMessage);
		}
	}
	
	/**
	 * Header when data is available
	 */
	protected abstract @NotNull List<Component> constructHeader();
	
	/**
	 * Header when no data is available
	 */
	protected abstract @NotNull List<Component> constructHeaderEmpty();
	
	/**
	 * Runs for each entry that should be rendered per page
	 *
	 * @param entryCount which index this entry is (offset from the first entry in the whole data set)
	 * @param entry the entry at this position
	 */
	protected abstract @NotNull List<Component> constructEntry(int entryCount, T entry);
	
	/**
	 * Footer when data is available, rendered before the page controls
	 */
	protected abstract @NotNull List<Component> constructFooter();
	
	/**
	 * Footer when no data is available, rendered before the page controls
	 */
	protected abstract @NotNull List<Component> constructFooterEmpty();
	
	/**
	 * Page Controls to send as the last part of a page
	 */
	protected abstract @NotNull List<Component> pageControls();
	
	/**
	 * Retrieves a page, using the cache if enabled.
	 */
	private List<T> getEntries(int page) {
		if(cacheEntries){
			return cachedEntries.computeIfAbsent(page, p -> List.copyOf(entries.apply(p)));
		}
		
		return entries.apply(page);
	}
	
	public void setPage(int page) {
		if(page < 0){
			this.page = 0;
			return;
		}
		
		if(!getEntries(page).isEmpty()){
			this.page = page;
		}
	}
	
	public void nextPage() {
		if(hasNextPage()){
			page++;
		}
	}
	
	/**
	 * swaps to the previous page
	 */
	public void prevPage() {
		page = Math.max(page - 1, 0);
	}
	
	/**
	 * If a next page is available to be loaded
	 */
	public boolean hasNextPage() {
		return !getEntries(page + 1).isEmpty();
	}
	
	/**
	 * @return returns the 1 indexed page for user display
	 */
	public int getPageDisplay() {
		return page + 1;
	}
}
