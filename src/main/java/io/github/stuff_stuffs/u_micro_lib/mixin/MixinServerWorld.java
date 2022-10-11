package io.github.stuff_stuffs.u_micro_lib.mixin;

import io.github.stuff_stuffs.u_micro_lib.api.common.block.UnloadableBlockEntity;
import io.github.stuff_stuffs.u_micro_lib.impl.micro.MicroBlockHandleImpl;
import io.github.stuff_stuffs.u_micro_lib.internal.common.micro.MicroBlockWorldExtensions;
import io.github.stuff_stuffs.u_micro_lib.internal.common.network.MicroBlockBreakSender;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld implements MicroBlockWorldExtensions {
    @Inject(method = "unloadEntities", at = @At("HEAD"))
    private void unloadEntityHook(final WorldChunk chunk, final CallbackInfo ci) {
        for (final BlockEntity entity : chunk.getBlockEntities().values()) {
            if (entity instanceof UnloadableBlockEntity unloadable) {
                unloadable.onUnload();
            }
        }
    }

    @Override
    public void microBlockBreakInfo(final int entityId, final long id, final int stage) {
        final ServerWorld serverWorld = (ServerWorld) (Object) this;
        final BlockPos blockPos = canonicalPos(new MicroBlockHandleImpl(serverWorld.getRegistryKey(), id));
        if (blockPos == null) {
            return;
        }
        final Collection<ServerPlayerEntity> around = PlayerLookup.around(serverWorld, blockPos, 64);
        MicroBlockBreakSender.send(entityId, id, stage, around);
    }
}
