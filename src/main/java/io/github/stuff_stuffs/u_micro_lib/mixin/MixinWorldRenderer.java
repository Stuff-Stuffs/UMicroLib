package io.github.stuff_stuffs.u_micro_lib.mixin;

import io.github.stuff_stuffs.u_micro_lib.api.common.micro.MicroBlockWorld;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlock;
import io.github.stuff_stuffs.u_micro_lib.impl.micro.MicroBlockHandleImpl;
import io.github.stuff_stuffs.u_micro_lib.internal.client.UMicroLibClient;
import io.github.stuff_stuffs.u_micro_lib.internal.client.render.MicroBlockBreakInfo;
import io.github.stuff_stuffs.u_micro_lib.internal.client.render.WorldRendererExtensions;
import io.github.stuff_stuffs.u_micro_lib.internal.common.block.entity.MicroBlockContainerBlockEntity;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.SortedSet;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer implements WorldRendererExtensions {
    @Shadow
    private int ticks;
    @Shadow
    private @Nullable ClientWorld world;

    @Shadow
    public abstract void render(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f positionMatrix);

    @Shadow
    @Final
    private BufferBuilderStorage bufferBuilders;
    @Shadow
    @Final
    private MinecraftClient client;
    @Unique
    private final Int2ObjectMap<MicroBlockBreakInfo> microBlockBreakingInfos = new Int2ObjectOpenHashMap<>();
    @Unique
    private final Long2ObjectMap<SortedSet<MicroBlockBreakInfo>> microBlockBreakingProgressions = new Long2ObjectOpenHashMap<>();

    @Override
    public void setMicroBlockBreakInfo(final int entityId, final long id, final int stage) {
        if (stage >= 0 && stage < 10) {
            MicroBlockBreakInfo info = microBlockBreakingInfos.get(entityId);
            if (info != null) {
                removeMicroBreakInfo(info);
            }
            if (info == null || id != info.id()) {
                info = new MicroBlockBreakInfo(entityId, id);
                microBlockBreakingInfos.put(entityId, info);
            }
            info.setStage(stage);
            info.lastUpdateTick = ticks;
            microBlockBreakingProgressions.computeIfAbsent(id, i -> new ObjectAVLTreeSet<>()).add(info);
        } else {
            final MicroBlockBreakInfo info = microBlockBreakingInfos.remove(entityId);
            if (info != null) {
                removeMicroBreakInfo(info);
            }
        }
    }

    private void removeMicroBreakInfo(final MicroBlockBreakInfo info) {
        final SortedSet<MicroBlockBreakInfo> infos = microBlockBreakingProgressions.get(info.id());
        infos.remove(info);
        if (infos.isEmpty()) {
            microBlockBreakingProgressions.remove(info.id());
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void tickHook(final CallbackInfo ci) {
        final Iterator<MicroBlockBreakInfo> iterator = microBlockBreakingInfos.values().iterator();
        while (iterator.hasNext()) {
            final MicroBlockBreakInfo info = iterator.next();
            if (ticks - info.lastUpdateTick > 400) {
                iterator.remove();
                removeMicroBreakInfo(info);
            }
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = {"ldc=destroyProgress"}))
    private void destroyBlockProgressHook(final MatrixStack matrices, final float tickDelta, final long limitTime, final boolean renderBlockOutline, final Camera camera, final GameRenderer gameRenderer, final LightmapTextureManager lightmapTextureManager, final Matrix4f positionMatrix, final CallbackInfo ci) {
        final Vec3d cameraPos = camera.getPos();
        final Long2ObjectMap<LongSet> breakingByPosition = new Long2ObjectOpenHashMap<>();
        for (final Long2ObjectMap.Entry<SortedSet<MicroBlockBreakInfo>> entry : microBlockBreakingProgressions.long2ObjectEntrySet()) {
            final MicroBlockHandleImpl handle = new MicroBlockHandleImpl(world.getRegistryKey(), entry.getLongKey());
            final BlockPos blockPos = ((MicroBlockWorld) world).canonicalPos(handle);
            if (blockPos == null) {
                continue;
            }
            final BlockEntity blockEntity = world.getBlockEntity(blockPos);
            if (!(blockEntity instanceof MicroBlockContainerBlockEntity microContainer)) {
                continue;
            }
            final MicroBlock<?> microBlock = microContainer.getContainer().byId(entry.getLongKey());
            if (microBlock == null) {
                continue;
            }
            breakingByPosition.computeIfAbsent(blockPos.asLong(), i -> new LongArraySet()).add(entry.getLongKey());
        }
        for (final Long2ObjectMap.Entry<LongSet> entry : breakingByPosition.long2ObjectEntrySet()) {
            final BlockPos blockPos = BlockPos.fromLong(entry.getLongKey());
            final BlockEntity blockEntity = world.getBlockEntity(blockPos);
            if (!(blockEntity instanceof MicroBlockContainerBlockEntity microContainer)) {
                continue;
            }
            final double dx = blockEntity.getPos().getX() - cameraPos.x;
            final double dy = blockEntity.getPos().getY() - cameraPos.y;
            final double dz = blockEntity.getPos().getZ() - cameraPos.z;
            if (dx * dx + dy * dy + dz * dz < 1024.0) {
                final LongSet list = entry.getValue();
                matrices.push();
                matrices.translate(dx, dy, dz);
                final LongIterator iterator = list.iterator();
                while (iterator.hasNext()) {
                    final long id = iterator.nextLong();
                    final SortedSet<MicroBlockBreakInfo> infos = microBlockBreakingProgressions.get(id);
                    if (infos == null || infos.isEmpty()) {
                        continue;
                    }
                    final int stage = infos.last().stage();
                    final VertexConsumer crumblingLayer = new OverlayVertexConsumer(
                            bufferBuilders.getEffectVertexConsumers().getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(stage)),
                            matrices.peek().getPositionMatrix(),
                            matrices.peek().getNormalMatrix()
                    );
                    UMicroLibClient.DAMAGE_BLOCK = LongSet.of(id);
                    client.getBlockRenderManager().renderDamage(world.getBlockState(blockPos), blockPos, world, matrices, crumblingLayer);
                }
                UMicroLibClient.DAMAGE_BLOCK = LongSets.emptySet();
                matrices.pop();
            }
        }
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/block/entity/BlockEntityRenderDispatcher;render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V"), index = 3)
    private VertexConsumerProvider destroyBlockEntityProgressHook(final BlockEntity blockEntity, final float tickDelta, final MatrixStack matrices, final VertexConsumerProvider vertexConsumers) {
        if (blockEntity instanceof MicroBlockContainerBlockEntity container) {
            if (vertexConsumers instanceof VertexConsumerProvider.Immediate) {
                final Long2IntMap map = new Long2IntOpenHashMap();
                container.getContainer().canonPieces().forEach((LongConsumer) value -> {
                    final SortedSet<MicroBlockBreakInfo> infos = microBlockBreakingProgressions.get(value);
                    if (infos != null && !infos.isEmpty()) {
                        map.put(value, infos.last().stage());
                    }
                });
                UMicroLibClient.DAMAGE_ENTITY = map;
            }
        }
        return vertexConsumers;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/block/entity/BlockEntityRenderDispatcher;render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V", shift = At.Shift.AFTER))
    private void destroyBlockEntityProgressHookFinish(final MatrixStack matrices, final float tickDelta, final long limitTime, final boolean renderBlockOutline, final Camera camera, final GameRenderer gameRenderer, final LightmapTextureManager lightmapTextureManager, final Matrix4f positionMatrix, final CallbackInfo ci) {
        UMicroLibClient.DAMAGE_ENTITY = Long2IntMaps.EMPTY_MAP;
    }
}
