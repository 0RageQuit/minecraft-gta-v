package dev.rehan.passthrough;

import com.google.gson.JsonArray;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Each mob needs GTA contacts around its own body, rather than contacts around Steve. */
public final class MobTerrain {
	private record Snapshot(List<VoxelShape> shapes, long at) {}
	private static final ConcurrentHashMap<Integer, Snapshot> contacts = new ConcurrentHashMap<>();
	private MobTerrain() {}
	public static void update(final int id, final JsonArray boxes) {
		var shapes = new java.util.ArrayList<VoxelShape>();
		for (int i = 0; i + 5 < boxes.size() && shapes.size() < 32; i += 6) {
			double[] v = new double[6]; boolean valid = true;
			for (int n = 0; n < 6; n++) { v[n] = boxes.get(i + n).getAsDouble(); valid &= Double.isFinite(v[n]); }
			if (valid && v[3] > v[0] && v[4] > v[1] && v[5] > v[2]) shapes.add(Shapes.create(new AABB(v[0],v[1],v[2],v[3],v[4],v[5])));
		}
		contacts.put(id, new Snapshot(List.copyOf(shapes), System.nanoTime()));
	}
	public static List<VoxelShape> contacts(final Entity entity, final AABB area) {
		if (!Passthrough.active || !(entity instanceof Mob) || MobWar.isProxy(entity)) return List.of();
		Snapshot snapshot = contacts.get(entity.getId());
		if (snapshot == null || System.nanoTime() - snapshot.at > 500_000_000L) return List.of();
		return snapshot.shapes.stream().filter(s -> s.bounds().intersects(area)).toList();
	}
	static void tick(final MinecraftServer server) {
		long now = System.nanoTime();
		contacts.entrySet().removeIf(e -> now - e.getValue().at > 1_000_000_000L);
		if (!Passthrough.active || server.getPlayerList().getPlayers().isEmpty()) return;
		var player = server.getPlayerList().getPlayers().get(0);
		var mobs = server.overworld().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(96),
			m -> m.isAlive() && !MobWar.isProxy(m));
		mobs.sort(java.util.Comparator.comparingDouble(player::distanceToSqr));
		StringBuilder out = new StringBuilder("{\"t\":\"mobterrain\",\"m\":[");
		int count = 0;
		for (Mob mob : mobs) {
			if (count >= 64) break;
			AABB b = mob.getBoundingBox(); var v = mob.getDeltaMovement();
			out.append(count++ == 0 ? "" : ",").append(String.format(Locale.ROOT,
				"[%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f]", mob.getId(), b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ,v.x,v.y,v.z));
		}
		Passthrough.events.accept(out.append("]}").toString());
	}
	static void clear() { contacts.clear(); }
}
