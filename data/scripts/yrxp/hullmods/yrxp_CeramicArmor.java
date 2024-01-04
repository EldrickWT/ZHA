package data.scripts.yrxp.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import java.util.HashSet;
import java.util.Set;

public class yrxp_CeramicArmor extends BaseHullMod {

    private final float explosive = -25f;    
    private final float energy = -25f;
    private final float kinetic = 25f;
    private final float frag = 25f;
    
    private final float toughness_bonus = 15f;

    private static final Set<String> BLOCKED_HULLMODS = new HashSet<String>();
    static {
        // These hullmods will automatically be removed
//        BLOCKED_HULLMODS.add("heavyarmor");
        BLOCKED_HULLMODS.add("SCY_reactiveArmor");
        BLOCKED_HULLMODS.add("yrxp_CeramicArmor");
    }
    private String ERROR="yrxp_IncompatibleHullmodWarning";

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        
        stats.getHighExplosiveDamageTakenMult().modifyMult(id, 1+(explosive/100));
        stats.getKineticDamageTakenMult().modifyMult(id, 1+(kinetic/100));
        stats.getEnergyDamageTakenMult().modifyMult(id, 1+(energy/100));
        stats.getFragmentationDamageTakenMult().modifyMult(id, 1+(frag/100));
        
        stats.getArmorDamageTakenMult().modifyPercent(id, toughness_bonus);
        //stats.getFragmentationDamageTakenMult().modifyMult(id, 1+(frag/100));
        //stats.getArmorDamageTakenMult().modifyMult(id, frag);
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
    
    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        
        if (index == 0) return "" + Math.round(explosive) + "%";  
        if (index == 1) return "" + Math.round(energy) + "%"; 
        if (index == 2) return "" + Math.round(kinetic) + "%";  
        if (index == 3) return "" + Math.round(frag) + "%";  
        
        if (index == 4) return "" + Math.round(toughness_bonus) + "%"; 
        
        return null;
    }
    
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        for (String tmp : BLOCKED_HULLMODS) {
            if (ship.getVariant().getHullMods().contains(tmp)) {                
                return false;
            }
        }
        return true;
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        for (String tmp : BLOCKED_HULLMODS) {
            if (ship.getVariant().getHullMods().contains(tmp)) {                
                return "Can not be installed in conjunction with "+Global.getSettings().getHullModSpec(tmp).getDisplayName();
            }
        }
        return null;
    }
}
