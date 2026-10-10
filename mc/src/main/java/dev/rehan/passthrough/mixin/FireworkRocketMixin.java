package dev.rehan.passthrough.mixin;

import dev.rehan.passthrough.WorldBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mirror crossbow rockets that burst in Minecraft, including timed bursts and block hits. */
@Mixin(FireworkRocketEntity.class)
abstract class FireworkRocketMixin {
	@Inject(method = "explode", at = @At("HEAD"))
	private void passthrough$blast(final ServerLevel level, final CallbackInfo ci) {
		FireworkRocketEntity rocket = (FireworkRocketEntity) (Object) this;
		if (rocket.isShotAtAngle()) {
			WorldBridge.onExplosion(rocket.position(), 3.0f, "firework_rocket");
		}
	}
}
