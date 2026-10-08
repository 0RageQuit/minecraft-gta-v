package dev.rehan.passthrough.client;

import com.google.gson.JsonObject;
import dev.rehan.passthrough.Passthrough;
import dev.rehan.passthrough.client.mixin.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonInfo;
import dev.rehan.passthrough.WorldBridge;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.sdl.SDLVideo;

/** Host input, applied on the client thread: the host window has the focus, so Minecraft never sees these itself. */
final class ClientInput {
	private ClientInput() {
	}

	static void handle(final Minecraft minecraft, final JsonObject m) {
		LocalPlayer player = minecraft.player;
		switch (m.get("t").getAsString()) {
			case "platform" -> {
				if (player != null && HostState.walking() && !player.getAbilities().flying && !player.isFallFlying()) {
					double dx = m.get("x").getAsDouble(), dy = m.get("y").getAsDouble(), dz = m.get("z").getAsDouble();
					if (Double.isFinite(dx + dy + dz) && dx * dx + dy * dy + dz * dz < 4) {
						player.setPos(player.getX() + dx, player.getY() + dy, player.getZ() + dz);
						player.xo += dx; player.yo += dy; player.zo += dz;
					}
				}
			}
			case "controls" -> {
				boolean on = m.get("on").getAsBoolean();
				HostState.walking(on);
				KeyMapping.releaseAll();
				var seed = m.has("pos") ? m.getAsJsonArray("pos") : null;
				WorldBridge.walking(on, seed == null ? null : new double[] {seed.get(0).getAsDouble(), seed.get(1).getAsDouble(), seed.get(2).getAsDouble()});
				if (player != null && m.has("pos")) {
					var pos = m.getAsJsonArray("pos");
					player.setPos(pos.get(0).getAsDouble(), pos.get(1).getAsDouble(), pos.get(2).getAsDouble());
					player.xo = player.xOld = player.getX(); player.yo = player.yOld = player.getY(); player.zo = player.zOld = player.getZ();
					player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
					player.getAbilities().flying = !on && player.getAbilities().mayfly;
					player.onUpdateAbilities();
					if (on) Passthrough.events.accept("{\"t\":\"perspective\",\"mode\":" + minecraft.options.getCameraType().ordinal() + "}");
				}
				if (!on && minecraft.gui.screen() != null) minecraft.gui.screen().onClose();
			}
			case "rawkey" -> {
				if (minecraft.gui.screen() != null) {
					KeyEvent event = new KeyEvent(m.get("scan").getAsInt(), m.get("code").getAsInt(), m.get("mods").getAsInt());
					if (m.get("down").getAsBoolean()) minecraft.gui.screen().keyPressed(event);
					else minecraft.gui.screen().keyReleased(event);
				}
			}
			case "character" -> {
				if (minecraft.gui.screen() != null) minecraft.gui.screen().charTyped(new CharacterEvent(m.get("cp").getAsInt()));
			}
			case "pointer" -> {
				if (minecraft.gui.screen() != null) {
					var window = minecraft.getWindow();
					minecraft.mouseHandler.onMove(window.handle(), m.get("x").getAsDouble() * window.getWidth(), m.get("y").getAsDouble() * window.getHeight(), 0, 0);
				}
			}
			case "mousebutton" -> {
				if (minecraft.gui.screen() != null) minecraft.mouseHandler.onButton(minecraft.getWindow().handle(), new MouseButtonInfo(m.get("button").getAsInt(), m.get("mods").getAsInt()), m.get("down").getAsBoolean() ? 1 : 0);
			}
			case "key" -> {
				String k = m.get("k").getAsString();
				boolean down = !m.has("down") || m.get("down").getAsBoolean();
				if (k.equals("chat") || k.equals("command")) {
					if (down && minecraft.gui.screen() == null) minecraft.gui.setScreen(new ChatScreen(k.equals("command") ? "/" : "", false));
					return;
				}
				if (k.equals("perspective")) {
					if (down && minecraft.gui.screen() == null) {
						CameraType type = minecraft.options.getCameraType().cycle();
						minecraft.options.setCameraType(type);
						Passthrough.events.accept("{\"t\":\"perspective\",\"mode\":" + type.ordinal() + "}");
					}
					return;
				}
				if (k.equals("escape")) {
					if (down && minecraft.gui.screen() != null) {
						minecraft.gui.screen().onClose();
					}

					return;
				}
				if (minecraft.gui.screen() != null && down) return;

				KeyMapping key = switch (k) {
					case "use" -> minecraft.options.keyUse;
					case "attack" -> minecraft.options.keyAttack;
					case "pick" -> minecraft.options.keyPickItem;
					case "inventory" -> minecraft.options.keyInventory;
					case "drop" -> minecraft.options.keyDrop;
					case "swap" -> minecraft.options.keySwapOffhand;
					case "forward" -> minecraft.options.keyUp;
					case "back" -> minecraft.options.keyDown;
					case "left" -> minecraft.options.keyLeft;
					case "right" -> minecraft.options.keyRight;
					case "jump" -> minecraft.options.keyJump;
					case "sneak" -> minecraft.options.keyShift;
					case "sprint" -> minecraft.options.keySprint;
					default -> null;
				};
				if (k.equals("attack") && down && player != null) {
					// a sword swing: the host hits what's in front of Steve in its own world
					Passthrough.events.accept("{\"t\":\"melee\",\"d\":" + player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 20.0 + "}");
				}

				if (key != null) {
					if (down && !key.isDown()) {
						KeyMappingAccessor access = (KeyMappingAccessor)key;
						access.passthrough$setClickCount(access.passthrough$getClickCount() + 1);
					}

					key.setDown(down);
				}
			}
			case "slot" -> {
				if (player != null) {
					player.getInventory().setSelectedSlot(Math.clamp(m.get("n").getAsInt(), 0, Inventory.getSelectionSize() - 1));
				}
			}
			case "scroll" -> {
				if (minecraft.gui.screen() != null) {
					minecraft.mouseHandler.onScroll(minecraft.getWindow().handle(), 0, m.get("d").getAsDouble());
				} else if (player != null) {
					Inventory inventory = player.getInventory();
					int size = Inventory.getSelectionSize();
					inventory.setSelectedSlot(Math.floorMod(inventory.getSelectedSlot() - m.get("d").getAsInt(), size));
				}
			}
			case "hud" -> {
				if (minecraft.gui.hud.isHidden() != m.get("hidden").getAsBoolean()) {
					minecraft.gui.hud.toggle();
				}
			}
			case "view" -> {
				// match the host's picture exactly: un-minimize/un-maximize first (resizing a maximized window is ignored)
				int w = m.get("w").getAsInt(), h = m.get("h").getAsInt();
				long handle = minecraft.getWindow().handle();
				SDLVideo.SDL_RestoreWindow(handle);
				minecraft.getWindow().setWindowed(w, h);
				SDLVideo.SDL_SetWindowSize(handle, w, h);
				SDLVideo.SDL_SyncWindow(handle);
			}
			default -> {
			}
		}
	}
}
