package dev.rehan.passthrough;

import com.google.gson.JsonArray;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Live GTA contacts enter the vanilla collision solver, as in Minecraft Ring.
 * Snapshots expire so an opened door or an unloaded world cannot leave ghost walls. */
public final class NativeTerrain {
	private record Contact(AABB bounds, VoxelShape shape, boolean floor) {}
	private record Snapshot(List<Contact> contacts, long at) {}
	private static volatile Snapshot latest = new Snapshot(List.of(), 0);
	private NativeTerrain() {}
	public static boolean fresh() { return System.nanoTime() - latest.at < 250_000_000L; }
	/** A floor lift must leave room for the player's current vanilla pose. */
	private static boolean clearSupport(AABB body, double height, List<Contact> contacts) {
		AABB resting = body.move(0, height - body.minY, 0);
		for (Contact c : contacts) {
			if (!c.floor && c.bounds.intersects(resting)) return false;
		}
		return true;
	}
	public static double floorHeight(AABB body) {
		Snapshot snapshot = latest;
		if (System.nanoTime() - snapshot.at >= 250_000_000L) return Double.NaN;
		double top = Double.NEGATIVE_INFINITY;
		for (Contact c : snapshot.contacts) {
			AABB b = c.bounds;
			if (c.floor && b.maxX > body.minX && b.minX < body.maxX && b.maxZ > body.minZ && b.minZ < body.maxZ
				// A walkable support can rise by at most the vanilla 0.6m step height.
				// A higher surface is an overhang, not the floor beneath a crawling player.
				&& b.maxY <= body.minY + 0.6 && b.maxY > body.minY - 3
				&& b.maxY > top && clearSupport(body, b.maxY, snapshot.contacts)) top = b.maxY;
		}
		return Double.isFinite(top) ? top : Double.NaN;
	}
	public static void update(JsonArray boxes, int floorCount) {
		List<Contact> result = new ArrayList<>();
		for (int i = 0; i + 5 < boxes.size() && result.size() < 256; i += 6) {
			double[] v = new double[6];
			boolean valid = true;
			for (int n = 0; n < 6; n++) { v[n] = boxes.get(i + n).getAsDouble(); valid &= Double.isFinite(v[n]); }
			if (!valid || v[3] <= v[0] || v[4] <= v[1] || v[5] <= v[2]) continue;
			boolean floor = i / 6 < floorCount;
			// A solid support volume, rather than a paper-thin surface that a delayed tick can cross.
			AABB bounds = new AABB(v[0], floor ? v[4] - 2.0 : v[1], v[2], v[3], v[4], v[5]);
			result.add(new Contact(bounds, Shapes.create(bounds), floor));
		}
		latest = new Snapshot(List.copyOf(result), System.nanoTime());
	}
	public static List<VoxelShape> contacts(Entity entity, AABB area) {
		Snapshot snapshot = latest;
		if (!Passthrough.active || !Passthrough.walking || entity instanceof Player player && player.isSpectator()
			|| System.nanoTime() - snapshot.at > 500_000_000L) return List.of();
		return snapshot.contacts.stream().filter(c -> c.bounds.intersects(area)).map(Contact::shape).toList();
	}
	public static boolean replacesFloor(BlockPos pos) {
		Snapshot snapshot = latest;
		if (System.nanoTime() - snapshot.at > 500_000_000L) return false;
		for (Contact contact : snapshot.contacts) {
			AABB b = contact.bounds;
			if (contact.floor && Math.abs(b.maxY - pos.getY() - 1) < 2.0
				&& b.maxX > pos.getX() && b.minX < pos.getX() + 1 && b.maxZ > pos.getZ() && b.minZ < pos.getZ() + 1) return true;
		}
		return false;
	}

	/** Recover only small penetration of a recently measured floor. Horizontal movement stays vanilla. */
	public static void recoverFloor(Player player) {
		if (!Passthrough.active || !Passthrough.walking || player.isSpectator()
			|| player.getAbilities().flying || player.isFallFlying() || player.getDeltaMovement().y > 0.01) return;
		Snapshot snapshot = latest;
		if (System.nanoTime() - snapshot.at > 500_000_000L) return;
		AABB body = player.getBoundingBox();
		double top = player.getY();
		for (Contact contact : snapshot.contacts) {
			AABB b = contact.bounds;
			if (contact.floor && b.maxY > top && b.maxY - player.getY() <= 0.6
				&& b.maxX > body.minX + 0.02 && b.minX < body.maxX - 0.02
				&& b.maxZ > body.minZ + 0.02 && b.minZ < body.maxZ - 0.02
				&& clearSupport(body, b.maxY, snapshot.contacts)
				&& player.level().noCollision(player, body.move(0, b.maxY - player.getY(), 0))) top = b.maxY;
		}
		if (top > player.getY() + 0.0001) {
			player.setPos(player.getX(), top, player.getZ());
			player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0, 1));
			player.setOnGround(true);
			player.fallDistance = 0;
		}
	}
}
