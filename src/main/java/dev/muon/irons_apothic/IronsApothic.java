package dev.muon.irons_apothic;

import dev.muon.irons_apothic.affix.AffixEventHandler;
import dev.muon.irons_apothic.affix.SchoolAttributeAffix;
import dev.muon.irons_apothic.affix.MagicTelepathicAffix;
import dev.muon.irons_apothic.affix.SpellEffectAffix;
import dev.muon.irons_apothic.affix.SpellLevelAffix;
import dev.shadowsoffire.apotheosis.affix.AffixRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(IronsApothic.MODID)
public class IronsApothic {
    public static final String MODID = "irons_apothic";
    public static final Logger LOGGER = LogManager.getLogger();

    public IronsApothic(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        LootCategories.register(modEventBus);
        AffixEventHandler.register();

        IronsApothic.LOGGER.info("Loading Iron's Apotheosis Compat");

    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Registering custom affix codecs...");
        AffixRegistry.INSTANCE.registerCodec(loc("attribute"), SchoolAttributeAffix.CODEC);
        AffixRegistry.INSTANCE.registerCodec(loc("spell_effect"), SpellEffectAffix.CODEC);
        AffixRegistry.INSTANCE.registerCodec(loc("magic_telepathic"), MagicTelepathicAffix.CODEC);
        AffixRegistry.INSTANCE.registerCodec(loc("spell_level"), SpellLevelAffix.CODEC);
        LOGGER.info("Custom affix codecs registered.");
    }

    public static ResourceLocation loc(String id) {
        return ResourceLocation.fromNamespaceAndPath(IronsApothic.MODID, id);
    }
}