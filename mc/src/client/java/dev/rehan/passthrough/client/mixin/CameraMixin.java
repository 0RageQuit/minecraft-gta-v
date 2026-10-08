package dev.rehan.passthrough.client.mixin;

import dev.rehan.passthrough.client.FrameExporter;
import dev.rehan.passthrough.client.HostState;
import dev.rehan.passthrough.client.PlayerSync;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The host's camera replaces the player's: position, rotation (with roll), field of view and first/third person. */
@Mixin(Camera.class)
abstract class CameraMixin {
	private static final float DEG = (float) (Math.PI / 180.0);
	@Shadow @Final private static Vector3fc FORWARDS;
	@Shadow @Final private static Vector3fc UP;
	@Shadow @Final private static Vector3fc LEFT;
	@Shadow @Final private Vector3f forwards;
	@Shadow @Final private Vector3f up;
	@Shadow @Final private Vector3f left;
	@Shadow @Final private Quaternionf rotation;
	@Shadow private float xRot;
	@Shadow private float yRot;
	@Shadow private boolean detached;
	@Shadow private int matrixPropertiesDirty;
	@Shadow private float depthFar;
	@Shadow private float eyeHeight;
	@Shadow private float eyeHeightOld;

	@Shadow
	protected abstract void setPosition(double x, double y, double z);

	@Inject(method = "update", at = @At("HEAD"))
	private void passthrough$beginFrame(final DeltaTracker deltaTracker, final CallbackInfo ci) {
		HostState.beginFrame();
		float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
		// Use Minecraft's own smoothed pose height, including crouching and flight.
		PlayerSync.frame(partialTick, Mth.lerp(partialTick, this.eyeHeightOld, this.eyeHeight));
	}

	@Inject(method = "alignWithEntity", at = @At("TAIL"))
	private void passthrough$hostCamera(final float partialTicks, final CallbackInfo ci) {
		HostState.Pose p = HostState.frame();
		if (p == null) {
			return;
		}

		this.xRot = p.pitch();
		this.yRot = p.yaw();
		this.rotation.rotationYXZ((float) Math.PI - p.yaw() * DEG, -p.pitch() * DEG, p.roll() * DEG);
		FORWARDS.rotate(this.rotation, this.forwards);
		UP.rotate(this.rotation, this.up);
		LEFT.rotate(this.rotation, this.left);
		this.matrixPropertiesDirty |= 3;
		double x = p.x(), y = p.y(), z = p.z();
		LocalPlayer player = Minecraft.getInstance().player;
		if (p.drive() && player != null) {
			// flight chase cam: the host framed Steve at p.p*; keep that framing exactly, wherever he is drawn now
			Vec3 at = PlayerSync.renderPosition(player, partialTicks);
			x += at.x - p.px();
			y += at.y - p.py();
			z += at.z - p.pz();
		}
		this.setPosition(x, y, z);
		// GTA pulls its camera in to the head against walls: then Minecraft's camera would be inside Steve's head
		boolean inside = player != null && player.getEyePosition(partialTicks).distanceToSqr(x, y, z) < 0.8 * 0.8;
		this.detached = !p.firstPerson() && !inside;
	}

	@Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
	private void passthrough$hostFov(final float partialTicks, final CallbackInfoReturnable<Float> cir) {
		// Preserve Minecraft's options and vanilla sprint/flight/item FOV effects.
		// The native camera receives this target on the following exported frame.
		PlayerSync.cameraFov(cir.getReturnValue());
		HostState.Pose p = HostState.frame();
		if (p != null) {
			// Render a guard band for timewarp: newly exposed screen edges need real pixels.
            // The exported camera FOV describes this wider world projection; GTA keeps
            // the vanilla visible FOV. HUD projection is calculated separately.
            // Flight uses camera-relative compositing and does not need timewarp.
            boolean cameraLocked = p.drive() && (!HostState.walking() ||
                (Minecraft.getInstance().player != null && Minecraft.getInstance().player.isFallFlying()));
            float worldFov = cameraLocked ? p.fov() : (float) Math.toDegrees(
                2.0 * Math.atan(Math.tan(Math.toRadians(p.fov()) * 0.5) * 1.25));
            cir.setReturnValue(worldFov);
		}
	}

	@Inject(method = "update", at = @At("TAIL"))
	private void passthrough$planes(final DeltaTracker deltaTracker, final CallbackInfo ci) {
		FrameExporter.setFar(this.depthFar);
	}
}
