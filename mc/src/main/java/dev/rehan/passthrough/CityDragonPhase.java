package dev.rehan.passthrough;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.phys.Vec3;

/** Flight targets for the bridge's Overworld, where there is no End arena or exit portal. */
public final class CityDragonPhase extends AbstractDragonPhaseInstance {
	private Vec3 flightTarget;
	private int ticks;
	private int fireCooldown = 80;
	private LivingEntity victim;

	public CityDragonPhase(final EnderDragon dragon) {
		super(dragon);
		flightTarget = dragon.position().add(0, 24, 32);
	}

	@Override
	public void doServerTick(final ServerLevel level) {
		// Keep the client in a flying animation rather than the summon-time hovering pose.
		if (dragon.getEntityData().get(EnderDragon.DATA_PHASE) != EnderDragonPhase.HOLDING_PATTERN.getId()) {
			dragon.getEntityData().set(EnderDragon.DATA_PHASE, EnderDragonPhase.HOLDING_PATTERN.getId());
		}
		ticks++;
		if (victim == null || !victim.isAlive() || victim.isRemoved() || ticks % 20 == 0) {
			victim = level.getEntitiesOfClass(LivingEntity.class, dragon.getBoundingBox().inflate(96),
				e -> (MobWar.isProxy(e) || e instanceof Player p && !p.isSpectator() && !p.isCreative()) && e.isAlive())
				.stream().min(java.util.Comparator.comparingDouble(dragon::distanceToSqr)).orElse(null);
		}
		// Creative players can watch the dragon, but are not attacked.
		LivingEntity anchor = victim;
		Vec3 attack = victim == null ? null : victim.getEyePosition();
		Vec3 car = DragonTraffic.nearest(dragon.position(), attack == null ? 96 * 96 : attack.distanceToSqr(dragon.position()));
		if (car != null) attack = car;
		if (anchor == null && attack == null) {
			anchor = level.getNearestPlayer(dragon, 160);
		}
		if (anchor == null) {
			flightTarget = dragon.position().add(0, 12, 32);
			return;
		}
		int cycle = ticks % 400;
		if (attack != null && cycle >= 160 && cycle < 300) {
			// Commit to one pass; a constantly moving target makes the vanilla turn controller circle forever.
			if (cycle == 160 || flightTarget == null) {
				Vec3 approach = attack.subtract(dragon.position()).multiply(1, 0, 1).normalize();
				flightTarget = attack.add(approach.scale(18)).add(0, 3, 0);
			}
		} else if (ticks % 40 == 1 || cycle == 300) {
			double angle = ticks * Math.PI / 120.0;
			Vec3 centre = attack == null ? anchor.position() : attack;
			flightTarget = centre.add(Math.cos(angle) * 32, 20 + Math.sin(angle * 2) * 5, Math.sin(angle) * 32);
		}
		if (dragon.horizontalCollision) {
			Vec3 forward = dragon.getHeadLookVector(1).multiply(1,0,1).normalize();
			flightTarget = dragon.position().add(-forward.z * 24, 12, forward.x * 24);
		}
		// The vanilla controller descends very slowly at city-scale flight distances.
		// Smooth the vertical speed while leaving its horizontal flight and multipart collisions intact.
		Vec3 velocity = dragon.getDeltaMovement();
		double vertical = Math.clamp((flightTarget.y - dragon.getY()) * 0.04, -0.6, 0.5);
		dragon.setDeltaMovement(velocity.x, velocity.y * 0.8 + vertical * 0.2, velocity.z);
		if (fireCooldown > 0) fireCooldown--;
		if (attack != null && cycle < 160 && fireCooldown == 0 && dragon.position().distanceToSqr(attack) < 80 * 80) {
			Vec3 origin = dragon.head.position().add(0, 1, 0);
			Vec3 aim = attack.subtract(origin);
			if (aim.lengthSqr() > 1 && dragon.getHeadLookVector(1).normalize().dot(aim.normalize()) > 0.8) {
				DragonFireball ball = new DragonFireball(level, dragon, aim.normalize());
				ball.setPos(origin.x, origin.y, origin.z);
				level.addFreshEntity(ball);
				fireCooldown = 60;
			}
		}
	}

	@Override
	public Vec3 getFlyTargetLocation() { return flightTarget; }
	@Override
	public float getFlySpeed() { return 1.5F; }
	@Override
	public float getTurnSpeed() { return super.getTurnSpeed() * 2.0F; }
	@Override
	public EnderDragonPhase<? extends DragonPhaseInstance> getPhase() { return EnderDragonPhase.HOLDING_PATTERN; }
}
