package dev.muon.irons_apothic.affix;

import com.google.common.base.Preconditions;
import dev.shadowsoffire.apotheosis.affix.AffixBuilder;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.placebo.util.StepFunction;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@SuppressWarnings("unchecked")
public abstract class SchoolFilteredAffixBuilder<T extends SchoolFilteredAffixBuilder<T, V>, V> extends AffixBuilder<T> {

    protected final Map<LootRarity, V> values = new LinkedHashMap<>();
    protected final Set<LootCategory> categories = new LinkedHashSet<>();
    @Nullable
    protected List<ResourceLocation> schoolIds;

    public T categories(LootCategory... categories) {
        Collections.addAll(this.categories, categories);
        return (T) this;
    }

    public T value(LootRarity rarity, V value) {
        this.values.put(rarity, value);
        return (T) this;
    }

    public T schools(SchoolType... schools) {
        return this.schools(Arrays.stream(schools).map(SchoolType::getId).toArray(ResourceLocation[]::new));
    }

    public T schools(ResourceLocation... schoolIds) {
        if (this.schoolIds == null) {
            this.schoolIds = new ArrayList<>();
        }
        Collections.addAll(this.schoolIds, schoolIds);
        return (T) this;
    }

    /**
     * Restricts the affix to gear with generic spell power, or with no school but some other magic attribute.
     * Without this or {@link #schools}, the affix matches all gear.
     */
    public T schoolless() {
        this.schoolIds = new ArrayList<>();
        return (T) this;
    }

    protected Optional<List<ResourceLocation>> schoolFilter() {
        return Optional.ofNullable(this.schoolIds).map(List::copyOf);
    }

    protected void validate() {
        Preconditions.checkState(this.definition != null, "Affix definition is not set");
        Preconditions.checkState(!this.values.isEmpty(), "Affix has no rarity values");
    }

    public static abstract class Valued<T extends Valued<T>> extends SchoolFilteredAffixBuilder<T, StepFunction> {
        protected float step = 0.01F;

        public T step(float step) {
            this.step = step;
            return (T) this;
        }

        public T value(LootRarity rarity, float min, float max) {
            return this.value(rarity, StepFunction.fromBounds(min, max, this.step));
        }

        public T value(LootRarity rarity, float value) {
            return this.value(rarity, StepFunction.constant(value));
        }
    }
}
