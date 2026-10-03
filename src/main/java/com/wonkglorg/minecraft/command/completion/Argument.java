package com.wonkglorg.minecraft.command.completion;

import lombok.Getter;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

public class Argument{
	@Getter
	private String argumentName;
	@Getter
	private boolean required;
	private Supplier<Collection<String>> suggestions;
	
	public Argument(String argumentName, boolean required, Supplier<Collection<String>> suggestions) {
		this.argumentName = argumentName;
		this.required = required;
		this.suggestions = suggestions;
	}
	
	public Argument(String argumentName, boolean required) {
		this.argumentName = argumentName;
		this.required = required;
		suggestions = Set::of;
	}
	
	public Collection<String> getSuggestions() {
		return suggestions.get();
	}
}
