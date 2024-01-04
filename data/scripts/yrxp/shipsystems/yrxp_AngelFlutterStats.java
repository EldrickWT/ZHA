package data.scripts.yrxp.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.*;
import org.lwjgl.util.vector.Vector2f;
//import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class yrxp_AngelFlutterStats extends BaseShipSystemScript
{
	//private static Logger log = Global.getLogger(yrxp_AngelFlutterStats.class);
	private static final Color MIRV_SMOKE = new Color(255, 100, 255, 125);
	//public static final float MAX_TURN_BONUS = 310f;//Full-circle
	public static final float MAX_TURN_BONUS = 140f;//Half-circle
	public static final float TURN_ACCEL_BONUS = 50f;
	public static final float INSTANT_BOOST_FLAT = 500f;
	public static final float INSTANT_BOOST_MULT = 5f;
	public static final float SPEED_FALLOFF_PER_SEC = 0.225f;
	public static final float FORWARD_PENALTY = 0.2f;
	public static final float REVERSE_PENALTY = 0.4f;
	public static final float IN_OVERRIDE = 0.2f;
	public static final float ACTIVE_OVERRIDE = 0.2f;
	public static final float OUT_OVERRIDE = 1f;
	public static final int USES_OVERRIDE = 1;
	public static final float REGEN_OVERRIDE = 0.01f;
	private static final float EXTEND_TIME = 0.1f;
	private static final float MAX_FRAC_OUT = 0.15f / OUT_OVERRIDE;
	private static final float BOOST_MULT = 3f;
	private static final Color ENGINE_COLOR_STANDARD = new Color(255, 255, 230);
	private static final Color CONTRAIL_COLOR_STANDARD = new Color(255, 255, 255, 75);
	private static final Color BOOST_COLOR_STANDARD = new Color(255, 255, 255, 200);
	private static final Vector2f ZERO = new Vector2f();
	private final Object STATUSKEY1 = new Object();
	private final Object ENGINEKEY1 = new Object();
	private final Object ENGINEKEY2 = new Object();
	private final Map<Integer, Float> engState = new HashMap<Integer, Float>();
	private boolean started = false;
	private boolean ended = false;
	//private final IntervalUtil interval = new IntervalUtil(0.2f, 0.2f);
	private final IntervalUtil interval = new IntervalUtil(0.05f, 0.05f);
	private float boostScale = 0.75f;
	private float boostVisualDir = 0f;
	private boolean boostForward = false;
	private Map<Vector2f, Float> renderList = new HashMap<Vector2f, Float>();
	private boolean doOnce = false;
	private Vector2f lastPosition;
	private float lastFacing;
	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel)
	{
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship == null)
		{
			return;
		}
		float shipRadius = ship.getCollisionRadius();
		float amount = Global.getCombatEngine().getElapsedInLastFrame();
		if (Global.getCombatEngine().isPaused())
		{
			amount = 0f;
		}
		Color ENGINE_COLOR = ENGINE_COLOR_STANDARD;
		Color CONTRAIL_COLOR = CONTRAIL_COLOR_STANDARD;
		Color BOOST_COLOR = BOOST_COLOR_STANDARD;
		float afterimageIntensity = 1f;
		ship.getEngineController().fadeToOtherColor(ENGINEKEY1, ENGINE_COLOR, CONTRAIL_COLOR, effectLevel, 1f);
		ship.getEngineController().extendFlame(ENGINEKEY2, 0f, 1f * effectLevel, 3f * effectLevel);
		if(!doOnce)
		{
			this.doOnce = true;
			this.launchFlares(ship);
		}
		/*
		*
		* Launches a missile barrage while evading
		*
		*/
		interval.advance(amount);
		if (interval.intervalElapsed())
		{
			//float randRange = (float) Math.sqrt(shipRadius) * 0.5f * afterimageIntensity * boostScale;
			//Vector2f randLoc = MathUtils.getRandomPointInCircle(ZERO, randRange);
			//renderList.put(randLoc, randRange);
			//this.launchMissile(ship);
			float currentSideBaseAngle = (Math.random()>0.5?ship.getFacing()+180:ship.getFacing()-180);
			float currentAngle = MathUtils.getRandomNumberInRange(-20, 20);
			float currentRadius = MathUtils.getRandomNumberInRange(20f, 30f);
			this.launchMissile(ship, currentRadius, currentSideBaseAngle, currentAngle);
			//float angle = MathUtils.getRandomNumberInRange(0,360);
			//if (angle < 0f)
			//{
			//angle += 360f;
			//}
			//else if (angle >= 360f)
			//{
			//angle -= 360f;
			//}
			//Vector2f location = MathUtils.getPointOnCircumference(ship.getLocation(), 4.5f, angle);
			//MissileAPI newMissile = (MissileAPI) Global.getCombatEngine().spawnProjectile(ship, null, "yrxp_lrbm_10k", location, angle, null);
			//newMissile.setFromMissile(true);
			//CombatEntityAPI target = ship.getShipTarget();
			//if(target != null)
			//{
			//GuidedMissileAI subAI = (GuidedMissileAI) newMissile.getAI();
			//subAI.setTarget(target);
			//}
		}
		this.renderAfterImages(ship, ship.getCollisionRadius());
		//log.info("STATE: " + state);
		if (!ended)
		{
			/* Unweighted direction calculation for visual purposes - 0 degrees is forward */
			Vector2f direction = new Vector2f();
			if (ship.getEngineController().isAccelerating())
			{
				direction.y += 1f;
			}
			else if (ship.getEngineController().isAcceleratingBackwards() || ship.getEngineController().isDecelerating())
			{
				direction.y -= 1f;
			}
			if (ship.getEngineController().isStrafingLeft())
			{
				direction.x -= 1f;
			}
			else if (ship.getEngineController().isStrafingRight())
			{
				direction.x += 1f;
			}
			if (direction.length() <= 0f)
			{
				direction.y = 1f;
			}
			boostVisualDir = MathUtils.clampAngle(VectorUtils.getFacing(direction) - 90f);
		}
		if (state == State.IN)
		{
			if (!started)
			{
				Global.getSoundPlayer().playSound("yrxp_evade", 1f, 1f, ship.getLocation(), ZERO);
				started = true;
			}
			List<ShipEngineAPI> engList = ship.getEngineController().getShipEngines();
			int engSize = engList.size();
			for (int i = 0; i < engSize; i++)
			{
				ShipEngineAPI eng = (ShipEngineAPI) engList.get(i);
				if (eng.isSystemActivated())
				{
					float targetLevel = getSystemEngineScale(eng, boostVisualDir) * 0.4f;
					Float currLevel = (Float) engState.get(i);
					if (currLevel == null)
					{
						currLevel = 0f;
					}
					if (currLevel > targetLevel)
					{
						currLevel = Math.max(targetLevel, currLevel - (amount / EXTEND_TIME));
					}
					else
					{
						currLevel = Math.min(targetLevel, currLevel + (amount / EXTEND_TIME));
					}
					engState.put(i, currLevel);
					ship.getEngineController().setFlameLevel(eng.getEngineSlot(), currLevel);
				}
			}
		}
		if (state == State.OUT || state == State.IN  || state == State.ACTIVE)
		{
			/* Black magic to counteract the effects of maneuvering penalties/bonuses on the effectiveness of this system */
			float decelMult = Math.max(0.5f, Math.min(2f, stats.getDeceleration().getModifiedValue() / stats.getDeceleration().getBaseValue()));
			float adjFalloffPerSec = SPEED_FALLOFF_PER_SEC * (float) Math.pow(decelMult, 0.5);
			float maxDecelPenalty = 1f / decelMult;
			stats.getMaxTurnRate().unmodify(id);
			stats.getDeceleration().modifyMult(id, this.lerp(1f, maxDecelPenalty, effectLevel));
			stats.getTurnAcceleration().modifyPercent(id, TURN_ACCEL_BONUS * effectLevel);
			if (boostForward)
			{
				ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
				ship.blockCommandForOneFrame(ShipCommand.ACCELERATE_BACKWARDS);
				ship.blockCommandForOneFrame(ShipCommand.DECELERATE);
			}
			else
			{
				//ship.blockCommandForOneFrame(ShipCommand.ACCELERATE);
			}
			/* Quickly unapply the instant repair buff */
			stats.getCombatEngineRepairTimeMult().unmodify(id);
			if (amount > 0f)
			{
				ship.getVelocity().scale((float) Math.pow(adjFalloffPerSec, amount));
			}
			List<ShipEngineAPI> engList = ship.getEngineController().getShipEngines();
			for (int i = 0; i < engList.size(); i++)
			{
				ShipEngineAPI eng = (ShipEngineAPI) engList.get(i);
				if (eng.isSystemActivated())
				{
					float targetLevel = getSystemEngineScale(eng, boostVisualDir) * effectLevel;
					if (targetLevel >= (1f - MAX_FRAC_OUT))
					{
						targetLevel = 1f;
					}
					else
					{
						targetLevel = targetLevel / (1f - MAX_FRAC_OUT);
					}
					engState.put(i, targetLevel);
					ship.getEngineController().setFlameLevel(eng.getEngineSlot(), targetLevel);
				}
			}
		}
		if (state == State.ACTIVE)
		{
			stats.getMaxTurnRate().modifyPercent(id, MAX_TURN_BONUS);
			ship.getEngineController().getExtendLengthFraction().advance(amount * 2f);
			ship.getEngineController().getExtendWidthFraction().advance(amount * 2f);
			ship.getEngineController().getExtendGlowFraction().advance(amount * 2f);
			List<ShipEngineAPI> engList = ship.getEngineController().getShipEngines();
			for (int i = 0; i < engList.size(); i++)
			{
				ShipEngineAPI eng = (ShipEngineAPI) engList.get(i);
				if (eng.isSystemActivated())
				{
					float targetLevel = getSystemEngineScale(eng, boostVisualDir);
					Float currLevel = (Float) engState.get(i);
					if (currLevel == null)
					{
						currLevel = 0f;
					}
					if (currLevel > targetLevel)
					{
						currLevel = Math.max(targetLevel, currLevel - (amount / EXTEND_TIME));
					}
					else
					{
						currLevel = Math.min(targetLevel, currLevel + (amount / EXTEND_TIME));
					}
					engState.put(i, currLevel);
					ship.getEngineController().setFlameLevel(eng.getEngineSlot(), currLevel);
				}
			}
		}
		if (state == State.OUT || state == State.IN  || state == State.ACTIVE)
		{
			if (!ended)
			{
				Vector2f direction = new Vector2f();
				boostForward = false;
				boostScale = 0.75f;
				if (ship.getEngineController().isAccelerating())
				{
					direction.y += 0.75f - FORWARD_PENALTY;
					boostScale -= FORWARD_PENALTY;
					boostForward = true;
				}
				else if (ship.getEngineController().isAcceleratingBackwards() || ship.getEngineController().isDecelerating())
				{
					direction.y -= 0.75f - REVERSE_PENALTY;
					boostScale -= REVERSE_PENALTY;
				}
				if (ship.getEngineController().isStrafingLeft())
				{
					direction.x -= 1f;
					boostScale += 0.25f;
					boostForward = false;
					ship.setAngularVelocity(MAX_TURN_BONUS);
				}
				else if (ship.getEngineController().isStrafingRight())
				{
					direction.x += 1f;
					boostScale += 0.25f;
					boostForward = false;
					ship.setAngularVelocity(-MAX_TURN_BONUS);
				}
				else
				{
					//If no direction, get random direction
					if(MathUtils.getRandomNumberInRange(0, 1)> 0)
					{
						ship.giveCommand(ShipCommand.STRAFE_RIGHT, null, 0);
						ship.setAngularVelocity(-MAX_TURN_BONUS);
						direction.x += 1f;
					}else
					{
						ship.giveCommand(ShipCommand.STRAFE_LEFT, null, 0);
						ship.setAngularVelocity(MAX_TURN_BONUS);
						direction.x -= 1f;
					}
					boostScale += 0.25f;
					boostForward = false;
				}
				if (direction.length() <= 0f)
				{
					direction.y = 0.75f - FORWARD_PENALTY;
					boostScale -= FORWARD_PENALTY;
				}
				direction.normalise();
				VectorUtils.rotate(direction, ship.getFacing() - 90f, direction);
				direction.scale(((ship.getMaxSpeedWithoutBoost() * INSTANT_BOOST_MULT) + INSTANT_BOOST_FLAT) * boostScale);
				Vector2f.add(ship.getVelocity(), direction, ship.getVelocity());
				ended = true;
				float duration = (float) Math.sqrt(shipRadius) / 25f;
				ship.getEngineController().getExtendLengthFraction().advance(1f);
				ship.getEngineController().getExtendWidthFraction().advance(1f);
				ship.getEngineController().getExtendGlowFraction().advance(1f);
				for (ShipEngineAPI eng : ship.getEngineController().getShipEngines())
				{
					float level = 1f;
					if (eng.isSystemActivated())
					{
						level = getSystemEngineScale(eng, boostVisualDir);
					}
					if ((eng.isActive() || eng.isSystemActivated()) && (level > 0f))
					{
						Color bigBoostColor = new Color(this.clamp255(Math.round(0.1f * ENGINE_COLOR.getRed())), this.clamp255(Math.round(0.1f * ENGINE_COLOR.getGreen())), this.clamp255(Math.round(0.1f * ENGINE_COLOR.getBlue())), this.clamp255(Math.round(0.3f * ENGINE_COLOR.getAlpha() * level)));
						Color boostColor = new Color(BOOST_COLOR.getRed(), BOOST_COLOR.getGreen(), BOOST_COLOR.getBlue(), this.clamp255(Math.round(BOOST_COLOR.getAlpha() * level)));
						Global.getCombatEngine().spawnExplosion(eng.getLocation(), ZERO, bigBoostColor, BOOST_MULT * 4f * boostScale * eng.getEngineSlot().getWidth(), duration);
						Global.getCombatEngine().spawnExplosion(eng.getLocation(), ZERO, boostColor, BOOST_MULT * 2f * boostScale * eng.getEngineSlot().getWidth(), 0.15f);
					}
				}
				float soundScale = (float) Math.sqrt(boostScale);
				int bob = ship.getHullSize().ordinal();
				switch (bob)
				{
					case 2:
					Global.getSoundPlayer().playSound("engine_disabled", 1f, 1f * soundScale, ship.getLocation(), ZERO);
					break;
					default:
					case 3:
					Global.getSoundPlayer().playSound("engine_disabled", 0.9f, 1.1f * soundScale, ship.getLocation(), ZERO);
					break;
					case 4:
					Global.getSoundPlayer().playSound("engine_disabled", 0.8f, 1.2f * soundScale, ship.getLocation(), ZERO);
					break;
					case 5:
					Global.getSoundPlayer().playSound("engine_disabled", 0.7f, 1.3f * soundScale, ship.getLocation(), ZERO);
					break;
				}
			}
		}
	}
	private void launchFlares(ShipAPI ship)
	{
		//WeaponAPI weaponRef = ship.getAllWeapons().get(MathUtils.getRandomNumberInRange(0, ship.getAllWeapons().size()-1));
		//Right side missiles
		float currentSideBaseAngle = ship.getFacing()+90;
		float currentAngle = 0;
		for(int i = 8; i >= 0; i--)
		{
			switch(i)
			{
				case 0:
				currentAngle = 0;
				break;
				case 1:
				currentAngle = 2;
				break;
				case 2:
				currentAngle = -2;
				break;
				case 3:
				currentAngle = 4;
				break;
				case 4:
				currentAngle = -4;
				break;
				case 5:
				currentAngle = 6;
				break;
				case 6:
				currentAngle = -6;
				break;
				case 7:
				currentAngle = 8;
				break;
				case 8:
				currentAngle = -8;
				break;
				default:
				currentAngle = 10;
			}
			createFlareMissiles(ship, currentSideBaseAngle, currentAngle);
		}
		//Right side flares
		currentSideBaseAngle = ship.getFacing()+90;
		currentAngle = 0;
		for(int i = 30; i >= 0; i--)
		{
			currentAngle = MathUtils.getRandomNumberInRange(-20, 20);
			createFlareFlares(ship, currentSideBaseAngle, currentAngle);
		}
		for(int i = 30; i >= 0; i--)
		{
			currentAngle = MathUtils.getRandomNumberInRange(-20, 20);
			createFlareFlaresLong(ship, currentSideBaseAngle, currentAngle);
		}
		//Left side missiles
		currentSideBaseAngle = ship.getFacing()-90;
		currentAngle = 0;
		for(int i = 8; i >= 0; i--)
		{
			switch(i)
			{
				case 0:
				currentAngle = 0;
				break;
				case 1:
				currentAngle = 2;
				break;
				case 2:
				currentAngle = -2;
				break;
				case 3:
				currentAngle = 4;
				break;
				case 4:
				currentAngle = -4;
				break;
				case 5:
				currentAngle = 6;
				break;
				case 6:
				currentAngle = -6;
				break;
				case 7:
				currentAngle = 8;
				break;
				case 8:
				currentAngle = -8;
				break;
				default:
				currentAngle = 10;
			}
			createFlareMissiles(ship, currentSideBaseAngle, currentAngle);
		}
		//Left side flares
		currentSideBaseAngle = ship.getFacing()-90;
		currentAngle = 0;
		for(int i = 30; i >= 0; i--)
		{
			currentAngle = MathUtils.getRandomNumberInRange(-20, 20);
			createFlareFlares(ship, currentSideBaseAngle, currentAngle);
		}
		for(int i = 30; i >= 0; i--)
		{
			currentAngle = MathUtils.getRandomNumberInRange(-20, 20);
			createFlareFlaresLong(ship, currentSideBaseAngle, currentAngle);
		}
		//Rear side missiles
		currentSideBaseAngle = ship.getFacing()+180;
		currentAngle = 0;
		for(int i = 8; i >= 0; i--)
		{
			switch(i)
			{
				case 0:
				currentAngle = 0;
				break;
				case 1:
				currentAngle = 4;
				break;
				case 2:
				currentAngle = -4;
				break;
				case 3:
				currentAngle = 8;
				break;
				case 4:
				currentAngle = -8;
				break;
				case 5:
				currentAngle = 12;
				break;
				case 6:
				currentAngle = -12;
				break;
				case 7:
				currentAngle = 16;
				break;
				case 8:
				currentAngle = -16;
				break;
				default:
				currentAngle = 20;
			}
			createFlareMissiles(ship, currentSideBaseAngle, currentAngle);
		}
		//Rear side flares
		currentSideBaseAngle = ship.getFacing()+180;
		currentAngle = 0;
		float currentRadius = 0;
		for(int i = 30; i >= 0; i--)
		{
			currentAngle = MathUtils.getRandomNumberInRange(-30, 30);
			currentRadius = MathUtils.getRandomNumberInRange(20, 30);
			createFlareFlares(ship, currentRadius, currentSideBaseAngle, currentAngle);
		}
		for(int i = 30; i >= 0; i--)
		{
			currentAngle = MathUtils.getRandomNumberInRange(-20, 20);
			currentRadius = MathUtils.getRandomNumberInRange(20, 30);
			createFlareFlaresLong(ship, currentRadius, currentSideBaseAngle, currentAngle);
		}
	}
	private void createFlareMissiles(ShipAPI ship, float currentSideBaseAngle, float currentAngle)
	{
		Vector2f location = MathUtils.getPointOnCircumference(ship.getLocation(), 50f, currentSideBaseAngle+currentAngle);
		float angle = currentSideBaseAngle+currentAngle;
		if (angle < 0f)
		{
			angle += 360f;
		}
		else if (angle >= 360f)
		{
			angle -= 360f;
		}
		MissileAPI newMissile = (MissileAPI) Global.getCombatEngine().spawnProjectile(ship, null, "yrxp_lrm_angel_wing", location, angle, null);
		newMissile.setFromMissile(true);
		Global.getCombatEngine().addSmokeParticle(location, ship.getVelocity(), 30f, 0.4f, 0.5f, MIRV_SMOKE);
		Global.getSoundPlayer().playSound("launch_flare_1", 1f, 1f, location, ship.getVelocity());
		CombatEntityAPI target = ship.getShipTarget();
		if(target == null)
		{
			return;
		}
/*
if (target != null && missile.getAI() instanceof GuidedMissileAI)
{
	GuidedMissileAI subAI = (GuidedMissileAI) newMissile.getAI();
	ai.setTarget(target);
}
*/
		GuidedMissileAI subAI = (GuidedMissileAI) newMissile.getAI();
		subAI.setTarget(target);
	}
	private void createFlareFlares(ShipAPI ship, float currentSideBaseAngle, float currentAngle)
	{
		Vector2f location = MathUtils.getPointOnCircumference(ship.getLocation(), MathUtils.getRandomNumberInRange(30f, 100f), currentSideBaseAngle+currentAngle);
		float angle = currentSideBaseAngle+currentAngle;
		if (angle < 0f)
		{
			angle += 360f;
		}
		else if (angle >= 360f)
		{
			angle -= 360f;
		}
		Global.getCombatEngine().addSmokeParticle(location, ship.getVelocity(), 20f, 0.4f, 0.5f, MIRV_SMOKE);
		Global.getSoundPlayer().playSound("launch_flare_1", 1f, 1f, location, ship.getVelocity());
		MissileAPI newMissile = (MissileAPI) Global.getCombatEngine().spawnProjectile(ship, null, "yrxp_angel_flare_short", location, angle, null);
		newMissile.setFromMissile(true);
	}
	private void createFlareFlares(ShipAPI ship, float currentRadius,  float currentSideBaseAngle, float currentAngle)
	{
		Vector2f location = MathUtils.getPointOnCircumference(ship.getLocation(), currentRadius, currentSideBaseAngle+currentAngle);
		float angle = currentAngle;
		if (angle < 0f)
		{
			angle += 360f;
		}
		else if (angle >= 360f)
		{
			angle -= 360f;
		}
		Global.getCombatEngine().addSmokeParticle(location, ship.getVelocity(), 20f, 0.4f, 0.5f, MIRV_SMOKE);
		Global.getSoundPlayer().playSound("launch_flare_1", 1f, 1f, location, ship.getVelocity());
		MissileAPI newMissile = (MissileAPI) Global.getCombatEngine().spawnProjectile(ship, null, "yrxp_angel_flare_short", location, angle, null);
		newMissile.setFromMissile(true);
	}
	private void createFlareFlaresLong(ShipAPI ship, float currentSideBaseAngle, float currentAngle)
	{
		Vector2f location = MathUtils.getPointOnCircumference(ship.getLocation(), MathUtils.getRandomNumberInRange(30f, 100f), currentSideBaseAngle+currentAngle);
		float angle = currentSideBaseAngle+currentAngle;
		if (angle < 0f)
		{
			angle += 360f;
		}
		else if (angle >= 360f)
		{
			angle -= 360f;
		}
		Global.getCombatEngine().addSmokeParticle(location, ship.getVelocity(), 20f, 0.4f, 0.5f, MIRV_SMOKE);
		Global.getSoundPlayer().playSound("launch_flare_1", 1f, 1f, location, ship.getVelocity());
		MissileAPI newMissile = (MissileAPI) Global.getCombatEngine().spawnProjectile(ship, null, "yrxp_angel_flare_long", location, angle, null);
		newMissile.setFromMissile(true);
	}
	private void createFlareFlaresLong(ShipAPI ship, float currentRadius,  float currentSideBaseAngle, float currentAngle)
	{
		Vector2f location = MathUtils.getPointOnCircumference(ship.getLocation(), currentRadius, currentSideBaseAngle+currentAngle);
		float angle = currentAngle;
		if (angle < 0f)
		{
			angle += 360f;
		}
		else if (angle >= 360f)
		{
			angle -= 360f;
		}
		Global.getCombatEngine().addSmokeParticle(location, ship.getVelocity(), 20f, 0.4f, 0.5f, MIRV_SMOKE);
		Global.getSoundPlayer().playSound("launch_flare_1", 1f, 1f, location, ship.getVelocity());
		MissileAPI newMissile = (MissileAPI) Global.getCombatEngine().spawnProjectile(ship, null, "yrxp_angel_flare_long", location, angle, null);
		newMissile.setFromMissile(true);
	}
	private void launchMissile(ShipAPI ship, float currentRadius,  float currentSideBaseAngle, float currentAngle)
	{
		Vector2f location = MathUtils.getPointOnCircumference(ship.getLocation(), currentRadius, currentSideBaseAngle+currentAngle);
		float angle = currentSideBaseAngle+currentAngle;
		if (angle < 0f)
		{
			angle += 360f;
		}
		else if (angle >= 360f)
		{
			angle -= 360f;
		}
		int launchType = MathUtils.getRandomNumberInRange(0, 2);
		String launchSound, weaponID;
		switch(launchType)
		{
			case 0:
			launchSound = "launch_flare_1";
			weaponID = "yrxp_angel_flare_long";
			break;
			case 1:
			launchSound = "launch_flare_1";
			weaponID = "yrxp_angel_flare_short";
			break;
			default:
			launchSound = "launch_flare_1";
			weaponID = "yrxp_lrm_angel_wing";
			break;
		}
		Global.getCombatEngine().addSmokeParticle(location, ship.getVelocity(), 20f, 0.4f, 0.5f, MIRV_SMOKE);
		Global.getSoundPlayer().playSound(launchSound, 1f, 1f, location, ship.getVelocity());
		MissileAPI newMissile = (MissileAPI) Global.getCombatEngine().spawnProjectile(ship, null, weaponID, location, angle, null);
		newMissile.setFromMissile(true);
		if(launchType == 0 || launchType == 1)
		{
			return;
		}
		CombatEntityAPI target = ship.getShipTarget();
		if(target == null)
		{
			return;
		}
		GuidedMissileAI subAI = (GuidedMissileAI) newMissile.getAI();
		subAI.setTarget(target);
		//WeaponAPI weaponRef = ship.getAllWeapons().get(MathUtils.getRandomNumberInRange(0, ship.getAllWeapons().size()-1));
		//Vector2f location = MathUtils.getPointOnCircumference(weaponRef.getLocation(), 4.5f, weaponRef.getCurrAngle());
		//float angle = MathUtils.getRandomNumberInRange(0,360);
		//float angle = weaponRef.getCurrAngle();
		//if (angle < 0f)
		//{
		//angle += 360f;
		//}
		//else if (angle >= 360f)
		//{
		//angle -= 360f;
		//}
		//MissileAPI newMissile = (MissileAPI) Global.getCombatEngine().spawnProjectile(
		//ship, null, "yrxp_lrm_angel_wing", location, angle, null);
		//newMissile.setFromMissile(true);
		//Global.getCombatEngine().addSmokeParticle(location, ship.getVelocity(), 30f, 0.4f, 0.5f, MIRV_SMOKE);
		//Global.getSoundPlayer().playSound("launch_flare_1", 1f, 1f, location, ship.getVelocity());
		//Global.getSoundPlayer().playSound("launch_flare_1", 1f, 1f, location, ship.getVelocity());
		//CombatEntityAPI target = ship.getShipTarget();
		//if(target == null)
		//{
		//return;
		//}
		//GuidedMissileAI subAI = (GuidedMissileAI) newMissile.getAI();
		//subAI.setTarget(target);
	}
	private void launchMissile(ShipAPI ship)
	{
		WeaponAPI weaponRef = (WeaponAPI) ship.getAllWeapons().get(MathUtils.getRandomNumberInRange(0, ship.getAllWeapons().size()-1));
		Vector2f location = MathUtils.getPointOnCircumference(weaponRef.getLocation(), 4.5f, weaponRef.getCurrAngle());
		//float angle = MathUtils.getRandomNumberInRange(0,360);
		float angle = weaponRef.getCurrAngle();
		if (angle < 0f)
		{
			angle += 360f;
		}
		else if (angle >= 360f)
		{
			angle -= 360f;
		}
		MissileAPI newMissile = (MissileAPI) Global.getCombatEngine().spawnProjectile(ship, null, "yrxp_lrm_angel_wing", location, angle, null);
		newMissile.setFromMissile(true);
		Global.getCombatEngine().addSmokeParticle(location, ship.getVelocity(), 30f, 0.4f, 0.5f, MIRV_SMOKE);
		Global.getSoundPlayer().playSound("launch_flare_1", 1f, 1f, location, ship.getVelocity());
		CombatEntityAPI target = ship.getShipTarget();
		if(target == null)
		{
			return;
		}
		GuidedMissileAI subAI = (GuidedMissileAI) newMissile.getAI();
		subAI.setTarget(target);
	}
	private void renderAfterImages(ShipAPI ship, float shipRadius)
	{
		Color CONTRAIL_COLOR = CONTRAIL_COLOR_STANDARD;
		float afterimageIntensity = 1f;
		//for (Map.Entry<Vector2f, Float> renderItem : this.renderList.entrySet())
		//{
		//Color afterimageColor = new Color(CONTRAIL_COLOR.getRed(), CONTRAIL_COLOR.getGreen(), CONTRAIL_COLOR.getBlue(), this.clamp255(Math.round(0.15f * afterimageIntensity * CONTRAIL_COLOR.getAlpha())));
		//ship.addAfterimage(afterimageColor, renderItem.getKey().x, renderItem.getKey().y, -ship.getVelocity().x, -ship.getVelocity().y, renderItem.getValue(), 0f, 0.1f, 0.5f, true, false, false);
		//}
		float randRange = (float) Math.sqrt(shipRadius) * 0.5f * afterimageIntensity * boostScale;
		Vector2f randLoc = MathUtils.getRandomPointInCircle(ZERO, randRange);
		Color afterimageColor = new Color(CONTRAIL_COLOR.getRed(), CONTRAIL_COLOR.getGreen(), CONTRAIL_COLOR.getBlue(), this.clamp255(Math.round(0.15f * afterimageIntensity * CONTRAIL_COLOR.getAlpha())));
		ship.addAfterimage(afterimageColor, randLoc.x, randLoc.y, -ship.getVelocity().x, -ship.getVelocity().y, randRange, 0.0f, 0.2f, 1.0f, true, false, false);
	}
	@Override
	public void unapply(MutableShipStatsAPI stats, String id)
	{
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship == null)
		{
			return;
		}
		doOnce = false;
		started = false;
		ended = false;
		boostScale = 0.75f;
		boostVisualDir = 0f;
		boostForward = false;
		engState.clear();
		stats.getCombatEngineRepairTimeMult().unmodify(id);
		stats.getEngineDamageTakenMult().unmodify(id);
		stats.getTimeMult().unmodify(id);
		String globalId = id + "_" + ship.getId();
		Global.getCombatEngine().getTimeMult().unmodify(globalId);
		stats.getMaxTurnRate().unmodify(id);
		stats.getDeceleration().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		ship.setAngularVelocity(ship.getAngularVelocity() * 0.2f);
		ship.setJitter(this, ENGINE_COLOR_STANDARD, 0f, 5, 0f, 13f);
		ship.setJitterUnder(this, ENGINE_COLOR_STANDARD, 0f, 25, 0f, 17f);
	}
	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship)
	{
		return true;
	}
	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship)
	{
		if (ship != null)
		{
			if (ship.getEngineController().isFlamedOut())
			{
				return "FLAMED OUT";
			}
		}
		return null;
	}
	@Override
	public float getInOverride(ShipAPI ship)
	{
		if (ship != null)
		{
			return IN_OVERRIDE;
		}
		return -1;
	}
	@Override
	public float getActiveOverride(ShipAPI ship)
	{
		if (ship != null)
		{
			return ACTIVE_OVERRIDE;
		}
		return -1;
	}
	@Override
	public float getOutOverride(ShipAPI ship)
	{
		if (ship != null)
		{
			return OUT_OVERRIDE;
		}
		return -1;
	}
	@Override
	public int getUsesOverride(ShipAPI ship)
	{
		if (ship != null)
		{
			return USES_OVERRIDE;
		}
		return -1;
	}
	@Override
	public float getRegenOverride(ShipAPI ship)
	{
		if (ship != null)
		{
			return REGEN_OVERRIDE;
		}
		return -1;
	}
	public float lerp(float x, float y, float alpha)
	{
		return (1f - alpha) * x + alpha * y;
	}
	public int clamp255(int x)
	{
		return Math.max(0, Math.min(255, x));
	}
	private static float getSystemEngineScale(ShipEngineAPI engine, float direction)
	{
		float engAngle = engine.getEngineSlot().getAngle();
		if (Math.abs(MathUtils.getShortestRotation(engAngle, direction)) > 100f)
		{
			return 1f;
		}
		else
		{
			return 0f;
		}
	}
}
