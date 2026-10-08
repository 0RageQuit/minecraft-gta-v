package dev.rehan.passthrough.client.mixin;

import dev.rehan.passthrough.Passthrough;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** GTA captures the OS pointer. Draw a cursor into Minecraft's exported menu layer. */
@Mixin(Screen.class)
abstract class ScreenCursorMixin {
	@Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"))
	private void passthrough$cursor(GuiGraphicsExtractor graphics, int x, int y, float partialTick, CallbackInfo ci) {
		if (!Passthrough.active) return;
		graphics.nextStratum();
		for (int row = 0; row < 11; row++) {
			int width = row < 7 ? row / 2 + 1 : 2;
			int offset = row < 7 ? 0 : 2;
			graphics.fill(x + offset - 1, y + row, x + offset + width + 1, y + row + 1, 0xff101010);
			graphics.fill(x + offset, y + row, x + offset + width, y + row + 1, 0xffffffff);
		}
	}
}
