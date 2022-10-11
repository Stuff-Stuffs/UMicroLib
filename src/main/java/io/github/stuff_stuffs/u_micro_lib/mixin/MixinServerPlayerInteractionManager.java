package io.github.stuff_stuffs.u_micro_lib.mixin;

import io.github.stuff_stuffs.u_micro_lib.api.common.micro.MicroBlockWorld;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlock;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlockContext;
import io.github.stuff_stuffs.u_micro_lib.impl.micro.MicroBlockHandleImpl;
import io.github.stuff_stuffs.u_micro_lib.internal.common.block.entity.MicroBlockContainerBlockEntity;
import io.github.stuff_stuffs.u_micro_lib.internal.common.micro.MicroBlockWorldExtensions;
import io.github.stuff_stuffs.u_micro_lib.internal.common.micro.ServerPlayerInteractionManagerExtensions;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalLong;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class MixinServerPlayerInteractionManager implements ServerPlayerInteractionManagerExtensions {
    @Shadow
    @Final
    protected ServerPlayerEntity player;
    @Shadow
    protected ServerWorld world;

    @Shadow
    public abstract void processBlockBreakingAction(BlockPos pos, PlayerActionC2SPacket.Action action, Direction direction, int worldHeight, int sequence);

    @Shadow
    public abstract boolean isCreative();

    @Shadow
    private int tickCounter;
    @Shadow
    private int blockBreakingProgress;
    @Shadow
    private boolean mining;
    @Unique
    private OptionalLong breakingMicroBlock = OptionalLong.empty();

    @Override
    public void startBreaking(final long id, final Direction direction, final BlockPos pos) {
        breakingMicroBlock = OptionalLong.of(id);
        processBlockBreakingAction(pos, PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, direction, player.world.getTopY(), 0);
    }

    @Redirect(method = "processBlockBreakingAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;calcBlockBreakingDelta(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)F"))
    private float processBlockBreakingActionCalcBlockDeltaRedirect(final BlockState instance, final PlayerEntity entity, final BlockView view, final BlockPos pos) {
        if (breakingMicroBlock.isPresent()) {
            final MicroBlockHandleImpl handle = new MicroBlockHandleImpl(world.getRegistryKey(), breakingMicroBlock.getAsLong());
            final BlockPos canonPos = ((MicroBlockWorld) world).canonicalPos(handle);
            if (canonPos != null) {
                final BlockEntity blockEntity = world.getBlockEntity(canonPos);
                if (blockEntity instanceof MicroBlockContainerBlockEntity container) {
                    final MicroBlock<?> microBlock = container.getContainer().byId(breakingMicroBlock.getAsLong());
                    if (microBlock != null) {
                        final MicroBlockContext context = ((MicroBlockWorld) world).createContext(handle);
                        return microBlock.calcBlockBreakingDelta(context, player);
                    }
                }
            }
        }
        return instance.calcBlockBreakingDelta(player, player.world, pos);
    }

    @Redirect(method = "processBlockBreakingAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;setBlockBreakingInfo(ILnet/minecraft/util/math/BlockPos;I)V"))
    private void hook(final ServerWorld instance, final int entityId, final BlockPos pos, final int progress) {
        if (breakingMicroBlock.isPresent()) {
            ((MicroBlockWorldExtensions) world).microBlockBreakInfo(entityId, breakingMicroBlock.getAsLong(), progress);
        } else {
            instance.setBlockBreakingInfo(player.getId(), pos, progress);
        }
    }

    @Inject(method = "tryBreakBlock", at = @At("HEAD"), cancellable = true)
    private void breakHook(final BlockPos pos, final CallbackInfoReturnable<Boolean> cir) {
        if (breakingMicroBlock.isPresent()) {
            final long id = breakingMicroBlock.getAsLong();
            final MicroBlockHandleImpl handle = new MicroBlockHandleImpl(world.getRegistryKey(), id);
            final BlockPos canonicalPos = ((MicroBlockWorld) world).canonicalPos(handle);
            if (canonicalPos == null) {
                cir.setReturnValue(false);
                return;
            }
            final BlockEntity blockEntity = world.getBlockEntity(canonicalPos);
            if (!(blockEntity instanceof MicroBlockContainerBlockEntity container)) {
                cir.setReturnValue(false);
                return;
            }
            final MicroBlock<?> microBlock = container.getContainer().byId(id);
            if (microBlock == null) {
                cir.setReturnValue(false);
                return;
            }
            final MicroBlockContext context = ((MicroBlockWorld) world).createContext(handle);
            microBlock.onBreak(context, player);
            ((MicroBlockWorldExtensions) world).breakPiece(id, microBlock);
            if (!isCreative()) {
                final ItemStack itemStack = player.getMainHandStack();
                itemStack.postMine(world, microBlock.blockStatePropertyProxy(), pos, player);
            }
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "continueMining", at = @At("HEAD"), cancellable = true)
    private void continueMiningHook(final BlockState state, final BlockPos pos, final int failedStartMiningTime, final CallbackInfoReturnable<Float> cir) {
        if (breakingMicroBlock.isPresent()) {
            final long id = breakingMicroBlock.getAsLong();
            final MicroBlockHandleImpl handle = new MicroBlockHandleImpl(world.getRegistryKey(), id);
            final BlockPos canonicalPos = ((MicroBlockWorld) world).canonicalPos(handle);
            if (canonicalPos == null) {
                //TODO update;
                throw new RuntimeException();
            }
            final BlockEntity blockEntity = world.getBlockEntity(canonicalPos);
            if (!(blockEntity instanceof MicroBlockContainerBlockEntity container)) {
                //TODO update;
                throw new RuntimeException();
            }
            final MicroBlock<?> microBlock = container.getContainer().byId(id);
            if (microBlock == null) {
                //TODO update;
                throw new RuntimeException();
            }
            final MicroBlockContext context = ((MicroBlockWorld) world).createContext(handle);

            final int i = tickCounter - failedStartMiningTime;
            final float f = microBlock.calcBlockBreakingDelta(context, player) * (float) (i + 1);
            final int j = (int) (f * 10.0F);
            if (j != blockBreakingProgress) {
                ((MicroBlockWorldExtensions) world).microBlockBreakInfo(player.getId(), id, j);
                blockBreakingProgress = j;
            }
            cir.setReturnValue(f);
        }
    }

    @Inject(method = "update", at = @At(value = "FIELD", target = "mining:Z", opcode = Opcodes.PUTFIELD))
    private void updateReset(final CallbackInfo ci) {
        breakingMicroBlock = OptionalLong.empty();
    }

    @Inject(method = "processBlockBreakingAction", at = @At("RETURN"))
    private void processReset(final CallbackInfo ci) {
        if (!mining) {
            breakingMicroBlock = OptionalLong.empty();
        }
    }
}
