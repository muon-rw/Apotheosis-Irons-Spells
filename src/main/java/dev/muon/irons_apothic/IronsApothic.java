package dev.muon.irons_apothic;

import dev.muon.irons_apothic.attribute.AttributeRegistry;
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
        AttributeRegistry.init(modEventBus);

        IronsApothic.LOGGER.info("Loading Iron's Apotheosis Compat");

    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    public static ResourceLocation loc(String id) {
        return ResourceLocation.fromNamespaceAndPath(IronsApothic.MODID, id);
    }
}