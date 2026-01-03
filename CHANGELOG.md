## 2.0.0
- Update all weights to be in line with Apotheosis weight changes
- Curios now support Spell Trigger and Spell Effect affixes, only for Spell Damage/Heal+Melee Hit Triggers (not Hurt/Projectile Hit)
- `school_attribute`, `spell_effect`, `mana_cost`, `spell_level`, and `spell_trigger` now support optional filtering and multiple schools.
- Updated Chinese Translation (`zh_cn`) - Thanks ZetaY!
- Updated other affix translations (may interfere with the updated Chinese and other keys)

## 1.9.2
- Temporarily disable the Cleanse cast affix due to a potential rendering crash when it's cast by an Invader

## 1.9.1
- Fix rare NPE with Mob Grinding Utils or other FakePlayers (thanks xtrm-en!)
- Relicense to MIT + CC-BY-4.0

## 1.9.0
- Add compat for Hazen n' Stuff 1.3.0+ Radiance/Shadow schools, and new spells

## 1.8.0
- Adding an Upgrade Orb to an item now allows it to roll affixes from that school. For example, if you add a fire upgrade orb to a Villager spell book, it can roll both fire and holy affixes.
- Initial support for ESS: Requiem (Ender's Spells n' Stuff support removed)

## 1.7.3
- Fix Frostbite affix's spell target being SELF instead of TARGET
- Remove unnecessary check+message for cooldown/learning/mana from affix spellcasts

## 1.7.2
- Add Staff Festive Affix (requires Apotheosis 8.4.0+)

## 1.7.1
- Add Chinese translation, thanks ZHAY10086!
- Add Brazilian Portuguese translation, thanks PrincessStelllar! 

## 1.7.0
- Fix outdated compat for Alshanex's Familiars sound school
- Nerf spell power bonus from Warlord gems 30% -> 22.5% 
- Fix Blood Lord gems receiving Holy Spell Power instead of Blood Spell Power
- Evocation gems/affixes are now properly themed around trickery and conjuration
- Changed a few other affix names

## 1.6.1
- Buffed a few unique gem bonuses (ex. Ice queen gem Ice Spell Power 30% -> 40%)
- Removed unusable Gem of the Earth extra bonus 

## 1.6.0
- Fix Flawed Ballast gems giving 80% spell power
- Added support for ISS Magic From The East's Spirit school

## 1.5.1
- Fix Ice Spell Level affix bonuses not loading at all (Thanks cHin4916!)

## 1.5.0
- Spellbooks are now reforgeable! Added spellbook affixes and many spellbook gem bonuses
- Changed Spell Level affix category to BASIC_EFFECT instead of ABILITY
- Slightly lowered the chance of mob effect on-cast affixes appearing on staffs
- Spell Level bonuses now have a range instead of being a fixed level bonus based on affix tier
- Added max mana affixes to staves/spellbooks

## 1.4.0
- Added Spell Trigger Affixes - cast a spell automatically to SELF or TARGET, on SPELL_DAMAGE, SPELL_HEAL, MELEE_HIT, PROJECTILE_HIT, HURT
- School-filtered affixes now support Curios (optionally - no built-in Curio support yet, for balance)
- Spell Level Affix now works from any slot, and now shows in tooltips (optionally - still only built-in on weapons, for balance)
- Removed the Durability affix from Staffs, and just slightly improved their other affix bonuses to compensate
- Added ManaCost affix, school-filtered mana cost reduction affix for weapons only
- Added a small amount of crit chance/damage to Staffs (~1/3 of Bows/Swords)

## 1.3.0
- Add Staff-specific bonuses to most existing Apotheosis gems
- Fix Gem class name (Staff: -> Staffs:)

## 1.2.1
- Fixed gems not being obtainable in Survival

## 1.2.0
- Added a new SchoolAttributeAffix type, replacing mixin-based affix filtering
- Cleaned up internals

## 1.1.0
- Fix Spell Level affixes not working correctly
- Various internal cleanup
- Automate publishing
- Update to and require Apoth > 8.3.0

## 1.0.0
- Initial