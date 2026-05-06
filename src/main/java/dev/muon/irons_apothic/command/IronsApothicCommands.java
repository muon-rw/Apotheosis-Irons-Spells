package dev.muon.irons_apothic.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.muon.irons_apothic.IronsApothic;
import dev.muon.irons_apothic.util.SpellDiscoveryUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Path;

public class IronsApothicCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            LiteralArgumentBuilder.<CommandSourceStack>literal("irons_apothic")
                .requires(source -> source.hasPermission(2)) // Require OP level 2
                .then(Commands.literal("spells")
                    .then(Commands.literal("list")
                        .executes(context -> {
                            SpellDiscoveryUtil.logSpells();
                            context.getSource().sendSuccess(() ->
                                Component.literal("Available spells have been logged to the console/log file."),
                                true
                            );
                            return 1;
                        })
                    )
                    .then(Commands.literal("dump")
                        .executes(context -> {
                            try {
                                Path outputFile = SpellDiscoveryUtil.dumpToFile();
                                context.getSource().sendSuccess(() ->
                                    Component.literal("Spell dump complete! Written to: " + outputFile),
                                    true
                                );
                                return 1;
                            } catch (IOException e) {
                                IronsApothic.LOGGER.error("Failed to write spell dump to file", e);
                                context.getSource().sendFailure(
                                    Component.literal("Failed to write spell dump: " + e.getMessage())
                                );
                                return 0;
                            }
                        })
                    )
                )
                .then(Commands.literal("schools")
                    .then(Commands.literal("list")
                        .executes(context -> SpellDiscoveryUtil.sendSchoolsToChat(context.getSource()))
                    )
                )
        );
    }
}
