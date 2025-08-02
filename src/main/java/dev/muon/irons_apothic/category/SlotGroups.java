package dev.muon.irons_apothic.category;

import dev.muon.irons_apothic.IronsApothic;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.apothic_attributes.compat.CurioEquipmentSlot;
import dev.shadowsoffire.apothic_attributes.modifiers.EntityEquipmentSlot;
import dev.shadowsoffire.apothic_attributes.modifiers.EntitySlotGroup;
import dev.shadowsoffire.placebo.registry.DeferredHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.neoforged.bus.api.IEventBus;

public class SlotGroups {

    public static final DeferredHelper R = DeferredHelper.create(IronsApothic.MODID);

    public static void register(IEventBus bus) {
        bus.register(R);
        Slots.init();
        Groups.init();
    }

    public static class Slots {
        public static final Holder<EntityEquipmentSlot> SPELLBOOK = slot("spellbook");

        private static Holder<EntityEquipmentSlot> slot(String slot) {
            return R.<EntityEquipmentSlot, EntityEquipmentSlot>customDH(slot, ALObjects.BuiltInRegs.ENTITY_EQUIPMENT_SLOT.key(), () -> new CurioEquipmentSlot(slot));
        }

        public static void init() {}
    }

    public static class Groups {
        public static final EntitySlotGroup SPELLBOOK = group("spellbook", HolderSet.direct(Slots.SPELLBOOK));

        private static EntitySlotGroup group(String path, HolderSet<EntityEquipmentSlot> slots) {
            return R.custom(path, ALObjects.BuiltInRegs.ENTITY_SLOT_GROUP.key(), new EntitySlotGroup(IronsApothic.loc(path), slots));
        }

        public static void init() {}
    }
}