package dev.muon.irons_apothic.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.irons_apothic.LootCategories;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

public class MagicTelepathicAffix extends Affix {
    public static final Codec<MagicTelepathicAffix> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            Affix.affixDef(),
            RarityRegistry.INSTANCE.holderCodec().fieldOf("min_rarity").forGetter(a -> a.minRarity))
        .apply(inst, MagicTelepathicAffix::new));

    protected final DynamicHolder<LootRarity> minRarity;

    public MagicTelepathicAffix(AffixDefinition definition, DynamicHolder<LootRarity> minRarity) {
        super(definition);
        this.minRarity = minRarity;
    }

    @Override
    public boolean enablesTelepathy() {
        return true;
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public MutableComponent getDescription(AffixInstance inst, AttributeTooltipContext ctx) {
        return Component.translatable("affix." + this.id() + ".desc.staff");
    }

    public static void drops(LivingDropsEvent e) {
        DamageSource src = e.getSource();
        Entity directEntity = src.getDirectEntity();
        Entity causingEntity = src.getEntity();

        boolean canTeleport = false;
        Vec3 targetPos = null;

        if (directEntity instanceof Projectile spell && spell.getOwner() != null) {
            canTeleport = IronsApothicAffixHelper.streamAffixes(spell).anyMatch(AffixInstance::enablesTelepathy);
            if (canTeleport) {
                targetPos = spell.getOwner().position();
            }
        }

        if (!canTeleport && causingEntity instanceof LivingEntity living) {
            ItemStack weapon = living.getMainHandItem();

            canTeleport = AffixHelper.streamAffixes(weapon).anyMatch(AffixInstance::enablesTelepathy);
            if (canTeleport) {
                targetPos = living.position();
            }
        }

        if (canTeleport && targetPos != null) {
            for (ItemEntity item : e.getDrops()) {
                item.setPos(targetPos.x, targetPos.y, targetPos.z);
                item.setPickUpDelay(0);
            }
        }
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        return this.minRarity.isBound() && LootCategories.isStaff(stack) && rarity.sortIndex() >= this.minRarity.get().sortIndex();
    }
}