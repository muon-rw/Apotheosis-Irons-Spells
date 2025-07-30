package dev.muon.irons_apothic.util;

import dev.muon.irons_apothic.IronsApothic;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.neoforged.fml.ModList;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.stream.Collectors;

public class SpellDiscoveryUtil {
    
    public static void logAvailableSpells() {
        var spells = getAvailableSpells();
        
        IronsApothic.LOGGER.info("=== Available Spells for Spell Trigger Affixes ===");
        
        // Group by school
        Map<SchoolType, List<AbstractSpell>> spellsBySchool = spells.stream()
                .collect(Collectors.groupingBy(AbstractSpell::getSchoolType));
        
        spellsBySchool.forEach((school, schoolSpells) -> {
            IronsApothic.LOGGER.info("\n{} School:", school.getDisplayName().getString());
            schoolSpells.forEach(spell -> {
                IronsApothic.LOGGER.info("  - {} ({}): Level {}-{}, Cast Type: {}", 
                    spell.getSpellName(),
                    spell.getSpellId(), 
                    spell.getMinLevel(), 
                    spell.getMaxLevel(),
                    spell.getCastType()
                );
            });
        });
        
        // List good buff/utility spells for affixes
        IronsApothic.LOGGER.info("\n=== Potential Buff/Utility Spells ===");
        spells.stream()
                .filter(spell -> spell.getCastType() == CastType.INSTANT)
                .filter(spell -> !spell.getSpellName().contains("bolt") && 
                               !spell.getSpellName().contains("slash") &&
                               !spell.getSpellName().contains("arrow"))
                .forEach(spell -> {
                    IronsApothic.LOGGER.info("  - {} ({})", spell.getSpellName(), spell.getSpellId());
                });
    }
    
    public static List<AbstractSpell> getAvailableSpells() {
        var allScanData = ModList.get().getAllScanData();
        Set<String> spellClassNames = new HashSet<>();

        allScanData.forEach(scanData -> {
            scanData.getAnnotations().forEach(annotationData -> {
                if (Objects.equals(annotationData.annotationType(), Type.getType(AutoSpellConfig.class))) {
                    spellClassNames.add(annotationData.memberName());
                }
            });
        });

        List<AbstractSpell> spells = new ArrayList<>();
        spellClassNames.forEach(spellName -> {
            try {
                Class<?> pluginClass = Class.forName(spellName);
                var pluginClassSubclass = pluginClass.asSubclass(AbstractSpell.class);
                var constructor = pluginClassSubclass.getDeclaredConstructor();
                var instance = constructor.newInstance();
                spells.add(instance);
            } catch (Exception e) {
                IronsApothic.LOGGER.error("SpellDiscovery error for {}: {}", spellName, e.getMessage());
            }
        });

        return spells;
    }
} 