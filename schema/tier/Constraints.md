# Description
Constraints are a standard set of definitions that may restrict how an object generates. This object is not as contextual as Exclusions, which are used by objects that target an Entity.  

When evaluating a constrained object, the constraint is checked before weighted selection. If an object's constraints prevent it from being selected, it will be as if it had zero weight.  

# Dependencies
This object references the following objects:
1. [WorldTier](./WorldTier.md)
2. HolderSet (A Vanilla / NeoForge construct which allows specifying a set of registry entries).

# Schema

```js
{
    "tiers": [           // [Optional] || A set of World Tiers this object may generate in. If the object is weighted, it is better to restrict tier access through that mechanism.
        WorldTier
    ],
    "dimensions": [      // [Optional] || A list of dimension registry names that this object may generate in.
        "string"
    ],
    "biomes": HolderSet, // [Optional] || A HolderSet of biomes that this object may generate in. Most commontly, this can be represented as a string list, or a #-prefixed tag name.
    "stages": [          // [Optional] || A set of game stages that this object may generate in.
        "string"
    ]
}
```

Note: for all members of a Constraints, if they are empty (or omitted), the object may generate in all [tiers/dimensions/biomes/stages] respectively.

# Examples
Constraints restricting an object to snowy biomes in the overworld.  

```json
{
    "biomes": "#c:is_snowy",
    "dimensions": [
        "minecraft:overworld"
    ]
}
```

Constraints restricting an object to The End.  

```json
{
    "dimensions": [
        "minecraft:the_end"
    ]
}
```
