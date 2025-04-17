# Description
A Mob Spawn Type represents the finite set of events that may cause a mob to spawn.  

# Schema

In JSON, a spawn type is represented as a single string.

```js
"string" // [Mandatory] || The spawn type.
```

The list of spawn types is as follows:

1. `natural` - Spawns incurred by natural mob spawning, not including those added during chunk generation.
2. `chunk_generation` - Mobs added during chunk generation.
3. `spawner` - Mobs spawned by a Mob Spawner (not including Trial Spawners).
4. `structure` - Mobs spawned by a structure, either passively (like piglins from a nether portal) or during structure placement.
5. `breeding` - A mob spawned by breeding. Based on usages, this is only applied by Villagers, and not animals.
6. `mob_summoned` - Mobs spawned in by another mob, such as villagers spawning Iron Golems.
7. `jockey` - Mobs spawned to be a jockey mount by another mob.
8. `event` - Mobs spawned by an event, such as a raid, or wandering trader spawning.
9. `conversion` - A mob that has been converted from another entity, such as villagers to zombie villagers.
10. `reinforcement` - A mob summoned for reinforcement by something like a Zombie.
11. `triggered` - A mob spawn triggered by something, such as a Warden spawning due to sculk shriekers.
12. `bucket` - A mob spawned by placing a bucket.
13. `spawn_egg` - A mob spawned by using a Spawn Egg item.
14. `command` - A mob summoned by the `/summon` command.
15. `dispenser` - A mob spawned in by having a dispenser use a Spawn Egg.
16. `patrol` - A mob spawned by patrol logic (used by Pillagers).
17. `trial_spawner` - A mob spawned by a Trial Spawner.
