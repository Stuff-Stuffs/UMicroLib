package io.github.stuff_stuffs.u_micro_lib.mixin;

import io.github.stuff_stuffs.u_micro_lib.api.common.block.UnloadableBlockEntity;
import io.github.stuff_stuffs.u_micro_lib.internal.client.render.WorldRendererExtensions;
import io.github.stuff_stuffs.u_micro_lib.internal.common.micro.MicroBlockWorldExtensions;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld implements MicroBlockWorldExtensions {
    @Shadow @Final private WorldRenderer worldRenderer;

    @Inject(method = "unloadBlockEntities", at = @At("HEAD"))
    private void unloadHook(final WorldChunk chunk, final CallbackInfo ci) {
        for (final BlockEntity entity : chunk.getBlockEntities().values()) {
            if (entity instanceof UnloadableBlockEntity unloadable) {
                unloadable.onUnload();
            }
        }
    }

    @Override
    public void microBlockBreakInfo(int entityId, long id, int stage) {
        ((WorldRendererExtensions)this.worldRenderer).setMicroBlockBreakInfo(entityId, id, stage);
    }
}
