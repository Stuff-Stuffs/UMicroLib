package io.github.stuff_stuffs.u_micro_lib.mixin;

import io.github.stuff_stuffs.u_micro_lib.api.common.block.InitializableBlockEntity;
import io.github.stuff_stuffs.u_micro_lib.internal.common.micro.MicroBlockWorldExtensions;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldChunk.class)
public class MixinWorldChunk {
    @Shadow
    @Final
    World world;

    @Inject(method = "setBlockEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/BlockEntity;cancelRemoval()V"))
    private void hook(final BlockEntity blockEntity, final CallbackInfo ci) {
        if (blockEntity instanceof InitializableBlockEntity initializableBlock) {
            ((MicroBlockWorldExtensions) world).addInitializableBlockEntity(initializableBlock);
        }
    }
}
