package dev.rehan.passthrough;

import java.util.Locale;
import net.minecraft.server.MinecraftServer;

/** GTA adaptation of Minecraft Ring's environment and shared-life bridges. */
public final class EnvironmentLifeBridge {
	private static long time = Long.MIN_VALUE;
	private static int weather = -1;
	private static boolean dead;
	private EnvironmentLifeBridge() {}
	public static void reset() { time = Long.MIN_VALUE; weather = -1; dead = false; }
	public static void tick(MinecraftServer server) {
		if (!Passthrough.active) return;
		var level = server.overworld();
		long now = Math.floorMod(level.getOverworldClockTime(), 24000L);
		int rain = level.isThundering() ? 2 : level.isRaining() ? 1 : 0;
		if (now != time || rain != weather || server.getTickCount() % 100 == 0) {
			time = now; weather = rain;
			Passthrough.events.accept("{\"t\":\"environment\",\"time\":" + now + ",\"weather\":" + rain + "}");
		}
		for (var player : server.getPlayerList().getPlayers()) {
			boolean died = !player.isAlive();
			if (died && !dead) Passthrough.events.accept("{\"t\":\"mcdeath\"}");
			dead = died;
			if (server.getTickCount() % 5 == 0) Passthrough.events.accept(String.format(Locale.ROOT,
				"{\"t\":\"life\",\"survival\":%b,\"health\":%.3f}", !player.isCreative() && !player.isSpectator(), player.getHealth()));
		}
	}
	public static void damage(double amount, boolean death) {
		var server = WorldBridge.server();
		if (server == null || !Double.isFinite(amount)) return;
		server.execute(() -> {
			for (var player : server.getPlayerList().getPlayers()) {
				if (death && player.isAlive()) player.kill(server.overworld());
				else if (amount > 0 && !player.isCreative() && !player.isSpectator())
					player.hurtServer(server.overworld(), player.damageSources().generic(), (float)Math.min(amount, 1000));
			}
		});
	}
}
