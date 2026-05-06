package dev.muon.irons_apothic.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.muon.irons_apothic.IronsApothic;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class SpellDiscoveryUtil {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static List<AbstractSpell> getAvailableSpells() {
        return SpellRegistry.REGISTRY.stream()
                .filter(spell -> spell != SpellRegistry.none())
                .toList();
    }

    public static void logSpells() {
        var spells = getAvailableSpells();

        IronsApothic.LOGGER.info("=== Available Spells for Spell Trigger Affixes ===");

        Map<SchoolType, List<AbstractSpell>> spellsBySchool = spells.stream()
                .collect(Collectors.groupingBy(
                        AbstractSpell::getSchoolType,
                        () -> new TreeMap<>(Comparator.comparing(s -> s.getId().toString())),
                        Collectors.toList()
                ));

        spellsBySchool.forEach((school, schoolSpells) -> {
            IronsApothic.LOGGER.info("\n{} School:", school.getDisplayName().getString());
            schoolSpells.forEach(spell -> {
                IronsApothic.LOGGER.info("  - {} ({}): Level {}-{}, Cast Type: {}, Min Rarity: {}\n      {}",
                    spell.getSpellName(),
                    spell.getSpellId(),
                    spell.getMinLevel(),
                    spell.getMaxLevel(),
                    spell.getCastType(),
                    spell.getRarity(spell.getMinLevel()).name(),
                    Component.translatable(spell.getComponentId() + ".guide").getString()
                );
            });
        });
    }


    public static int sendSchoolsToChat(CommandSourceStack source) {
        List<SchoolType> schools = SchoolRegistry.REGISTRY.stream()
                .sorted(Comparator.comparing(s -> s.getId().toString()))
                .toList();

        source.sendSuccess(() -> Component.literal("Available Schools (" + schools.size() + "):"), false);
        for (SchoolType school : schools) {
            source.sendSuccess(() -> Component.literal(" - ")
                    .append(school.getDisplayName())
                    .append(Component.literal(" (" + school.getId() + ")")), false);
        }
        return schools.size();
    }

    public static Path dumpToFile() throws IOException {
        List<SpellData> spellDataList = new ArrayList<>();

        for (AbstractSpell spell : getAvailableSpells()) {
            spellDataList.add(new SpellData(
                    spell.getSpellId(),
                    spell.getSchoolType().getId().toString(),
                    spell.getRarity(spell.getMinLevel()).name(),
                    spell.getCastType().name(),
                    spell.getMinLevel(),
                    spell.getMaxLevel(),
                    Component.translatable(spell.getComponentId() + ".guide").getString()
            ));
        }

        Path modConfigDir = FMLPaths.CONFIGDIR.get().resolve(IronsApothic.MODID);
        Files.createDirectories(modConfigDir);
        Path outputFile = modConfigDir.resolve("spells.json");
        Files.writeString(outputFile, GSON.toJson(spellDataList));
        IronsApothic.LOGGER.info("Spell dump written to: {}", outputFile);
        return outputFile;
    }

    private record SpellData(String spellId, String school, String minRarity, String castType,
                             int minLevel, int maxLevel, String description) {}
}
