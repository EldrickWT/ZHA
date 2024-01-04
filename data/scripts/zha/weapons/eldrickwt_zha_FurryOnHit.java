package data.scripts.zha.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import java.awt.Color;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class eldrickwt_zha_FurryOnHit implements OnHitEffectPlugin
	{
	private static final Color EXPLOSION_COLOR = new Color(119,200,137,125);
	// How likely it is that the extra damage will be applied (1 = 100% chance)
	private static final float EXTRA_DAMAGE_CHANCE = 1f;
	private static final int NUM_PARTICLES = 9;
	private static final Color PARTICLE_COLOR = new Color(119,200,137,125);
	// The sound the projectile makes if it deals extra damage
	private static final String SOUND_ID = "plasmachargeboom";
	// The damage types that the extra damage can deal (randomly selected)
	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine)
		{
		// Check if it procs
		if (Math.random() <= EXTRA_DAMAGE_CHANCE)
			{
			// Spawn visual effects
			Vector2f vel = new Vector2f(target.getVelocity());
			vel.scale(0.5f);
			engine.spawnExplosion(point, vel, EXPLOSION_COLOR, 655f, 0.15f);
			float speed = projectile.getVelocity().length();
			float facing = projectile.getFacing();
			for (int x = 0; x < NUM_PARTICLES; x++)
				{
				engine.addHitParticle(point, MathUtils.getPointOnCircumference(null, MathUtils.getRandomNumberInRange(speed * .090f, speed * .57f),MathUtils.getRandomNumberInRange(facing - 180f, facing + 180f)), 5f, 1f, 1.6f, PARTICLE_COLOR);
				}
			// Sound follows enemy that was hit
			Global.getSoundPlayer().playSound(SOUND_ID, 0.7f, 1f, target.getLocation(), target.getVelocity());
			}
		}
	}