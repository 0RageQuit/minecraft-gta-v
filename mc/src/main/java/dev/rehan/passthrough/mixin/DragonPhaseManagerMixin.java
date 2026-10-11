package dev.rehan.passthrough.mixin;

import dev.rehan.passthrough.CityDragonPhase;
import dev.rehan.passthrough.Passthrough;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhaseManager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderDragonPhaseManager.class)
abstract class DragonPhaseManagerMixin {
	@Shadow @Final private EnderDragon dragon;
	@Shadow private DragonPhaseInstance currentPhase;
	@Unique private CityDragonPhase passthrough$cityPhase;

	@Inject(method = "getCurrentPhase", at = @At("HEAD"), cancellable = true)
	private void passthrough$cityFlight(final CallbackInfoReturnable<DragonPhaseInstance> cir) {
		if (Passthrough.active && !dragon.level().isClientSide() && dragon.level().dimension().equals(Level.OVERWORLD)
			&& dragon.getDragonFight() == null && dragon.isAlive() && currentPhase != null
			&& currentPhase.getPhase() != EnderDragonPhase.DYING) {
			if (passthrough$cityPhase == null) passthrough$cityPhase = new CityDragonPhase(dragon);
			cir.setReturnValue(passthrough$cityPhase);
		}
	}
}
