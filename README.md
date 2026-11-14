# Fuel Siphoning

Fuel Siphoning adds a toggleable campaign ability that converts supplies into fuel while your fleet is inside a nebula cloud. This fork targets **Starsector 0.98.5a-RC5**. The original mod is by Sundog. See the [upstream repository](https://github.com/NateNBJ/FuelSiphoning) and [original forum thread](https://fractalsoftworks.com/forum/index.php?topic=15272.0).

| Location | Fuel gained per supply |
| --- | ---: |
| Nebula clouds in ordinary systems | 1 |
| Nebula clouds inside nebula systems | 2 |

Fuel production scales with fleet fuel capacity and current burn level. It still works at a standstill. While siphoning, the fleet is easier to detect and leaves an orange contrail. Siphoning stops when you leave the cloud, run out of supplies, or fill your fuel tanks.

## Install and use

1. Exit Starsector. If upgrading, back up any custom settings and move the old Fuel Siphoning folder out of `mods`.
2. Extract the install ZIP into `mods` so that `mods/Fuel Siphoning/mod_info.json` exists.
3. Enable **Fuel Siphoning** in the launcher. Load a campaign and assign **Siphon Fuel** to a slot on the campaign ability bar if needed.
4. Enter a nebula cloud and toggle the ability on. Its tooltip shows the current yield.

With LunaLib enabled, configure the mod in LunaSettings. Otherwise, edit `FUEL_SIPHONING_OPTIONS.ini` in the installed mod folder and reload the game. The two yield settings use units of fuel per supply and start at the new defaults when upgrading. Carry over any fuel price, hyperspace consumption, sensor profile, and conversion speed settings you customized in an older INI.

## Build

Use Python 3.9+ and JDK 17 or later. Have `javac` and `jar` on `PATH`, or set `JAVA_HOME` to the JDK directory. LunaLib is needed to compile the optional settings integration.

```sh
python build.py "/path/to/Starsector" "/path/to/LunaLib/jars/LunaLib.jar"
```

The script writes `dist/fuel-siphoning.zip`. LunaLib is optional when running the mod.

## Development

- `src/fuel_siphoning/ModPlugin.java` grants the ability and manages the save and load hooks.
- `src/fuel_siphoning/campaign/abilities/` handles cloud detection, cargo conversion, and the tooltip.
- `src/fuel_siphoning/config/` reads the INI or LunaLib options and applies campaign modifiers.
- `data/` and `sun_fs/graphics/` contain the ability definition, settings UI, and icons. Keep the yield defaults in the INI, LunaSettings CSV, and Java settings aligned.

For a campaign smoke test, compare supply and fuel changes while stopped in each kind of cloud. Try topping up a nearly full tank, starting with scarce supplies, and leaving a cloud with siphoning on. Save and reload to check that the ability is offered again, then repeat with LunaLib enabled and verify that changing a yield updates the tooltip.
