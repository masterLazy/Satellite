package masterlazy.satellite.guard;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import masterlazy.satellite.guard.model.RuleAction;
import net.minecraft.commands.CommandSourceStack;

import java.util.concurrent.CompletableFuture;

public class SuggestionAction implements SuggestionProvider<CommandSourceStack> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        for (RuleAction action : RuleAction.values()) {
            builder.suggest(action.name().toLowerCase());
        }
        return builder.buildFuture();
    }
}
