package io.github.stuff_stuffs.u_micro_lib.internal.client;

import io.github.stuff_stuffs.u_micro_lib.api.client.render.MicroBlockModel;
import io.github.stuff_stuffs.u_micro_lib.api.client.render.MicroBlockRenderer;
import io.github.stuff_stuffs.u_micro_lib.api.client.render.MicroBlockRendererRegistry;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlock;
import io.github.stuff_stuffs.u_micro_lib.impl.render.MicroBlockRendererRegistryImpl;
import io.github.stuff_stuffs.u_micro_lib.internal.client.render.model.MicroBlockContainerUnbakedModel;
import io.github.stuff_stuffs.u_micro_lib.internal.common.UMicroLibCommon;
import io.github.stuff_stuffs.u_micro_lib.internal.common.block.UMicroLibBlocks;
import io.github.stuff_stuffs.u_micro_lib.internal.common.block.entity.MicroBlockContainerBlockEntity;
import io.github.stuff_stuffs.u_micro_lib.internal.common.block.entity.UMicroLibBlockEntities;
import io.github.stuff_stuffs.u_micro_lib.internal.common.micro.MicroBlockWorldExtensions;
import io.github.stuff_stuffs.u_micro_lib.internal.common.network.MicroBlockBreakSender;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.util.Optional;
import java.util.PrimitiveIterator;

public class UMicroLibClient implements ClientModInitializer {
    public static LongSet DAMAGE_BLOCK = LongSets.emptySet();
    public static Long2IntMap DAMAGE_ENTITY = Long2IntMaps.EMPTY_MAP;

