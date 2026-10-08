package com.nogamble.v1_21_11.mixin;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.nogamble.NoGamble;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(ChatInputSuggestor.class)
public abstract class ChatInputSuggestorMixin {

    @Shadow
    private CompletableFuture<Suggestions> pendingSuggestions;

    @Inject(method = "showCommandSuggestions", at = @At("HEAD"))
    private void nogamble$filterBlockedSuggestions(CallbackInfo ci) {
        if (this.pendingSuggestions != null) {
            this.pendingSuggestions = this.pendingSuggestions.thenApply(suggestions -> {
                if (suggestions == null) {
                    return null;
                }
                NoGamble mod = NoGamble.getInstance();
                List<Suggestion> list = suggestions.getList();
                List<Suggestion> filtered = new ArrayList<>();
                for (Suggestion suggestion : list) {
                    if (!mod.isCommandBlocked(suggestion.getText())) {
                        filtered.add(suggestion);
                    }
                }
                return new Suggestions(suggestions.getRange(), filtered);
            });
        }
    }
}
