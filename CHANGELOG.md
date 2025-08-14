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