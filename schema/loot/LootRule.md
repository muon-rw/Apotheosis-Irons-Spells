# Description
A Loot Rule is an object which specifies an action that a Loot Rarity will apply to an item when it is reforged.  

Loot Rules within a rarity are executed in order, and the affixes on an item are cleared before the reforge begins.  

# Dependencies
This object references the following objects:
1. DataComponentPatch (Vanilla Item NBT)
2. [AffixType](../affix/AffixType.md)

# Subtypes
Loot Rules are subtyped, meaning each subtype declares a `"type"` key and its own parameters.

## Component Loot Rule
Applies one or more data components to the reforged item stack.  

This loot rule is not reversable, meaning that once a component is applied, further reforges do not remove said component, unless the rule will override the component again.  

### Schema
```js
{
    "type": "apotheosis:component",
    "components": DataComponentPatch  // [Mandatory] || The components to apply or remove.
}
```

### Examples
A rule which applies Unbreaking and removes the Durability Bonus component.  

For a component patch, any key starting with `!` will remove that component from the stack.  

```json
{
    "type": "apotheosis:component",
    "components": {
        "!apotheosis:durability_bonus": {},
        "minecraft:unbreakable": {}
    }
}
```

## Affix Loot Rule
Applies a single affix of the specified affix type to the item.  

If there are no affixes available of the specified type, a warning will be logged when the rule is executed.  

### Schema
```js
{
    "type": "apotheosis:affix",
    "affix_type": AffixType     // [Mandatory] || The type of affix to apply.
}
```

### Examples
A rule which applies a single stat affix

```json
{
    "type": "apotheosis:affix",
    "affix_type": "stat"
}
```

## Socket Loot Rule
Applies a random number of sockets to the target item. This rule never reduces the number of sockets already on an item.  

### Schema
```js
{
    "type": "apotheosis:socket",
    "min": integer, // [Mandatory] || The minimum number of sockets to apply. Range: [0, 16].
    "max": integer, // [Mandatory] || The maximum number of sockets to apply. Range: [1, 16].
}
```

### Examples

A rule which applies between one and three sockets

```json
{
    "type": "apotheosis:socket",
    "min": 1,
    "max": 3
}
```

## Durability Loot Rule
Applies a random durability bonus to the target item. This rule overwrites any existing durability bonus.  

### Schema
```js
{
    "type": "apotheosis:durability",
    "min": float, // [Mandatory] || The minimum possible durability bonus. Range: [0, 1].
    "max": float, // [Mandatory] || The maximum possible durability bonus. Range: [0, 1].
}
```

### Examples

A rule which applies a durability bonus between 45% and 75%.

```json
{
    "type": "apotheosis:durability",
    "min": 0.45,
    "max": 0.75
}
```

## Chanced Loot Rule
Has a random chance to apply a given loot rule.

### Schema
```js
{
    "type": "apotheosis:chanced",
    "chance": float,     // [Mandatory] || The chance that the rule is applied. Range: [0, 1]
    "rule": LootRule     // [Mandatory] || The underlying loot rule that will be applied if the roll is successful.
}
```

### Examples
A rule that has a 25% chance to apply a basic effect affix.

```json
{
    "type": "apotheosis:chanced",
    "chance": 0.25,
    "rule": {
        "type": "apotheosis:affix",
        "affix_type": "basic_effect"
    }
}
```

## Combined Loot Rule
Bundles multiple loot rules into a single loot rule object. This is useful when combined with the Chanced loot rule to put multiple rules into the same chance roll.  

### Schema
```js
{
    "type": "apotheosis:combined",
    "rules": [      // [Mandatory] || The list of loot rules to apply.
        LootRule
    ]
}
```

### Examples
A combined rule that will apply sockets and a durability bonus.

```json
{
    "type": "apotheosis:combined",
    "rules": [
        {
            "type": "apotheosis:socket",
            "min": 1,
            "max": 3
        },
        {
            "type": "apotheosis:durability",
            "min": 0.25,
            "max": 0.55
        }
    ]
}
```

## Select Loot Rule
Rolls a random chance, and selects a different loot rule based on the outcome of the roll. If the roll succeeds, the `if_true` rule will be applied, otherwise the `if_false` rule will be applied.  

### Schema
```js
{
    "type": "apotheosis:select",
    "chance": float,      // [Mandatory] || The chance that the "if_true" rule is applied. Range: [0, 1]
    "if_true": LootRule,  // [Mandatory] || The loot rule to apply when the roll succeeds.
    "if_false": LootRule  // [Mandatory] || The loot rule to apply when the roll fails.
}
```

### Examples
This rule has a 99% chance to apply a 45% - 75% durability bonus, and a 1% chance to instead remove any existing durability bonus and apply Unbreakable.

```json
{
    "type": "apotheosis:select",
    "chance": 0.99,
    "if_false": {
        "type": "apotheosis:component",
        "components": {
            "!apotheosis:durability_bonus": {},
            "minecraft:unbreakable": {}
        }
    },
    "if_true": {
        "type": "apotheosis:durability",
        "min": 0.45,
        "max": 0.75
    }
}
```
