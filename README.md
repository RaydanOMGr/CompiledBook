> ⚠️ WARNING: This mod enables RCE on your server, use with caution

# Mod with no name yet
currently CompiledBook

## What is this
This mod lets you compile java code that you write into a book and immediately execute it on the same server

Highly hazardous

## Usage
You take a Book & Quill and write whatever code you want into it

To execute it, you need to sign the book, put it in your offhand and write /execbook

This will compile the book, and if any errors occur during compilation, it will inform you

If the compilation succeeds, the mod will scan for all compiled classes extending me.andreasmelone.compiledbook.CompiledClass and execute those

If you have compiled the book before, the mod will not recompile classes and execute what is already compiled

After execution, you will be notified on the result. A book (referred to as program) always returns an exit code, 0 indicating success, anything else indicating a failure.

Known exit codes: 

| Exit code  | Usage                                                                                                        |
|------------|--------------------------------------------------------------------------------------------------------------|
| 0x00       | Indicates success, should always be returned if the program did not fail and successfully finished execution |
| 0xD0000001 | Indicates that something has gone wrong in the code that is executing yours; an error on my side             |
| 0xD0000002 | Indicates that an exception occurred, causing the execution to finish prematurely; an error on your side     |



## Example
```java
import me.andreasmelone.compiledbook.CompiledBook;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

public int execute(MinecraftServer server, ServerLevel level, ServerPlayer executor) {
    executor.sendSystemMessage(Component.literal("hi"));
    return 0;
}
```