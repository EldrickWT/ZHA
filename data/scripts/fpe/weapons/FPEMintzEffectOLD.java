package data.scripts.fpe.weapons;

import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundAPI;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.IntervalUtil;

//From FPE, so I wouldn't have to reinvent the wheel from the TachyonLance file as this IS the level of nasty I was looking to start working -from-.--Eld 2019
// Modernized for 2023 -- Eld 2023

public class FPEMintzEffectOLD implements BeamEffectPlugin {

	private IntervalUtil fireInterval = new IntervalUtil(0.25f, 1.75f);
	private boolean wasZero = true;
	
	
	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		CombatEntityAPI target = beam.getDamageTarget();
		if (target instanceof ShipAPI && beam.getBrightness() >= 1f) {
			float dur = beam.getDamage().getDpsDuration();
			// needed because when the ship is in fast-time, dpsDuration will not be reset every frame as it should be
			if (!wasZero) dur = 0;
			wasZero = beam.getDamage().getDpsDuration() <= 0;
			fireInterval.advance(dur);
			
			if (fireInterval.intervalElapsed()) {
				ShipAPI ship = (ShipAPI) target;
				boolean hitShield = target.getShield() != null && target.getShield().isWithinArc(beam.getTo());
				float pierceChance = ((ShipAPI)target).getHardFluxLevel() - 0.1f;
				pierceChance *= ship.getMutableStats().getDynamic().getValue(Stats.SHIELD_PIERCED_MULT);
				
				boolean piercedShield = hitShield && (float) Math.random() < pierceChance;
				//piercedShield = true;
				
				if (!hitShield || piercedShield) {
					Vector2f point = beam.getRayEndPrevFrame();
					float emp = beam.getDamage().getFluxComponent() * 5f;
					float dam = beam.getDamage().getDamage() * 5f;
					Global.getCombatEngine().spawnEmpArcPierceShields(beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(), DamageType.HIGH_EXPLOSIVE, dam, emp, 100000f, "tachyon_lance_emp_impact", beam.getWidth() + 9f, beam.getFringeColor(), beam.getCoreColor());
				}
			}
		}
//			Global.getSoundPlayer().playLoop("system_emp_emitter_loop", beam.getDamageTarget(), 1.5f, beam.getBrightness() * 0.5f, beam.getTo(), new Vector2f());
	}
}
