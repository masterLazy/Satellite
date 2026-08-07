package masterlazy.satellite.auth;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;

import java.util.concurrent.CompletableFuture;

public class SuggestionRegistered implements SuggestionProvider<CommandSourceStack> {
    private final AuthService service;

    public SuggestionRegistered(AuthService service) {
        this.service = service;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        for (String name : service.getRegisteredNames()) {
            builder.suggest(name);
        }
        return builder.buildFuture();
    }
}
