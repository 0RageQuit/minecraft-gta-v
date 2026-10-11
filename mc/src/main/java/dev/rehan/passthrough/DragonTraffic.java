package dev.rehan.passthrough;

import java.util.List;
import net.minecraft.world.phys.Vec3;

/** GTA vehicle centres, supplied by the host; never persisted into the Minecraft world. */
public final class DragonTraffic {
	private record Snapshot(List<Vec3> targets, long at) {}
	private static volatile Snapshot latest = new Snapshot(List.of(), 0);
	private DragonTraffic() {}
	public static void update(final com.google.gson.JsonArray rows) {
		var targets = new java.util.ArrayList<Vec3>();
		for (var row : rows) {
			if (targets.size() >= 64) break;
			var v = row.getAsJsonArray();
			if (v.size() != 4) continue;
			double x = v.get(1).getAsDouble(), y = v.get(2).getAsDouble(), z = v.get(3).getAsDouble();
			if (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)) targets.add(new Vec3(x, y, z));
		}
		latest = new Snapshot(List.copyOf(targets), System.nanoTime());
	}
	public static Vec3 nearest(final Vec3 from, final double maxDistanceSquared) {
		Snapshot snapshot = latest;
		if (System.nanoTime() - snapshot.at > 1_500_000_000L) return null;
		return snapshot.targets.stream().filter(p -> p.distanceToSqr(from) < maxDistanceSquared)
			.min(java.util.Comparator.comparingDouble(from::distanceToSqr)).orElse(null);
	}
}
