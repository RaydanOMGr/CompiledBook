package me.andreasmelone.compiledbook;

import com.mojang.logging.LogUtils;
import me.andreasmelone.compiledbook.classloader.BookClassLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import javax.tools.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Compiler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(4);
    private final Map<UUID, BookClassLoader> compiledExecutables = new HashMap<>();

    public void compile(ServerPlayer executor, UUID bookId, String text, Runnable callback) throws IOException {
        if(compiledExecutables.containsKey(bookId)) {
            executor.sendSystemMessage(Component.literal("Compiled classes found!"));
            callback.run();
            return;
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        MinecraftServer mcServer = executor.getServer();
        assert mcServer != null; // shouldn't ever be the case

        Path outDir = Path.of(".bookcache", bookId.toString());
        Files.createDirectories(outDir);
        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outDir.toFile()));

        Path textFile = Files.createTempFile("compiledbook", ".java");
        Files.writeString(textFile, text);
        Iterable<? extends JavaFileObject> javaFile = fileManager.getJavaFileObjects(textFile);

        THREAD_POOL.submit(() -> {
            StringWriter writer = new StringWriter();
            if(!compiler.getTask(writer, fileManager, diagnostics, null, null, javaFile).call()) {
                executor.sendSystemMessage(Component.literal("Compilation of book " + bookId + " failed!").withStyle(ChatFormatting.RED));
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    executor.sendSystemMessage(
                            Component.literal("Error on line " + diagnostic.getLineNumber() + ": " + diagnostic.getMessage(null))
                                    .withStyle(ChatFormatting.RED)
                    );
                }
                return;
            }

            try (var stream = Files.walk(outDir)) {
                BookClassLoader bookClassLoader = new BookClassLoader(bookId.toString(), this.getClass().getClassLoader());
                stream
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            if(!file.toString().endsWith(".class")) return;
                            String className = outDir.toAbsolutePath()
                                    .normalize()
                                    .relativize(file.toAbsolutePath().normalize())
                                    .toString();
                            className = className.substring(0, className.length() - ".class".length()).replace(File.separator, "/");
                            byte[] readBytes = Files.readAllBytes(file);
                            bookClassLoader.defineClass(className, readBytes);
                        } catch (IOException e) {
                            executor.sendSystemMessage(Component.literal("Failed to read file " + file + "! See log for more information.").withStyle(ChatFormatting.RED));
                            LOGGER.error("Failed to read file {}!", file, e);
                        }  catch (Throwable e) {
                            executor.sendSystemMessage(Component.literal("Failed to define class! See log for more information.").withStyle(ChatFormatting.RED));
                            LOGGER.error("Failed to define class!", e);
                        }
                    });
                compiledExecutables.put(bookId, bookClassLoader);
            } catch (IOException e) {
                executor.sendSystemMessage(Component.literal("Failed to walk files! See log for more information.").withStyle(ChatFormatting.RED));
                LOGGER.error("Failed to walk files!", e);
            }

            executor.sendSystemMessage(Component.literal("Successfully compiled book " + bookId + "!").withStyle(ChatFormatting.GREEN));
            mcServer.schedule(mcServer.wrapRunnable(callback));
        });
    }

    public Set<Class<? extends CompiledClass>> getCompiledClasses(UUID bookId) {
        return compiledExecutables.get(bookId).getDefinedSubtypeClasses(CompiledClass.class);
    }
}
