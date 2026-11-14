package fuel_siphoning.config;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import org.json.JSONObject;

import fuel_siphoning.ModPlugin;

public record SiphoningSettings(float fuelConsumptionMult, float fuelPriceMult,
                                float sensorProfileIncreasePercent, float fuelPerSupplyNormal,
                                float fuelPerSupplyNebulaSystem, float conversionRateMult) {
    private static final String SETTINGS_PATH = "FUEL_SIPHONING_OPTIONS.ini";
    private static SiphoningSettings current = new SiphoningSettings(1f, 1f, 300f, 1f, 2f, 1f);
    private static float originalFuelPrice;

    public static SiphoningSettings current() {
        return current;
    }

    public static boolean isLunaLibEnabled() {
        return Global.getSettings().getModManager().isModEnabled("lunalib");
    }

    public static void rememberFuelPrice() {
        originalFuelPrice = Global.getSettings().getCommoditySpec(Commodities.FUEL).getBasePrice();
    }

    public static void reload() {
        try {
            SiphoningSettings loaded = isLunaLibEnabled()
                    ? LunaFuelSiphoningSettings.read()
                    : readFile();
            loaded.applyCampaignModifiers();
            current = loaded;
        } catch (Exception e) {
            Global.getLogger(SiphoningSettings.class).error("Could not load Fuel Siphoning Reloaded settings", e);
            if (Global.getSector().getCampaignUI() != null) {
                Global.getSector().getCampaignUI().addMessage(
                        "Could not load Fuel Siphoning Reloaded settings. See starsector.log.", Color.RED);
            }
        }
    }

    private static SiphoningSettings readFile() throws Exception {
        JSONObject config = Global.getSettings().getMergedJSONForMod(SETTINGS_PATH, ModPlugin.ID);
        return new SiphoningSettings(
                (float) config.getDouble("fuelConsumptionMult"),
                (float) config.getDouble("fuelPriceMult"),
                (float) config.getDouble("sensorProfileIncreasePercent"),
                (float) config.getDouble("fuelPerSupplyNormal"),
                (float) config.getDouble("fuelPerSupplyNebulaSystem"),
                (float) config.getDouble("conversionRateMult"));
    }

    public SiphoningSettings {
        if (!Float.isFinite(fuelPerSupplyNormal) || fuelPerSupplyNormal <= 0f
                || !Float.isFinite(fuelPerSupplyNebulaSystem) || fuelPerSupplyNebulaSystem <= 0f) {
            throw new IllegalArgumentException("Fuel per supply must be a positive finite number.");
        }
        if (!Float.isFinite(conversionRateMult) || conversionRateMult < 0f) {
            throw new IllegalArgumentException("The conversion rate multiplier must be finite and nonnegative.");
        }
    }

    private void applyCampaignModifiers() {
        MutableStat fuelConsumption = Global.getSector().getPlayerFleet().getStats().getFuelUseHyperMult();
        fuelConsumption.unmodify("sun_ns_fuel_mult"); // Clean up the modifier from Nomadic Survival.
        fuelConsumption.modifyMult("sun_fs_fuel_mult", Math.max(0f, fuelConsumptionMult));
        Global.getSector().getEconomy().getCommoditySpec(Commodities.FUEL)
                .setBasePrice(Math.max(1f, originalFuelPrice * fuelPriceMult));
    }
}
