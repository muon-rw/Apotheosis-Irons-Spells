package dev.muon.irons_apothic;

import dev.muon.irons_apothic.affix.*;
import dev.muon.irons_apothic.category.LootCategories;
import dev.muon.irons_apothic.category.SlotGroups;
import dev.muon.irons_apothic.command.IronsApothicCommands;
import dev.muon.irons_apothic.loot.SchoolSelectLootRule;
import dev.muon.irons_apothic.network.IronsApothicPayloads;
import dev.shadowsoffire.apotheosis.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.loot.LootRule;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(IronsApothic.MODID)
public class IronsApothic {
    public static final String MODID = "irons_apothic";
    public static final Logger LOGGER = LogManager.getLogger();

    public IronsApothic(IEventBus modEventBus, ModContainer modContainer) {
        SlotGroups.register(modEventBus);
        LootCategories.register(modEventBus);
        AffixEventHandler.register();
        IronsApothicPayloads.register(modEventBus);
        // Registered here, not in common setup, because data runs skip common setup and addons datagen our affixes
        registerCodecs();

        NeoForge.EVENT_BUS.addListener(this::registerCommands);

        IronsApothic.LOGGER.info("Loading Iron's Apotheosis Compat");

    }

    private static void registerCodecs() {
        AffixRegistry.INSTANCE.registerCodec(loc("attribute"), SchoolAttributeAffix.CODEC);
        AffixRegistry.INSTANCE.registerCodec(loc("spell_effect"), SpellEffectAffix.CODEC);
        AffixRegistry.INSTANCE.registerCodec(loc("magic_telepathic"), MagicTelepathicAffix.CODEC);
        AffixRegistry.INSTANCE.registerCodec(loc("spell_level"), SpellLevelAffix.CODEC);
        AffixRegistry.INSTANCE.registerCodec(loc("spell_trigger"), SpellTriggerAffix.CODEC);
        AffixRegistry.INSTANCE.registerCodec(loc("imbued_spell_trigger"), ImbuedSpellTriggerAffix.CODEC);
        AffixRegistry.INSTANCE.registerCodec(loc("mana_cost"), ManaCostAffix.CODEC);
        LootRule.CODEC.register(loc("school_select"), SchoolSelectLootRule.CODEC);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        IronsApothicCommands.register(event.getDispatcher());
        LOGGER.info("Commands registered.");
    }

    public static ResourceLocation loc(String id) {
        return ResourceLocation.fromNamespaceAndPath(IronsApothic.MODID, id);
    }
}