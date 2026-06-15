package com.wonkglorg.minecraft.command.completion;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import static com.wonkglorg.minecraft.command.AbstractCommand.getArgument;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public class ArgumentBuilder{
	private static final String ARGUMENT_NAME = "args";
	private String missingArgumentsMessage = "<red>Missing required arguments %s!";
	private final Map<String, Argument> arguments = new HashMap<>();
	private final BiFunction<CommandContext<CommandSourceStack>, Map<String, String>, Integer> execute;
	
	public ArgumentBuilder(BiFunction<CommandContext<CommandSourceStack>, Map<String, String>, Integer> execute) {
		this.execute = execute;
	}
	
	public void addArgument(Argument argument) {
		arguments.put(argument.getArgumentName(), argument);
	}
	
	public RequiredArgumentBuilder<CommandSourceStack, String> constructArguments() {
		return Commands.argument(ARGUMENT_NAME, StringArgumentType.greedyString()).suggests(this::suggest).executes(this::execute);
	}
	
	public String getMissingArgumentsMessage() {
		return missingArgumentsMessage;
	}
	
	public void setMissingArgumentsMessage(String missingArgumentsMessage) {
		this.missingArgumentsMessage = missingArgumentsMessage;
	}
	
	private CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
		String input = builder.getRemaining().toLowerCase();
		
		boolean endsWithSpace = input.endsWith(" ");
		
		String[] parts = input.split(" ");
		String current;
		
		if(endsWithSpace){
			current = "";
		} else {
			current = parts.length == 0 ? "" : parts[parts.length - 1];
		}
		
		Set<String> usedKeys = getUsedArguments(input);
		
		String fullInput = builder.getInput();
		int lastSpace = fullInput.lastIndexOf(' ');
		
		int offset;
		if(endsWithSpace){
			offset = fullInput.length();
		} else {
			offset = lastSpace == -1 ? builder.getStart() : lastSpace + 1;
		}
		
		builder = builder.createOffset(offset);
		
		if(!current.contains(":")){
			suggestKeys(builder, current, usedKeys);
			return builder.buildFuture();
		}
		
		String[] kv = current.split(":", 2);
		String key = kv[0];
		String value = kv.length > 1 ? kv[1] : "";
		
		Argument argument = arguments.get(key);
		if(argument != null){
			suggest(builder, argument, value);
		}
		
		return builder.buildFuture();
	}
	
	private void suggest(SuggestionsBuilder builder, Argument argument, String value) {
		for(var suggest : argument.getSuggestions()){
			if(suggest.toLowerCase().startsWith(value)){
				builder.suggest(argument.getArgumentName() + ":" + suggest);
			}
		}
	}
	
	private void suggestKeys(SuggestionsBuilder builder, String current, Set<String> usedKeys) {
		
		for(String key : arguments.keySet()){
			if(usedKeys.contains(key)) continue;
			
			if(key.startsWith(current)){
				builder.suggest(key + ":");
			}
		}
	}
	
	private Set<String> getUsedArguments(String input) {
		Set<String> used = new HashSet<>();
		
		if(input == null || input.isEmpty()) return used;
		
		String[] parts = input.split(" ");
		
		for(String part : parts){
			int idx = part.indexOf(':');
			if(idx > 0){
				used.add(part.substring(0, idx).toLowerCase());
			}
		}
		return used;
	}
	
	/**
	 * Parses Arguments
	 *
	 * @return map of keys and their assigned value
	 */
	public Map<String, String> parseArgs(CommandContext<CommandSourceStack> ctx) {
		return parseArgs(getArgument(ctx, ARGUMENT_NAME, String.class, null));
	}
	
	/**
	 * Parses Arguments
	 *
	 * @return map of keys and their assigned value
	 */
	public Map<String, String> parseArgs(String args) {
		Map<String, String> map = new HashMap<>();
		
		if(args == null || args.isEmpty()) return map;
		
		String[] parts = args.split(" ");
		
		for(String part : parts){
			if(!part.contains(":")) continue;
			
			String[] kv = part.split(":", 2);
			map.put(kv[0].toLowerCase(), kv[1]);
		}
		
		return map;
	}
	
	private int execute(CommandContext<CommandSourceStack> ctx) {
		if(execute == null){
			return -1;
		}
		String raw = getArgument(ctx, ARGUMENT_NAME, String.class, null);
		Map<String, String> args = parseArgs(raw);
		
		Set<String> usedArgs = getUsedArguments(raw);
		
		List<String> required = arguments.values()
										 .stream()
										 .filter(argument -> !usedArgs.contains(argument.getArgumentName()))
										 .filter(Argument::isRequired)
										 .map(Argument::getArgumentName)
										 .toList();
		
		if(!required.isEmpty()){
			ctx.getSource().getSender().sendMessage(miniMessage().deserialize(missingArgumentsMessage.formatted(String.join(", ", required))));
		}
		
		return execute.apply(ctx, args);
	}
	
}
