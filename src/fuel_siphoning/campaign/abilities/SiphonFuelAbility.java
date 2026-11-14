package fuel_siphoning.campaign.abilities;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.fleet.FleetMemberViewAPI;
import com.fs.starfarer.api.impl.campaign.abilities.BaseToggleAbility;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import com.fs.starfarer.campaign.fleet.CargoData;

import fuel_siphoning.config.SiphoningSettings;

public class SiphonFuelAbility extends BaseToggleAbility {
    private static final Color CONTRAIL_COLOR = new Color(255, 97, 27, 80);
    // RC5's NebulaTerrainPlugin applies this modifier while a fleet is in a cloud.
    private static final String NEBULA_VISIBILITY_MOD = Terrain.NEBULA + "_stat_mod_1";

    public float getFuelPerSupply(boolean inNebulaSystem) {
        SiphoningSettings settings = SiphoningSettings.current();
        return inNebulaSystem ? settings.fuelPerSupplyNebulaSystem() : settings.fuelPerSupplyNormal();
    }

    @Override
    protected String getActivationText() {
        CampaignFleetAPI fleet = getFleet();
        if (fleet == null) return null;
        CargoAPI cargo = fleet.getCargo();
        boolean hasSupplies = availableSupplies(cargo) > 0f;
        boolean hasFuelSpace = cargo.getFuel() + fractionalCommodity(cargo, Commodities.FUEL) < cargo.getMaxFuel();
        return hasSupplies && hasFuelSpace ? "Siphoning Fuel" : null;
    }

    @Override
    protected void activateImpl() {}

    @Override
    public boolean showActiveIndicator() {
        return isActive();
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        String status = turnedOn ? " (on)" : " (off)";
        LabelAPI title = tooltip.addTitle(spec.getName() + status);
        title.highlightLast(status);
        title.setHighlightColor(Misc.getGrayColor());

        float pad = 10f;
        tooltip.addPara("Synthesize fuel using the trace antimatter and heavy isotopes of hydrogen found in nebulae.", pad);
        if (!hasFuelSource()) {
            tooltip.addPara("Your fleet must be within a nebula in order to siphon fuel.", Misc.getNegativeHighlightColor(), pad);
        } else {
            boolean inNebulaSystem = getFleet().getContainingLocation().isNebula();
            Color highlight = inNebulaSystem ? Misc.getPositiveHighlightColor() : Misc.getHighlightColor();
            String density = (inNebulaSystem ? "high" : "low") + " density nebula";
            String fuelPerSupply = Misc.getRoundedValueMaxOneAfterDecimal(getFuelPerSupply(inNebulaSystem));
            String canOrIs = isActive() ? "is siphoning" : "can siphon";
            tooltip.addPara("Your fleet is within a %s and " + canOrIs + " %s fuel per unit of supplies.",
                    pad, Misc.getTextColor(), highlight, density, fuelPerSupply);
        }

        tooltip.addPara("Fuel is converted faster when your fleet has a high burn level and fuel capacity.", pad);
        tooltip.addPara("Increases the range at which the fleet can be detected by %s and consumes supplies in exchange for fuel.",
                pad, Misc.getHighlightColor(), (int) SiphoningSettings.current().sensorProfileIncreasePercent() + "%");
        addIncompatibleToTooltip(tooltip, expanded);
    }

    @Override
    public boolean hasTooltip() {
        return true;
    }

    @Override
    protected void applyEffect(float amount, float level) {
        CampaignFleetAPI fleet = getFleet();
        if (fleet == null || !isActive()) return;

        if (!hasFuelSource()) {
            deactivate();
            return;
        }

        CargoAPI cargo = fleet.getCargo();
        float supplies = availableSupplies(cargo);
        float fuelCapacity = cargo.getMaxFuel();
        float fuelSpace = fuelCapacity - (cargo.getFuel() + fractionalCommodity(cargo, Commodities.FUEL));
        if (supplies <= 0f) {
            stopSiphoning("Out of supplies");
            return;
        }
        if (fuelSpace <= 0f) {
            stopSiphoning("Full of fuel");
            return;
        }

        SiphoningSettings settings = SiphoningSettings.current();
        fleet.getStats().getDetectedRangeMod().modifyPercent(getModId(), settings.sensorProfileIncreasePercent(), "Siphoning fuel");

        // Burn and tank size set fuel produced per day. Density only changes the supply cost.
        float days = Global.getSector().getClock().convertToDays(amount);
        float burnFactor = Math.min(21f, fleet.getCurrBurnLevel() + 1f) / 21f;
        float fuelPerSupply = getFuelPerSupply(fleet.getContainingLocation().isNebula());
        float intendedFuel = days * fuelCapacity * settings.conversionRateMult() * level * burnFactor;
        float fuelFromSupplies = supplies * fuelPerSupply;
        float fuelAdded = Math.min(intendedFuel, Math.min(fuelSpace, fuelFromSupplies));
        if (fuelAdded > 0f) {
            cargo.removeSupplies(Math.min(supplies, fuelAdded / fuelPerSupply));
            cargo.addFuel(fuelAdded);
        }

        if (fuelAdded >= fuelSpace) {
            stopSiphoning("Full of fuel");
        } else if (fuelAdded >= fuelFromSupplies) {
            stopSiphoning("Out of supplies");
        } else {
            for (FleetMemberViewAPI view : fleet.getViews()) {
                view.getContrailColor().shift("sun_fs_wake", CONTRAIL_COLOR, getActivationDays(), 2, 1f);
                view.getContrailWidthMult().shift("sun_fs_wake", 6, getActivationDays(), 2, 1f);
            }
        }
    }

    private float availableSupplies(CargoAPI cargo) {
        return cargo.getCommodityQuantity(Commodities.SUPPLIES) + fractionalCommodity(cargo, Commodities.SUPPLIES);
    }

    private float fractionalCommodity(CargoAPI cargo, String commodity) {
        // RC5 stores player cargo fractions separately from the visible commodity stacks.
        return cargo instanceof CargoData data ? data.getPartial(CargoAPI.CargoItemType.RESOURCES.name() + commodity) : 0f;
    }

    private void stopSiphoning(String message) {
        entity.addFloatingText(message, Misc.setAlpha(entity.getIndicatorColor(), 255), 0.5f);
        deactivate();
    }

    @Override
    public boolean isUsable() {
        return isActive() || hasFuelSource();
    }

    protected boolean hasFuelSource() {
        CampaignFleetAPI fleet = getFleet();
        return fleet != null && fleet.getContainingLocation() != null
                && fleet.getStats().getDetectedRangeMod().getMultBonuses().containsKey(NEBULA_VISIBILITY_MOD);
    }

    @Override
    protected void deactivateImpl() {
        cleanupImpl();
    }

    @Override
    protected void cleanupImpl() {
        CampaignFleetAPI fleet = getFleet();
        if (fleet != null) {
            fleet.getStats().getDetectedRangeMod().unmodify(getModId());
        }
    }
}
