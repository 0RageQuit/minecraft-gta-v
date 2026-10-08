package dev.rehan.passthrough.mixin;

import dev.rehan.passthrough.NativeTerrain;
import dev.rehan.passthrough.Passthrough;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only replace the coarse host floor when a fresh, precise support exists. */
@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class TerrainFloorMixin {
	@Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;", at = @At("HEAD"), cancellable = true)
	private void passthrough$floor(BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> callback) {
		if (Passthrough.active && Passthrough.walking && ((BlockState)(Object)this).is(Blocks.BARRIER)
			&& context instanceof EntityCollisionContext entity && entity.getEntity() instanceof Player
			&& NativeTerrain.replacesFloor(pos)) callback.setReturnValue(Shapes.empty());
	}
}
