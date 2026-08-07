package masterlazy.satellite.guard;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import masterlazy.satellite.guard.model.ConditionType;
import net.minecraft.commands.CommandSourceStack;

import java.util.concurrent.CompletableFuture;

public class SuggestionType implements SuggestionProvider<CommandSourceStack> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        for (ConditionType type : ConditionType.values()) {
            builder.suggest(type.name().toLowerCase());
        }
        return builder.buildFuture();
    }
}
