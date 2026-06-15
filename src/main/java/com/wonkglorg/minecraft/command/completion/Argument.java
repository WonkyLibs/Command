package com.wonkglorg.minecraft.command.completion;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

public class Argument{
	private String argumentName;
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
	
	public String getArgumentName() {
		return argumentName;
	}
	
	public boolean isRequired() {
		return required;
	}
	
	public Collection<String> getSuggestions() {
		return suggestions.get();
	}
}
