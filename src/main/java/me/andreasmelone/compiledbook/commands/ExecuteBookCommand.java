package me.andreasmelone.compiledbook.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import me.andreasmelone.compiledbook.CompiledClass;
import me.andreasmelone.compiledbook.Compiler;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class ExecuteBookCommand {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, Compiler compiler) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("execbook");
        command
                .requires(CommandSourceStack::isPlayer)
                .executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ItemStack stack = player.getItemBySlot(EquipmentSlot.OFFHAND);
            if(!stack.getComponents().has(DataComponents.WRITTEN_BOOK_CONTENT)) {
                ctx.getSource().sendFailure(Component.literal("Please hold a written book in your offhand!").withStyle(ChatFormatting.RED));
                return 1;
            }

            WrittenBookContent contentComponent = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
            if(contentComponent == null) { // shouldn't happen but lets make sure
                ctx.getSource().sendFailure(Component.literal("The provided item returned NULL for component WRITTEN_BOOK_CONTENT!").withStyle(ChatFormatting.RED));
                return 1;
            }

            StringBuilder sb = new StringBuilder();
            for (Component page : contentComponent.getPages(false)) {
                sb.append(page.getString()).append('\n');
            }
            String content = sb.toString();
            UUID bookId = nameUUIDFromHash(content);

            try {
                compiler.compile(player, bookId, content, () -> {
                    if(compiler.getCompiledClasses(bookId) == null) return;
                    for (Class<? extends CompiledClass> compiledClass : compiler.getCompiledClasses(bookId)) {
                        int exitCode = 0xD0000001;
                        try {
                            Constructor<? extends CompiledClass> ctor = compiledClass.getDeclaredConstructor();
                            ctor.setAccessible(true);
                            CompiledClass clazz = ctor.newInstance();
                            exitCode = clazz.execute(player.getServer(), player.serverLevel(), player);
                        } catch (InstantiationException | IllegalAccessException e) {
                            ctx.getSource().sendFailure(Component.literal("Failed to create an instance of " + compiledClass.getName() + "! Check logs for more details."));
                            LOGGER.error("Failed to create an instance of {}!", compiledClass.getName(), e);
                        } catch (Exception e) {
                            exitCode = 0xD0000002;
                            ctx.getSource().sendFailure(Component.literal("Your code produced an exception!").withStyle(ChatFormatting.RED));
                            ctx.getSource().sendFailure(Component.literal(e.getMessage()).withStyle(ChatFormatting.RED));
                            for (StackTraceElement el : e.getStackTrace()) {
                                ctx.getSource().sendFailure(Component.literal(el.toString()).withStyle(ChatFormatting.RED));
                            }
                        }
                        Component exitComponent = Component.literal("Program " + bookId + " finished with code " + exitCode + " (0x" + Integer.toHexString(exitCode) + ")");
                        if(exitCode == 0) {
                            ctx.getSource().sendSuccess(() -> exitComponent.copy().withStyle(ChatFormatting.GREEN), true);
                        } else {
                            ctx.getSource().sendFailure(exitComponent.copy().withStyle(ChatFormatting.RED));
                        }
                    }
                });
            } catch (Exception e) {
                ctx.getSource().sendFailure(Component.literal("Failed to compile! Check logs for more details."));
                LOGGER.error("Failed to compile!", e);
            }

            return 1;
        });

        dispatcher.register(command);
    }

    // copied from java.util.UUID, modified to do version 5 instead of version 3
    private static UUID nameUUIDFromHash(String name) {
        MessageDigest sha1 = getSHA1();
        byte[] hash = sha1.digest(name.getBytes(StandardCharsets.UTF_8));

        hash[6] &= 0x0f;  // clear version
        hash[6] |= 0x50;  // set to version 5
        hash[8] &= 0x3f;  // clear variant
        hash[8] |= (byte) 0x80;  // set to IETF variant

        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++) msb = (msb << 8) | (hash[i] & 0xff);
        for (int i = 8; i < 16; i++) lsb = (lsb << 8) | (hash[i] & 0xff);

        return new UUID(msb, lsb);
    }

    // I hate how java wants me to have an ugly try-catch in my beautiful method
    // so I extracted the try-catch into this separate method!
    private static MessageDigest getSHA1() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            // not happening lmao
            throw new RuntimeException(e);
        }
    }
}
