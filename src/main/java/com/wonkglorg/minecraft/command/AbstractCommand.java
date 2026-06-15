package com.wonkglorg.minecraft.command;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Abstract command to provide some helper functions
 *
 */
@SuppressWarnings("unused")
public abstract class AbstractCommand{
	private LiteralCommandNode<CommandSourceStack> command;
	
	public abstract LiteralArgumentBuilder<CommandSourceStack> argumentBuilder();
	
	public Set<String> aliases() {
		return Set.of();
	}
	
	public LiteralCommandNode<CommandSourceStack> getCommand() {
		if(command == null){
			command = argumentBuilder().build();
		}
		return command;
	}
	
	public void register(ReloadableRegistrarEvent<Commands> registrar) {
		LiteralCommandNode<CommandSourceStack> node = getCommand();
		registrar.registrar().register(node);
		for(var alias : aliases()){
			registrar.registrar().register(literal(alias).executes(node.getCommand()).redirect(node).build());
		}
	}
	
	/**
	 * @param permission the permission the sender needs
	 */
	public static Predicate<CommandSourceStack> permissions(String... permission) {
		return c -> {
			if(permission.length == 0) return true;
			return Arrays.stream(permission).allMatch(p -> c.getSender().hasPermission(p));
		};
	}
	
	/**
	 * Returns elements from the provided list based on the current users input
	 *
	 * @param collection the set of completion options
	 * @param <U> the source context
	 * @return suggestion provider
	 */
	public static <U> SuggestionProvider<U> suggestMatching(Collection<String> collection) {
		return (ctx, builder) -> suggestMatching(builder, collection).buildFuture();
	}
	
	/**
	 * Returns elements from the provided list based on the current users input
	 *
	 * @param collection the set of completion options
	 * @param <U> the source context
	 * @return suggestion provider
	 */
	public static <U> SuggestionProvider<U> suggestMatching(String[] collection) {
		return (ctx, builder) -> suggestMatching(builder, collection).buildFuture();
	}
	
	/**
	 * Returns elements from the provided list based on the current users input
	 *
	 * @param collection the set of completion options
	 * @param toString the string function for these
	 * @param <T> value of elements
	 * @param <U> the source context
	 * @return suggestion provider
	 */
	public static <T, U> SuggestionProvider<U> suggestMatching(Collection<T> collection, Function<T, String> toString) {
		return (ctx, builder) -> suggestMatching(builder, collection, toString).buildFuture();
	}
	
	/**
	 * Returns elements from the provided list based on the current users input
	 *
	 * @param collection the set of completion options
	 * @param toString the string function for these
	 * @param <T> value of elements
	 * @param <U> the source context
	 * @return suggestion provider
	 */
	public static <T, U> SuggestionProvider<U> suggestMatching(T[] collection, Function<T, String> toString) {
		return (ctx, builder) -> suggestMatching(builder, collection, toString).buildFuture();
	}
	
	/**
	 * Returns elements from the provided list based on the current users input
	 *
	 * @param collection the set function of completion options
	 * @param toString the string function for these
	 * @param <T> value of elements
	 * @param <U> the source context
	 * @return suggestion provider
	 */
	public static <T, U> SuggestionProvider<U> suggestMatching(Function<CommandContext<U>, Collection<T>> collection, Function<T, String> toString) {
		return (ctx, builder) -> suggestMatching(builder, collection.apply(ctx), toString).buildFuture();
	}
	
	/**
	 * Returns elements from the provided list based on the current users input
	 *
	 * @param collection the set of completion options
	 * @return suggestion provider
	 */
	public static SuggestionsBuilder suggestMatching(SuggestionsBuilder builder, Collection<String> collection) {
		//@formatter:off
		collection.stream()
				  .filter(Objects::nonNull)
				  .filter(v -> v.toLowerCase().startsWith(builder.getRemainingLowerCase()))
				  .forEach(builder::suggest);
		//@formatter:on
		return builder;
	}
	
	/**
	 * Returns elements from the provided list based on the current users input
	 *
	 * @param collection the set of completion options
	 * @return suggestion provider
	 */
	public static SuggestionsBuilder suggestMatching(SuggestionsBuilder builder, String[] collection) {
		//@formatter:off
		Arrays.stream(collection)
				  .filter(Objects::nonNull)
				  .filter(v -> v.toLowerCase().startsWith(builder.getRemainingLowerCase()))
				  .forEach(builder::suggest);
		//@formatter:on
		return builder;
	}
	
	/**
	 * Returns elements from the provided list based on the current users input
	 *
	 * @param builder the suggestionBuilder to fill
	 * @param collection the set of completion options
	 * @param toString the string function for these
	 * @param <T> value of elements
	 * @return suggestion provider
	 */
	public static <T> SuggestionsBuilder suggestMatching(SuggestionsBuilder builder, Collection<T> collection, Function<T, String> toString) {
		//@formatter:off
		collection.stream()
				  .filter(Objects::nonNull)
				  .map(toString)
				  .filter(v -> v.toLowerCase().startsWith(builder.getRemainingLowerCase()))
				  .forEach(builder::suggest);
		//@formatter:on
		return builder;
	}
	
	/**
	 * Returns elements from the provided list based on the current users input
	 *
	 * @param builder the suggestionBuilder to fill
	 * @param collection the set of completion options
	 * @param toString the string function for these
	 * @param <T> value of elements
	 * @return suggestion provider
	 */
	public static <T> SuggestionsBuilder suggestMatching(SuggestionsBuilder builder, T[] collection, Function<T, String> toString) {
		//@formatter:off
		Arrays.stream(collection)
				  .filter(Objects::nonNull)
				  .map(toString)
				  .filter(v -> v.toLowerCase().startsWith(builder.getRemainingLowerCase()))
				  .forEach(builder::suggest);
		//@formatter:on
		return builder;
	}
	
