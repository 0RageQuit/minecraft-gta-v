package dev.rehan.passthrough.mixin;

import dev.rehan.passthrough.MobWar;
import dev.rehan.passthrough.Passthrough;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Boss flight normally bypasses the collision solver entirely. */
@Mixin(Entity.class)
abstract class MobPhysicsMixin {
	@Unique private boolean passthrough$restoreNoPhysics;
	@Inject(method = "move", at = @At("HEAD"))
	private void passthrough$collide(final MoverType type, final Vec3 movement, final CallbackInfo ci) {
		Entity e = (Entity)(Object)this;
		passthrough$restoreNoPhysics = Passthrough.active && e instanceof Mob && !MobWar.isProxy(e)
			&& e.level().dimension().equals(Level.OVERWORLD) && e.noPhysics;
		if (passthrough$restoreNoPhysics) e.noPhysics = false;
	}
	@Inject(method = "move", at = @At("RETURN"))
	private void passthrough$restore(final MoverType type, final Vec3 movement, final CallbackInfo ci) {
		if (passthrough$restoreNoPhysics) {
			((Entity)(Object)this).noPhysics = true;
			passthrough$restoreNoPhysics = false;
		}
	}
}
