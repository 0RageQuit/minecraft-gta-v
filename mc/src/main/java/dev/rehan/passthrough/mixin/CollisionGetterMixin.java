package dev.rehan.passthrough.mixin;

import com.google.common.collect.Iterables;
import dev.rehan.passthrough.NativeTerrain;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adapted from minecraft-ring CollisionGetterMixin (MIT; see LICENSE-minecraft-ring). */
@Mixin(CollisionGetter.class)
public interface CollisionGetterMixin {
	@Inject(method = "getBlockCollisionsFromContext", at = @At("RETURN"), cancellable = true)
	private void passthrough$contacts(CollisionContext context, AABB area, CallbackInfoReturnable<Iterable<VoxelShape>> callback) {
		Entity entity = context instanceof EntityCollisionContext c ? c.getEntity() : null;
		var contacts = NativeTerrain.contacts(entity, area);
		if (!contacts.isEmpty()) callback.setReturnValue(Iterables.concat(callback.getReturnValue(), contacts));
	}
}