	/**
	 * Adds an argument with the given suggestion options
	 *
	 * @param argumentName the argument name
	 * @param suggestions the tab suggestions
	 * @return the required argument
	 */
	public static <T> RequiredArgumentBuilder<CommandSourceStack, String> suggestedArgument(String argumentName, Collection<String> suggestions) {
		return argument(argumentName, StringArgumentType.string()).suggests(suggestMatching(suggestions));
	}
	
	/**
	 * Adds an argument with the given suggestion options
	 *
	 * @param argumentName the argument name
	 * @param suggestions the tab suggestions
	 * @return the required argument
	 */
	public static <T> RequiredArgumentBuilder<CommandSourceStack, String> suggestedArgument(String argumentName, String[] suggestions) {
		return argument(argumentName, StringArgumentType.string()).suggests(suggestMatching(suggestions));
	}
	
	/**
	 * Adds an argument with the given suggestion options
	 *
	 * @param argumentName the argument name
	 * @param argumentType the value type to interpret the argument as
	 * @param suggestions the tab suggestions
	 * @return the required argument
	 */
	public static <T> RequiredArgumentBuilder<CommandSourceStack, T> suggestedArgument(String argumentName,
																					   ArgumentType<T> argumentType,
																					   Collection<String> suggestions) {
		return argument(argumentName, argumentType).suggests(suggestMatching(suggestions));
	}
	
	/**
	 * Adds an argument with the given suggestion options
	 *
	 * @param argumentName the argument name
	 * @param argumentType the value type to interpret the argument as
	 * @param suggestions the tab suggestions
	 * @return the required argument
	 */
	public static <T> RequiredArgumentBuilder<CommandSourceStack, T> suggestedArgument(String argumentName,
																					   ArgumentType<T> argumentType,
																					   String[] suggestions) {
		return argument(argumentName, argumentType).suggests(suggestMatching(suggestions));
	}
	
	/**
	 * Adds an argument with the given suggestion options
	 *
	 * @param argumentName the argument name
	 * @param suggestions the tab suggestions
	 * @param toString the function to map the suggestion values to a string
	 * @return the required argument
	 */
	public static <T> RequiredArgumentBuilder<CommandSourceStack, String> suggestedArgument(String argumentName,
																							Collection<T> suggestions,
																							Function<T, String> toString) {
		return argument(argumentName, StringArgumentType.string()).suggests(suggestMatching(suggestions, toString));
	}
	
	/**
	 * Adds an argument with the given suggestion options
	 *
	 * @param argumentName the argument name
	 * @param suggestions the tab suggestions
	 * @param toString the function to map the suggestion values to a string
	 * @return the required argument
	 */
	public static <T> RequiredArgumentBuilder<CommandSourceStack, String> suggestedArgument(String argumentName,
																							T[] suggestions,
																							Function<T, String> toString) {
		return argument(argumentName, StringArgumentType.string()).suggests(suggestMatching(suggestions, toString));
	}
	
	/**
	 * Adds an argument with the given suggestion options
	 *
	 * @param argumentName the argument name
	 * @param argumentType the value type to interpret the argument as
	 * @param suggestions the tab suggestions
	 * @param toString the function to map the suggestion values to a string
	 * @return the required argument
	 */
	public static <T, U> RequiredArgumentBuilder<CommandSourceStack, T> suggestedArgument(String argumentName,
																						  ArgumentType<T> argumentType,
																						  Collection<U> suggestions,
																						  Function<U, String> toString) {
		return argument(argumentName, argumentType).suggests(suggestMatching(suggestions, toString));
	}
	
	/**
	 * Adds an argument with the given suggestion options
	 *
	 * @param argumentName the argument name
	 * @param argumentType the value type to interpret the argument as
	 * @param suggestions the tab suggestions
	 * @param toString the function to map the suggestion values to a string
	 * @return the required argument
	 */
	public static <T, U> RequiredArgumentBuilder<CommandSourceStack, T> suggestedArgument(String argumentName,
																						  ArgumentType<T> argumentType,
																						  U[] suggestions,
																						  Function<U, String> toString) {
		return argument(argumentName, argumentType).suggests(suggestMatching(suggestions, toString));
	}
	
	public static Message toMessage(Component component) {
		return MessageComponentSerializer.message().serialize(component);
	}
	
	public static Message toMessage(String text) {
		return MessageComponentSerializer.message().serialize(MiniMessage.miniMessage().deserialize(text));
	}
	
	public static Component toComponent(String text) {
		return MiniMessage.miniMessage().deserialize(text);
	}
	
	public static void suggest(SuggestionsBuilder builder, String message, String tooltip) {
		builder.suggest(message, toMessage(tooltip));
	}
	
	public static void suggest(SuggestionsBuilder builder, String message, Component tooltip) {
		builder.suggest(message, toMessage(tooltip));
	}
	
	public static <U> U getArgument(CommandContext<CommandSourceStack> context, String argument, Class<U> clazz, U def) {
		try{
			return context.getArgument(argument, clazz);
		} catch(IllegalArgumentException ignored){
			return def;
		}
	}
	
	public static <U> U getArgument(CommandContext<CommandSourceStack> context, String argument, Class<U> clazz) {
		try{
			return context.getArgument(argument, clazz);
		} catch(IllegalArgumentException ignored){
			return null;
		}
	}
}
