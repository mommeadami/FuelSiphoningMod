package fuel_siphoning;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

import fuel_siphoning.config.LunaFuelSiphoningSettings;
import fuel_siphoning.config.SiphoningSettings;

public class ModPlugin extends BaseModPlugin {
    public static final String ID = "sun_fuel_siphoning";
    public static final String ABILITY_ID = "sun_fs_siphon_fuel";

    @Override
    public void onApplicationLoad() {
        SiphoningSettings.rememberFuelPrice();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        SiphoningSettings.reload();
        if (SiphoningSettings.isLunaLibEnabled()) {
            LunaFuelSiphoningSettings.addListener();
        }
        if (!Global.getSector().getPlayerFleet().hasAbility(ABILITY_ID)) {
            grantAbility();
        }
    }

    // Keep the ability instance out of saves so they can be loaded without this mod.
    @Override
    public void beforeGameSave() {
        Global.getSector().getCharacterData().removeAbility(ABILITY_ID);
    }

    @Override
    public void afterGameSave() {
        grantAbility();
    }

    private void grantAbility() {
        Global.getSector().getCharacterData().addAbility(ABILITY_ID);
    }
}
