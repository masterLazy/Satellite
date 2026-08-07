package masterlazy.satellite;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import masterlazy.satellite.guard.GuardService;
import masterlazy.satellite.guard.model.RuleEntry;
import net.minecraft.commands.CommandSourceStack;

import java.util.concurrent.CompletableFuture;

public class SuggestionRule implements SuggestionProvider<CommandSourceStack> {
    private final GuardService service;

    public SuggestionRule(GuardService service) {
        this.service = service;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        for (RuleEntry rule : service.getRules()) {
            builder.suggest(rule.id());
        }
        return builder.buildFuture();
    }
}
