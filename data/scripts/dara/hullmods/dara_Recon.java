package data.scripts.dara.hullmods;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.util.Misc;

public class dara_Recon extends BaseHullMod {

	private static final float ZERO_FLUX_BOOST_BONUS = 25f;	// +25 to Zero-Flux speed boost (total +75 top speed)
	private static final float SIGHT_RANGE_BONUS = 0.5f;	// +50% sight radius in combat
	
	private static final float PEAK_LOSS = 30f;		// -30 seconds Peak Performance Time
	private static final float CARGO_PENALTY = 0.5f;	// -50% cargo capacity
	
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

		stats.getZeroFluxSpeedBoost().modifyFlat(id, ZERO_FLUX_BOOST_BONUS);
		stats.getSightRadiusMod().modifyMult(id, (1f + SIGHT_RANGE_BONUS));

		stats.getPeakCRDuration().modifyFlat(id, -PEAK_LOSS);
		stats.getCargoMod().modifyPercent(id, -(100f * CARGO_PENALTY));
	}
	
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) ZERO_FLUX_BOOST_BONUS;
		if (index == 1) return "" + (int) (100f * SIGHT_RANGE_BONUS);
		if (index == 2) return "" + (int) PEAK_LOSS;
		//if (index == 3) return "" + (int) (100f * CARGO_PENALTY);
		if (index == 3) return "halved";
		
		return null;
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {

		//if (ship.getVariant().hasHullMod(HullMods.CIVGRADE)) return false;	// Let's not be mean to civilian ships, they can use this too.
		if (ship.getVariant().hasHullMod(HullMods.PHASE_FIELD)) return false;	// Phase ships can't though. That'd just be ludicrious!
		if (ship.getHullSize() != HullSize.FRIGATE) return false;		// Oh, and it's only available for frigates.

		return !ship.getVariant().getHullMods().contains("dara_recon_builtin");	// Can't be stacked with the Spectacle's built-in version
	}

	public String getUnapplicableReason(ShipAPI ship) {
		if (ship != null && ship.getHullSize() != HullSize.FRIGATE) {
			return "Can only be installed on frigates";
		}
		if (ship.getVariant().hasHullMod(HullMods.PHASE_FIELD)) {
			return "Can not be installed on phase ships";
		}
		//if (ship.getVariant().hasHullMod(HullMods.CIVGRADE)) {
		//	return "Can not be installed on civilian ships";
		//}
		if (ship.getVariant().getHullMods().contains("dara_recon_builtin")) {
			return "This ship is already equipped with reconnaissance hardware";
		}
		return null;
	}

	private Color color = new Color(150,255,225,255);	// A light, pale greenish colour
	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
			ship.getEngineController().fadeToOtherColor(this, color, null, 1f, 0.4f);
			//ship.getEngineController().extendFlame(this, 0.25f, 0.25f, 0.25f);
	}

	

}
