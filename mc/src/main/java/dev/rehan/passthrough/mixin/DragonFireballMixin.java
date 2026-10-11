package dev.rehan.passthrough.mixin;

import dev.rehan.passthrough.Passthrough;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DragonFireball.class)
abstract class DragonFireballMixin {
	@Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
	private void passthrough$explosive(final HitResult hit, final CallbackInfo ci) {
		DragonFireball ball = (DragonFireball)(Object)this;
		if (Passthrough.active && !ball.isRemoved() && ball.level() instanceof ServerLevel level
			&& level.dimension().equals(Level.OVERWORLD)) {
			level.explode(ball, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, 3.0F, Level.ExplosionInteraction.MOB);
			ball.discard();
			ci.cancel();
		}
	}
}
