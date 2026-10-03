package com.wonkglorg.minecraft.command.paged;

import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;

@SuppressWarnings("unused")
public abstract class ChatPagination<T>{
	
	/**
	 * When this pagination was requested.
	 */
	@Getter
	protected final long requestTime;
	
	/**
	 * Function to evaluate entries based on the provided page number.
	 */
	private final BiFunction<ChatPagination<T>, Integer, CompletableFuture<PageResult<T>>> entries;
	
	/**
	 * Cached page requests, including requests that are still loading.
	 */
	private final Map<Integer, CompletableFuture<PageResult<T>>> cachedEntries = new ConcurrentHashMap<>();
	
	/**
	 * The currently loaded page result.
	 */
	private volatile PageResult<T> currentPage;
	
	/**
	 * The page number associated with currentPage.
	 */
	private volatile int loadedPage = -1;
	
	/**
	 * The maximum entries being displayed on one page.
	 */
	private final int pageSize;
	
	/**
	 * Current requested page.
	 */
	@Getter
	private volatile int page = 0;
	
	/**
	 * Whether resulting entries should be cached.
	 */
	@Getter
	private boolean isCached = true;
	
	/**
	 * Whether the pagination is currently loading page data.
	 */
	@Getter
	private volatile boolean loadingPageData = false;
	
	/**
	 * Identifies the most recent page-loading request.
	 * Prevents older requests from overwriting newer ones.
	 */
	private final AtomicLong loadGeneration = new AtomicLong();
	
	protected ChatPagination(int pageSize, List<T> entries) {
		this(pageSize, entries, true);
	}
	
	protected ChatPagination(int pageSize, List<T> entries, boolean cacheEntries) {
		this(pageSize, ((pagination, providedPage) -> {
			int from = Math.min(providedPage * pageSize, entries.size());
			int to = Math.min(from + pageSize, entries.size());
			
			return CompletableFuture.completedFuture(new PageResult<>(entries.subList(from, to), to < entries.size()));
		}));
		
		this.isCached = cacheEntries;
		
		if(cacheEntries){
			int pageCount = Math.ceilDiv(entries.size(), pageSize);
			
			for(int i = 0; i < pageCount; i++){
				int from = i * pageSize;
				int to = Math.min(from + pageSize, entries.size());
				
				cachedEntries.put(i, CompletableFuture.completedFuture(new PageResult<>(entries.subList(from, to), to < entries.size())));
			}
		}
	}
	
	protected ChatPagination(int pageSize, BiFunction<ChatPagination<T>, Integer, CompletableFuture<PageResult<T>>> entries) {
		if(pageSize <= 0){
			throw new IllegalArgumentException("Page size must be greater than 0");
		}
		
		this.requestTime = System.currentTimeMillis();
		this.pageSize = pageSize;
		this.entries = entries;
	}
	
	/**
	 * Changes the requested page and loads its data.
	 */
	public CompletableFuture<Void> setPage(int page) {
		this.page = Math.max(0, page);
		return loadPage();
	}
	
	/**
	 * Changes to the next page if one is available.
	 */
	public CompletableFuture<Void> nextPage() {
		if(!hasNextPage()){
			return CompletableFuture.completedFuture(null);
		}
		
		return setPage(page + 1);
	}
	
	/**
	 * Changes to the previous page.
	 */
	public CompletableFuture<Void> prevPage() {
		return setPage(page - 1);
	}
	
	/**
	 * Loads the currently requested page.
	 */
	private CompletableFuture<Void> loadPage() {
		int requestedPage = page;
		long generation = loadGeneration.incrementAndGet();
		
		loadingPageData = true;
		
		return getEntries(requestedPage).thenAccept(result -> {
			if(loadGeneration.get() != generation){
				return;
			}
			
			currentPage = result;
			loadedPage = requestedPage;
			
		}).whenComplete((result, error) -> {
			if(loadGeneration.get() == generation){
				loadingPageData = false;
			}
		});
	}
	
	/**
	 * Retrieves a page, using the cache if enabled.
	 */
	private CompletableFuture<PageResult<T>> getEntries(int requestedPage) {
		if(!isCached){
			return entries.apply(this, requestedPage);
		}
		
		CompletableFuture<PageResult<T>> cached = cachedEntries.get(requestedPage);
		
		if(cached != null){
			return cached;
		}
		
		// Insert a promise first to prevent duplicate requests.
		CompletableFuture<PageResult<T>> promise = new CompletableFuture<>();
		
		cached = cachedEntries.putIfAbsent(requestedPage, promise);
		
		if(cached != null){
			return cached;
		}
		
		try{
			entries.apply(this, requestedPage).whenComplete((result, error) -> {
				if(error != null){
					cachedEntries.remove(requestedPage, promise);
					promise.completeExceptionally(error);
				} else {
					promise.complete(result);
				}
			});
		} catch(Throwable error){
			cachedEntries.remove(requestedPage, promise);
			promise.completeExceptionally(error);
		}
		
		return promise;
	}
	
	/**
	 * Sends the currently loaded page to the specified audience.
	 */
	public void sendToAudience(Audience audience) {
		PageResult<T> result = currentPage;
		int displayedPage = page;
		
		if(loadingPageData || result == null || loadedPage != displayedPage){
			return;
		}
		
		List<Component> header = result.hasEntries() ? constructHeader() : constructHeaderEmpty();
		
		header.forEach(audience::sendMessage);
		
		int entryCount = displayedPage * pageSize;
		
		for(T entry : result.entries()){
			List<Component> messages = constructEntry(entryCount++, entry);
			messages.forEach(audience::sendMessage);
		}
		
		List<Component> footer = result.hasEntries() ? constructFooter() : constructFooterEmpty();
		
		footer.forEach(audience::sendMessage);
		
		pageControls().forEach(audience::sendMessage);
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
	 * Whether a next page is available.
	 */
	public boolean hasNextPage() {
		return currentPage != null && loadedPage == page && !loadingPageData && currentPage.hasNext();
	}
	
	/**
	 * Returns the 1-indexed page for user display.
	 */
	public int getPageDisplay() {
		return page + 1;
	}
}