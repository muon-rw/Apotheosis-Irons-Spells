# Description
Bonus Loot Tables are a data attachment that allow specifying additional loot tables rolled when an entity is killed.  
These tables do not override the entity's original loot table, unlike vanilla's `CustomDeathLootTable` NBT entry.

# Schema

Bonus Loot Tables are a json array of strings, where each string is a loot table registry name.

```js
[             // [Mandatory] || A list of loot table registry names that will be rolled when the mob is killed.
    "string"
]
```

Any invalid tables will be silently ignored, so be careful to double-check the names.

# Examples
Bonus Loot Tables that appends both the boss drops and the rare boss drops loot tables.  

```json
[
    "apotheosis:entity/boss_drops",
    "apotheosis:entity/rare_boss_drops"
]
```
