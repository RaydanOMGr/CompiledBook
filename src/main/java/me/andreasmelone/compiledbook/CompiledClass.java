package me.andreasmelone.compiledbook;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * This is a special class all classes written in books
 * that need to be executed have to extend
 */
public abstract class CompiledClass {
    public CompiledClass() {
    }

    /**
     * This method is executed whenever the book is executed
     * @param server the current {@link MinecraftServer} instance
     * @param level the current world
     * @param executor the {@link ServerPlayer} that executed the book
     * @return the exit code, 0 stands for success, whereas any other exit code is considered a failure
     *         0x000000FF is a reserved exit code and means that something went wrong,
     *         such as an exception in your code or an exception in the code that runs this method
     */
    public abstract int execute(MinecraftServer server, ServerLevel level, ServerPlayer executor);
}
