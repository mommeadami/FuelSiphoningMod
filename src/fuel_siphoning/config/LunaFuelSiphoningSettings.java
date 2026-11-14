package fuel_siphoning.config;

import com.fs.starfarer.api.Global;
import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;

import fuel_siphoning.ModPlugin;

// Only loaded when LunaLib is enabled.
public class LunaFuelSiphoningSettings implements LunaSettingsListener {
    public static void addListener() {
        if (!LunaSettings.hasSettingsListenerOfClass(LunaFuelSiphoningSettings.class)) {
            LunaSettings.addSettingsListener(new LunaFuelSiphoningSettings());
        }
    }

    static SiphoningSettings read() {
        return new SiphoningSettings(
                getFloat("fuelConsumptionMult"),
                getFloat("fuelPriceMult"),
                getFloat("sensorProfileIncreasePercent"),
                getFloat("fuelPerSupplyNormal"),
                getFloat("fuelPerSupplyNebulaSystem"),
                getFloat("conversionRateMult"));
    }

    private static float getFloat(String key) {
        Float value = LunaSettings.getFloat(ModPlugin.ID, "sun_fs_" + key);
        if (value == null) {
            throw new IllegalStateException("Missing LunaLib setting " + key);
        }
        return value;
    }

    @Override
    public void settingsChanged(String modId) {
        if (ModPlugin.ID.equals(modId) && Global.getSector() != null && Global.getSector().getPlayerFleet() != null) {
            SiphoningSettings.reload();
        }
    }
}
