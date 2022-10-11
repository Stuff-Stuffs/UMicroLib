package io.github.stuff_stuffs.u_micro_lib.mixin;

import io.github.stuff_stuffs.u_micro_lib.api.common.micro.MicroBlockWorld;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlock;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlockContext;
import io.github.stuff_stuffs.u_micro_lib.impl.micro.MicroBlockHandleImpl;
import io.github.stuff_stuffs.u_micro_lib.internal.client.network.PlayerBreakSender;
import io.github.stuff_stuffs.u_micro_lib.internal.common.block.UMicroLibBlocks;
import io.github.stuff_stuffs.u_micro_lib.internal.common.block.entity.MicroBlockContainerBlockEntity;
import io.github.stuff_stuffs.u_micro_lib.internal.common.micro.MicroBlockWorldExtensions;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalLong;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class MixinClientPlayerInteractionManager {
    @Shadow
    private GameMode gameMode;

    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    private int blockBreakingCooldown;

    @Shadow
    public abstract float getReachDistance();

    @Shadow
    private float currentBreakingProgress;
    @Shadow
    private boolean breakingBlock;
    @Shadow
    private BlockPos currentBreakingPos;
    @Shadow
    private ItemStack selectedStack;
    @Shadow
    private float blockBreakingSoundCooldown;
    @Shadow
    @Final
    private ClientPlayNetworkHandler networkHandler;

    @Shadow
    protected abstract boolean isCurrentlyBreaking(BlockPos pos);

    @Shadow
    protected abstract void sendSequencedPacket(ClientWorld world, SequencedPacketCreator packetCreator);

    @Shadow
    public abstract boolean updateBlockBreakingProgress(BlockPos pos, Direction direction);

    @Unique
    private OptionalLong breakingMicroBlock = OptionalLong.empty();

    @Inject(method = "attackBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/tutorial/TutorialManager;onBlockBreaking(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;F)V"), cancellable = true)
    private void attackHook(final BlockPos pos, final Direction direction, final CallbackInfoReturnable<Boolean> cir) {
        final BlockState blockState = client.world.getBlockState(pos);
        if (!blockState.isOf(UMicroLibBlocks.MULTI_PART_BLOCK)) {
            if (breakingMicroBlock.isPresent()) {
                breakingMicroBlock = OptionalLong.empty();
                networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, currentBreakingPos, direction));
            }
            return;
        }
        final BlockEntity blockEntity = client.world.getBlockEntity(pos);
        if (!(blockEntity instanceof MicroBlockContainerBlockEntity microContainer)) {
            if (breakingMicroBlock.isPresent()) {
                breakingMicroBlock = OptionalLong.empty();
                networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, currentBreakingPos, direction));
            }
            return;
        }
        final Vec3d vec = client.gameRenderer.getCamera().getPos();
        final Vec3d dir = client.player.getRotationVector();
        final OptionalLong optId = microContainer.getContainer().rayCastId(vec.x, vec.y, vec.z, dir, getReachDistance());

        if (optId.isEmpty() || optId.getAsLong() == Long.MIN_VALUE) {
            if (breakingMicroBlock.isPresent()) {
                breakingMicroBlock = OptionalLong.empty();
                networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, currentBreakingPos, direction));
            }
            return;
        }
        final long id = optId.getAsLong();
        if (gameMode.isCreative()) {
            final MicroBlockHandleImpl handle = new MicroBlockHandleImpl(client.world.getRegistryKey(), id);
            final MicroBlockContext context = ((MicroBlockWorld) client.world).createContext(handle);
            blockBreakingCooldown = 5;
            breakMicroBlock(id, context);
        } else {
            if (breakingBlock && breakingMicroBlock.isPresent() && breakingMicroBlock.getAsLong() != id) {
                networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, currentBreakingPos, direction));
            }
            final MicroBlockHandleImpl handle = new MicroBlockHandleImpl(client.world.getRegistryKey(), id);
            final BlockPos canonicalPos = ((MicroBlockWorld) client.world).canonicalPos(handle);
            final BlockEntity canonEntity = client.world.getBlockEntity(canonicalPos);
            if (!(canonEntity instanceof MicroBlockContainerBlockEntity canonMicroContainer)) {
                if (breakingMicroBlock.isPresent()) {
                    breakingMicroBlock = OptionalLong.empty();
                    networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, currentBreakingPos, direction));
                }
                return;
            }
            final MicroBlock<?> microBlock = canonMicroContainer.getContainer().byId(id);
            if (microBlock == null) {
                if (breakingMicroBlock.isPresent()) {
                    breakingMicroBlock = OptionalLong.empty();
                    networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, currentBreakingPos, direction));
                }
                return;
            }
            final MicroBlockContext context = ((MicroBlockWorld) client.world).createContext(handle);
            if (currentBreakingProgress == 0.0F) {
                microBlock.onBreakStart(context, direction, client.player);
            }
            if (microBlock.calcBlockBreakingDelta(context, client.player) >= 1.0F) {
                breakMicroBlock(id, context);
            } else {
                breakingBlock = true;
                currentBreakingPos = pos;
                selectedStack = client.player.getMainHandStack();
                currentBreakingProgress = 0.0F;
                blockBreakingSoundCooldown = 0.0F;
                breakingMicroBlock = OptionalLong.of(id);
                ((MicroBlockWorldExtensions) client.world).microBlockBreakInfo(client.player.getId(), id, (int) (currentBreakingProgress * 10.0F) - 1);
            }
        }
        PlayerBreakSender.send(pos, id, direction);
        cir.setReturnValue(true);
    }

    private void breakMicroBlock(final long id, final MicroBlockContext context) {
        final MicroBlockWorldExtensions extensions = (MicroBlockWorldExtensions) client.world;
        final BlockPos blockPos = extensions.canonicalPos(new MicroBlockHandleImpl(client.world.getRegistryKey(), id));
        final BlockEntity blockEntity = client.world.getBlockEntity(blockPos);
        if (!(blockEntity instanceof MicroBlockContainerBlockEntity microContainer)) {
            return;
        }
        final MicroBlock<?> microBlock = microContainer.getContainer().byId(id);
        if (microBlock == null) {
            return;
        }
        extensions.breakPiece(id, microBlock);
        microBlock.onBreak(context, client.player);
    }

    @Inject(method = "updateBlockBreakingProgress", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/GameMode;isCreative()Z"), cancellable = true)
    private void hook(final BlockPos pos, final Direction direction, final CallbackInfoReturnable<Boolean> cir) {
        if (breakingMicroBlock.isEmpty()) {
            return;
        }
        if (gameMode.isCreative() && client.world.getWorldBorder().contains(pos)) {
            final long id = breakingMicroBlock.getAsLong();
            final MicroBlockHandleImpl handle = new MicroBlockHandleImpl(client.world.getRegistryKey(), id);
            final MicroBlockContext context = ((MicroBlockWorld) client.world).createContext(handle);
            blockBreakingCooldown = 5;
            breakMicroBlock(id, context);
            cir.setReturnValue(true);
        } else if (isCurrentlyBreaking(pos)) {
            final long id = breakingMicroBlock.getAsLong();
            final MicroBlockHandleImpl handle = new MicroBlockHandleImpl(client.world.getRegistryKey(), id);
            final BlockEntity blockEntity = client.world.getBlockEntity(((MicroBlockWorld) client.world).canonicalPos(handle));
            if (!(blockEntity instanceof MicroBlockContainerBlockEntity microBlockContainer)) {
                breakingMicroBlock = OptionalLong.empty();
                return;
            }
            final MicroBlock<?> microBlock = microBlockContainer.getContainer().byId(id);
            final MicroBlockContext context = ((MicroBlockWorld) client.world).createContext(handle);
            final Vec3d vec = client.gameRenderer.getCamera().getPos();
            final Vec3d dir = client.player.getRotationVector();
            final OptionalLong optId = microBlockContainer.getContainer().rayCastId(vec.x, vec.y, vec.z, dir, getReachDistance());
            if (!optId.equals(breakingMicroBlock)) {
                breakingMicroBlock = optId;
                currentBreakingProgress = 0;
                cir.setReturnValue(updateBlockBreakingProgress(pos, direction));
                return;
            }
            currentBreakingProgress += microBlock.calcBlockBreakingDelta(context, client.player);
            if (blockBreakingSoundCooldown % 4.0F == 0.0F) {
                final BlockSoundGroup blockSoundGroup = microBlock.blockSoundGroup();
                client.getSoundManager().play(new PositionedSoundInstance(blockSoundGroup.getHitSound(), SoundCategory.BLOCKS, (blockSoundGroup.getVolume() + 1.0F) / 8.0F, blockSoundGroup.getPitch() * 0.5F, SoundInstance.createRandom(), pos));
            }

            ++blockBreakingSoundCooldown;
            if (currentBreakingProgress >= 1.0F) {
                breakingBlock = false;
                sendSequencedPacket(client.world, (sequence) -> {
                    breakMicroBlock(id, context);
                    return new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, direction, sequence);
                });
                currentBreakingProgress = 0.0F;
                blockBreakingSoundCooldown = 0.0F;
                blockBreakingCooldown = 5;
                breakingMicroBlock = OptionalLong.empty();
            }
            ((MicroBlockWorldExtensions) client.world).microBlockBreakInfo(client.player.getId(), id, (int) (currentBreakingProgress * 10.0F) - 1);
            cir.setReturnValue(true);
        }
    }
}
