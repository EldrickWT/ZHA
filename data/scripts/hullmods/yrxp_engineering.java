package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonalityAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.mission.FleetSide;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import org.apache.log4j.Logger;
public class yrxp_engineering extends BaseHullMod {    

//	   private static Logger log = Global.getLogger(yrxp_engineering.class);  
    
    private static final Set<String> BLOCKED_HULLMODS = new HashSet<String>();
    static
    {
        // These hullmods will automatically be removed
        // This prevents unexplained hullmod blocking
        BLOCKED_HULLMODS.add("frontshield");
        BLOCKED_HULLMODS.add(HullMods.CONVERTED_HANGAR);
        BLOCKED_HULLMODS.add("TSC_converted_hangar");
        
    }
    
    private boolean runOnce=false;
    private float maxRange=0;
    private IntervalUtil timer = new IntervalUtil (0.5f,1.5f);
    private String ID, ERROR="yrxp_IncompatibleHullmodWarning";

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
    	if(hullSize.equals(HullSize.FIGHTER)) {
	        stats.getEngineDamageTakenMult().modifyMult(id, 0.75f);
	        stats.getCombatEngineRepairTimeMult().modifyMult(id, 0.50f);
    	}
        ID=id;
    }
        
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id){        
        for (String tmp : BLOCKED_HULLMODS) {
            if (ship.getVariant().getHullMods().contains(tmp)) {                
                ship.getVariant().removeMod(tmp);      
                ship.getVariant().addMod(ERROR);
            }
        }
        
        
    }
    
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        return "Makeshift Shield Generator";
    }
    
    //MORE VENTING AI
    
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        
    	if(ship.getHullSize().equals(HullSize.FIGHTER)) {
//    		log.info("FOUND FIGHTER!");
    		resolveFighter(ship);
    		return;
    	}
        
        if (!runOnce){
            runOnce=true;
            List<WeaponAPI> loadout = ship.getAllWeapons();
            if (loadout!=null){
                for (WeaponAPI w : loadout){
                    if (w.getType()!=WeaponAPI.WeaponType.MISSILE){
                        if (w.getRange()>maxRange){
                            maxRange=w.getRange();
                        }
                    }
                }
            }
            timer.randomize();
        }
        
        if (Global.getCombatEngine().isPaused() || ship.getShipAI() == null) {
            return;
        }
        timer.advance(amount);
        if (timer.intervalElapsed()) {
        	
        	
        	
            if (ship.getFluxTracker().isOverloadedOrVenting()) {
                return;
            }
            MissileAPI closest=AIUtils.getNearestEnemyMissile(ship);
            if (closest!=null && MathUtils.isWithinRange(ship, closest,500)){
                return;
            }
            
            if ( ship.getFluxTracker().getFluxLevel()<0.5 && AIUtils.getNearbyEnemies(ship, maxRange)!=null) {
                return;
            }
            
            //venting need
            
            float ventingNeed;
            int bob = ship.getHullSize().ordinal();
            switch (bob) {
                case 5:
                    ventingNeed = 2*(float) Math.pow(ship.getFluxTracker().getFluxLevel(),5f);
                    break;
                case 4:
                    ventingNeed = 1.5f*(float) Math.pow(ship.getFluxTracker().getFluxLevel(),4f);
                    break;
                case 3:
                    ventingNeed = (float) Math.pow(ship.getFluxTracker().getFluxLevel(),3f);
                    break;
                default:
                    ventingNeed = (float) Math.pow(ship.getFluxTracker().getFluxLevel(),2f);
                    break;
            }
            
            float hullFactor;
            switch (bob) {
                case 5:
                    hullFactor=(float) Math.pow(ship.getHullLevel(),0.4f);
                    break;
                case 4:
                    hullFactor=(float) Math.pow(ship.getHullLevel(),0.6f);
                    break;
                case 3:
                    hullFactor=ship.getHullLevel();
                    break;
                default:
                    hullFactor=(float) Math.pow(ship.getHullLevel(),2f);
                    break;
            }
            
            //situational danger
            
            float dangerFactor=0;

            List<ShipAPI> nearbyEnemies = AIUtils.getNearbyEnemies(ship, 2000f);
            for (ShipAPI enemy : nearbyEnemies) {
                //reset often with timid or cautious personalities
                FleetSide side = FleetSide.PLAYER;
                if (ship.getOriginalOwner()>0){
                    side = FleetSide.ENEMY;
                }
                if(Global.getCombatEngine().getFleetManager(side).getDeployedFleetMember(ship)!=null){
                    PersonalityAPI personality = (Global.getCombatEngine().getFleetManager(side).getDeployedFleetMember(ship)).getMember().getCaptain().getPersonalityAPI();
                    if(personality.getId().equals("timid") || personality.getId().equals("cautious")){
                        if (enemy.getFluxTracker().isOverloaded() && enemy.getFluxTracker().getOverloadTimeRemaining() > ship.getFluxTracker().getTimeToVent()) {
                            continue;
                        }
                        if (enemy.getFluxTracker().isVenting() && enemy.getFluxTracker().getTimeToVent() > ship.getFluxTracker().getTimeToVent()) {
                            continue;
                        }
                    }                
                }
                int zuul = enemy.getHullSize().ordinal();
                switch (zuul) {
                    case 5:
                        dangerFactor+= Math.max(0,3f-(MathUtils.getDistanceSquared(enemy.getLocation(), ship.getLocation())/1000000));
                        break;
                    case 4:
                        dangerFactor+= Math.max(0,2.25f-(MathUtils.getDistanceSquared(enemy.getLocation(), ship.getLocation())/1000000));
                        break;
                    case 3:
                        dangerFactor+= Math.max(0,1.5f-(MathUtils.getDistanceSquared(enemy.getLocation(), ship.getLocation())/1000000));
                        break;
                    case 2:
                        dangerFactor+= Math.max(0,1f-(MathUtils.getDistanceSquared(enemy.getLocation(), ship.getLocation())/1000000));
                        break;
                    default:
                        dangerFactor+= Math.max(0,0.5f-(MathUtils.getDistanceSquared(enemy.getLocation(), ship.getLocation())/640000));
                        break;
                }                
            }
            
//            //situational help
//            
//            float helpFactor=0;
//            
//            List<ShipAPI> nearbyAllies = AIUtils.getNearbyAllies(ship, 2000f);
//            for (ShipAPI ally : nearbyAllies) {
//                
//                if (ally.getHullSize()==ShipAPI.HullSize.CAPITAL_SHIP){
//                    helpFactor+= Math.max(0,2-(MathUtils.getDistanceSquared(ally.getLocation(), ship.getLocation())/1000000));
//                } else if (ally.getHullSize()==ShipAPI.HullSize.CRUISER){
//                    helpFactor+= Math.max(0,2-(MathUtils.getDistanceSquared(ally.getLocation(), ship.getLocation())/800000));
//                } else if (ally.getHullSize()==ShipAPI.HullSize.DESTROYER){
//                    helpFactor+= Math.max(0,2-(MathUtils.getDistanceSquared(ally.getLocation(), ship.getLocation())/600000));
//                } else if (ally.getHullSize()==ShipAPI.HullSize.FRIGATE){
//                    helpFactor+= Math.max(0,2-(MathUtils.getDistanceSquared(ally.getLocation(), ship.getLocation())/400000));
//                }
//            }
            
//            float decisionLevel= ventingNeed * hullFactor * ((helpFactor+1)/(dangerFactor+1));
            float decisionLevel = (ventingNeed*hullFactor+1)/(dangerFactor+1);
            
            if (decisionLevel >=1.5f || (ship.getFluxTracker().getFluxLevel()>0.1f && dangerFactor ==0)) {
                ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
            }
        }
    }

	private void resolveFighter(ShipAPI ship) {
		// TODO Auto-generated method stub

    	float hpPercent = ship.getHitpoints()/ship.getMaxHitpoints();
    	
    	if(hpPercent > 0.99f) {
    		return;
    	}
    	
    	if(ship.getWing() == null){
    		return;
    	}
    	
//    	log.info(":: Ordering retreat :: " + ship.getHullSpec().getHullId());
    	ship.getWing().orderReturn(ship);
//    	FighterWingAPI.orderReturn(ship);
	}    
}
