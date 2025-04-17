package dev.muon.irons_apothic.config;

import dev.shadowsoffire.placebo.config.Configuration;

public class OWApothConfig {
    private static Configuration config;

    public static double affixBaseChance;
    public static double affixLevelChanceIncrease;
    public static double affixMaxChance;
    public static double gemBaseChance;
    public static double gemLevelChanceIncrease;
    public static double gemMaxChance;
    public static double chestLootBaseChance;
    public static double chestLootLevelChanceIncrease;
    public static double chestLootMaxChance;

    public static String[] levelRarityMappings;
    public static String[] gemRarityMappings;
    public static String[] playerLevelRarityMappings;

    public static void init() {
        config = new Configuration("irons_apothic");
        config.setTitle("Otherworld Apotheosis Configuration");

        String category = "loot";

        affixBaseChance = config.getFloat("affixBaseChance", category, 0.05f, 0, 1,
                "Base chance for an item to receive affixes");

        affixLevelChanceIncrease = config.getFloat("affixLevelChanceIncrease", category, 0.01f, 0, 1,
                "How much the affix chance increases per level");

        affixMaxChance = config.getFloat("affixMaxChance", category, 0.25f, 0, 1,
                "Maximum chance for an item to receive affixes");

        gemBaseChance = config.getFloat("gemBaseChance", category, 0.02f, 0, 1,
                "Base chance for a gem to drop");

        gemLevelChanceIncrease = config.getFloat("gemLevelChanceIncrease", category, 0.005f, 0, 1,
                "How much the gem drop chance increases per level");

        gemMaxChance = config.getFloat("gemMaxChance", category, 0.15f, 0, 1,
                "Maximum chance for a gem to drop");

        chestLootBaseChance = config.getFloat("chestLootBaseChance", category, 0.15f, 0, 1,
                "Base chance for a chest item to receive affixes");

        chestLootLevelChanceIncrease = config.getFloat("chestLootLevelChanceIncrease", category, 0.02f, 0, 1,
                "How much the chest loot affix chance increases per player level");

        chestLootMaxChance = config.getFloat("chestLootMaxChance", category, 0.5f, 0, 1,
                "Maximum chance for a chest item to receive affixes");

        levelRarityMappings = config.getStringList("levelAffixMappings", category,
                new String[] {
                        "25=mythic-ancient",
                        "20=epic-mythic",
                        "15=rare-epic",
                        "10=uncommon-rare",
                        "5=common-uncommon",
                        "1=common-common"
                },
                "Level threshold to rarity mapping for affixes. Format: 'level=minRarity-maxRarity'\n" +
                        "Each entry applies to levels from its value up to (but not including) the next threshold.\n" +
                        "For example: '20=epic-mythic' applies to levels 20-24 if the next threshold is 25.\n" +
                        "Valid rarities: common, uncommon, rare, epic, mythic, ancient" +
                        "Note that within a given range, which rarity gets assigned is still determined by the" +
                        "weights defined in the rarity itself, and is affected by Luck.");

        gemRarityMappings = config.getStringList("levelGemMappings", category,
                new String[]{
                        "25=mythic-ancient",
                        "20=epic-mythic",
                        "15=rare-epic",
                        "10=uncommon-rare",
                        "5=common-uncommon",
                        "1=common-common"
                },
                "Level:Rarity mapping for gems. Format: 'level=minRarity-maxRarity'\n" +
                        "Each entry applies to levels from its value up to (but not including) the next threshold.\n" +
                        "For example: '20=epic-mythic' applies to levels 20-24 if the next threshold is 25.\n" +
                        "Valid rarities: common, uncommon, rare, epic, mythic, ancient" +
                        "Note that within a given range, which rarity gets assigned is still determined by the" +
                        "weights defined in the rarity itself, and is affected by Luck.");

        playerLevelRarityMappings = config.getStringList("playerLevelMappings", category,
                new String[]{
                        "16=rare-mythic",
                        "12=rare-epic",
                        "6=uncommon-rare",
                        "4=common-uncommon",
                        "2=common-common"
                },
                "Level threshold to rarity mapping for container loot based on player level.\n" +
                        "Each entry applies to levels from its value up to (but not including) the next threshold.\n" +
                        "For example: '15=rare-epic' applies to levels 15-19 if the next threshold is 20.\n" +
                        "Valid rarities: common, uncommon, rare, epic, mythic, ancient");
        
        if (config.hasChanged()) {
            config.save();
        }
    }
}