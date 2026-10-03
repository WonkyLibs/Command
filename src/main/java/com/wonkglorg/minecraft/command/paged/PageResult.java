package com.wonkglorg.minecraft.command.paged;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

/**
 *
 * @param entries entries contained on this page
 * @param hasNext if more entries exist to fill another page
 * @param <T>
 */
public record PageResult<T>(List<T> entries, boolean hasNext) implements Iterable<T>{
	public boolean hasEntries() {
		return !entries.isEmpty();
	}
	
	@Override
	public @NotNull Iterator<T> iterator() {
		return entries.iterator();
	}
}