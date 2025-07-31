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