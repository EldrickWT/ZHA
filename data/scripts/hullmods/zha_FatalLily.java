package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class zha_FatalLily extends BaseHullMod {

	public static final float SYSTEM_RELOAD_RATE = 55f;
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getBallisticRoFMult().modifyMult(id, 1f + SYSTEM_RELOAD_RATE * 0.01f);
		stats.getEnergyRoFMult().modifyMult(id, 1f + SYSTEM_RELOAD_RATE * 0.01f);
		stats.getMissileRoFMult().modifyMult(id, 1f + SYSTEM_RELOAD_RATE * 0.01f);
	}
	
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) SYSTEM_RELOAD_RATE + "%";
		return null;
	}


}