    @Override
    public void onInitializeClient() {
        UMicroLibCommon.MODEL_UPDATER = (world, pos) -> {
            if (world instanceof ClientWorld clientWorld) {
                clientWorld.scheduleBlockRenders(pos.getX(), pos.getY(), pos.getZ());
            }
        };
        BlockEntityRendererRegistry.register(UMicroLibBlockEntities.MULTI_PART_BLOCK_ENTITY_TYPE, ctx -> new BlockEntityRenderer<>() {
            @Override
            public void render(final MicroBlockContainerBlockEntity entity, final float tickDelta, final MatrixStack matrices, final VertexConsumerProvider vertexConsumers, final int light, final int overlay) {
                final PrimitiveIterator.OfLong iterator = entity.getContainer().canonPieces().iterator();
                while (iterator.hasNext()) {
                    final long id = iterator.nextLong();
                    if (DAMAGE_ENTITY.containsKey(id)) {
                        final int stage = MathHelper.clamp(DAMAGE_ENTITY.get(id), 0, 9);
                        final MatrixStack.Entry entry = matrices.peek();
                        final VertexConsumer vertexConsumer = new OverlayVertexConsumer(
                                MinecraftClient.getInstance().getBufferBuilders().getEffectVertexConsumers().getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(stage)),
                                entry.getPositionMatrix(),
                                entry.getNormalMatrix()
                        );
                        final VertexConsumerProvider vertexConsumersDamaged = renderLayer -> {
                            final VertexConsumer vertexConsumer2x = vertexConsumers.getBuffer(renderLayer);
                            return renderLayer.hasCrumbling() ? VertexConsumers.union(vertexConsumer, vertexConsumer2x) : vertexConsumer2x;
                        };
                        render(entity.getContainer().byId(id), tickDelta, matrices, vertexConsumersDamaged, light, overlay, entity.getPos());
                    } else {
                        render(entity.getContainer().byId(id), tickDelta, matrices, vertexConsumers, light, overlay, entity.getPos());
                    }
                }
            }

            private <ClientData> void render(final MicroBlock<ClientData> microBlock, final float tickDelta, final MatrixStack matrices, final VertexConsumerProvider vertexConsumers, final int light, final int overlay, final BlockPos pos) {
                final MicroBlockRenderer<ClientData> renderer = MicroBlockRendererRegistryImpl.INSTANCE.renderer(microBlock.type());
                matrices.push();
                final Vec3d d = microBlock.pos();
                matrices.translate(d.x - pos.getX(), d.y - pos.getY(), d.z - pos.getZ());
                renderer.renderDynamic(microBlock.clientData(), tickDelta, matrices, vertexConsumers, light, overlay);
                matrices.pop();
            }

            @Override
            public boolean rendersOutsideBoundingBox(final MicroBlockContainerBlockEntity blockEntity) {
                return true;
            }

            @Override
            public int getRenderDistance() {
                return 1024;
            }
        });
        MicroBlockRendererRegistry.get().register(UMicroLibCommon.PIECE_TYPE, new MicroBlockRenderer<>() {
            @Override
            public boolean hasBakedModel() {
                return false;
            }

            @Override
            public MicroBlockModel<Object> bakedModel(final Object o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void renderDynamic(final Object o, final float tickDelta, final MatrixStack matrices, final VertexConsumerProvider vertexConsumers, final int light, final int overlay) {
                matrices.scale(14 / 16.0F, 14 / 16.0F, 14 / 16.0F);
                MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(Blocks.COBBLESTONE.getDefaultState(), matrices, vertexConsumers, light, overlay);
            }
        });
        WorldRenderEvents.BLOCK_OUTLINE.register((worldRenderContext, blockOutlineContext) -> {
            if (blockOutlineContext.blockState().isOf(UMicroLibBlocks.MULTI_PART_BLOCK)) {
                final BlockPos pos = blockOutlineContext.blockPos();
                final ClientWorld world = worldRenderContext.world();
                final BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof MicroBlockContainerBlockEntity microBlock) {
                    final Optional<VoxelShape> shape = microBlock.getContainer().rayCastShape(blockOutlineContext.cameraX(), blockOutlineContext.cameraY(), blockOutlineContext.cameraZ(), blockOutlineContext.entity().getRotationVec(worldRenderContext.tickDelta()), MinecraftClient.getInstance().interactionManager.getReachDistance());
                    if (shape.isPresent()) {
                        final VoxelShape voxelShape = shape.get();
                        final MatrixStack matrices = worldRenderContext.matrixStack();
                        matrices.push();
                        final Vec3d cameraPos = worldRenderContext.camera().getPos();
                        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
                        final MatrixStack.Entry peek = matrices.peek();
                        final VertexConsumer consumer = worldRenderContext.consumers().getBuffer(RenderLayer.getLines());
                        voxelShape.forEachEdge((minX, minY, minZ, maxX, maxY, maxZ) -> {
                            float k = (float) (maxX - minX);
                            float l = (float) (maxY - minY);
                            float m = (float) (maxZ - minZ);
                            final float n = MathHelper.sqrt(k * k + l * l + m * m);
                            k /= n;
                            l /= n;
                            m /= n;
                            consumer.vertex(peek.getPositionMatrix(), (float) (minX), (float) (minY), (float) (minZ)).color(0, 0, 0, 0.4F).normal(peek.getNormalMatrix(), k, l, m).next();
                            consumer.vertex(peek.getPositionMatrix(), (float) (maxX), (float) (maxY), (float) (maxZ)).color(0, 0, 0, 0.4F).normal(peek.getNormalMatrix(), k, l, m).next();
                        });
                        matrices.pop();
                        return false;
                    }
                }
            }
            return true;
        });
        ModelLoadingRegistry.INSTANCE.registerResourceProvider(manager -> (resourceId, context) -> {
            if ("u_micro_lib".equals(resourceId.getNamespace())) {
                if ("block/micro_container".equals(resourceId.getPath())) {
                    return new MicroBlockContainerUnbakedModel();
                }
            }
            return null;
        });
        ClientPlayNetworking.registerGlobalReceiver(MicroBlockBreakSender.CHANNEL, (client, handler, buf, responseSender) -> {
            final int entityId = buf.readInt();
            final long id = buf.readLong();
            final int stage = buf.readVarInt();
            if (entityId != MinecraftClient.getInstance().player.getId()) {
                client.execute(() -> ((MicroBlockWorldExtensions) client.world).microBlockBreakInfo(entityId, id, stage));
            }
        });
    }
}
