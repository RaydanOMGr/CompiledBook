package me.andreasmelone.compiledbook;

import com.mojang.logging.LogUtils;
import me.andreasmelone.compiledbook.commands.ExecuteBookCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;

public class CompiledBookMod implements ModInitializer {
    public static final Logger LOGGER = LogUtils.getLogger();
    private final Compiler compilator = new Compiler();

    @Override
    public void onInitialize() {
        LOGGER.info("I am about to commit arbitrary code execution!");
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ExecuteBookCommand.register(dispatcher, compilator));
    }
}
