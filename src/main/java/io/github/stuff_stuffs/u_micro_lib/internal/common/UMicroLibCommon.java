package io.github.stuff_stuffs.u_micro_lib.internal.common;

import io.github.stuff_stuffs.u_micro_lib.api.common.micro.MicroBlockWorld;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.block.MicroBlockType;
import io.github.stuff_stuffs.u_micro_lib.api.common.micro.item.MicroBlockPlacementContext;
import io.github.stuff_stuffs.u_micro_lib.api.common.util.MicroBlockPos;
import io.github.stuff_stuffs.u_micro_lib.internal.client.network.PlayerBreakSender;
import io.github.stuff_stuffs.u_micro_lib.internal.common.block.UMicroLibBlocks;
import io.github.stuff_stuffs.u_micro_lib.internal.common.micro.ServerPlayerInteractionManagerExtensions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

public class UMicroLibCommon implements ModInitializer {
    public static BiConsumer<World, BlockPos> MODEL_UPDATER = (world, pos) -> {
    };
    public static final String MOD_ID = "u_micro_lib";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Item TEST_ITEM = new Item(new FabricItemSettings()) {
        @Override
        public ActionResult useOnBlock(final ItemUsageContext context) {
            final World world = context.getWorld();
            if (!world.isClient) {
                final MicroBlockPlacementContext placementContext = ((MicroBlockWorld) world).createPlacementContext(context.getPlayer(), context.getHitPos(), context.getSide(), context.getStack());
                final Test piece = new Test(Test.placementPosition(placementContext));
                placementContext.tryPlace(piece);
            }
            return ActionResult.SUCCESS;
        }
    };
    public static final MicroBlockType<Test, Object> PIECE_TYPE = MicroBlockType.create(p -> new Test(new MicroBlockPos(p)));

    @Override
    public void onInitialize() {
        Registry.register(Registry.ITEM, id("test"), TEST_ITEM);
        Registry.register(MicroBlockType.REGISTRY, id("test"), PIECE_TYPE);
        UMicroLibBlocks.init();
        ServerPlayNetworking.registerGlobalReceiver(PlayerBreakSender.CHANNEL, (server, player, handler, buf, responseSender) -> {
            final BlockPos pos = buf.readBlockPos();
            final Direction direction = buf.readEnumConstant(Direction.class);
            final long id = buf.readLong();
            server.execute(() -> ((ServerPlayerInteractionManagerExtensions) player.interactionManager).startBreaking(id, direction, pos));
        });
    }

    public static Identifier id(final String path) {
        return new Identifier(MOD_ID, path);
    }
}
