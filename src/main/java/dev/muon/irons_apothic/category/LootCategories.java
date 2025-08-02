package dev.muon.irons_apothic.category;

import dev.muon.irons_apothic.IronsApothic;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import io.redspace.ironsspellbooks.item.CastingItem;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class LootCategories {

    private static final DeferredRegister<LootCategory> LOOT_CATEGORIES = DeferredRegister.create(Apoth.BuiltInRegs.LOOT_CATEGORY.key(), IronsApothic.MODID);


    public static LootCategory SPELLBOOK = new LootCategory(
            s -> s.getItem() instanceof SpellBook, SlotGroups.Groups.SPELLBOOK, 900
    );
    public static final LootCategory STAFF = new LootCategory(
            s -> s.getItem() instanceof CastingItem, ALObjects.EquipmentSlotGroups.MAINHAND, 900
    );

    static {
        LOOT_CATEGORIES.register("staff", () -> STAFF);
        LOOT_CATEGORIES.register("spellbook", () -> SPELLBOOK);
    }

    public static void register(IEventBus modEventBus) {
        LOOT_CATEGORIES.register(modEventBus);
    }

    public static boolean isStaff(ItemStack stack) {
        return LootCategory.forItem(stack) == STAFF;
    }
    public static boolean isSpellBook(ItemStack stack) {
        return LootCategory.forItem(stack) == SPELLBOOK;
    }
}
