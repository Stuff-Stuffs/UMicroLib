package io.github.stuff_stuffs.u_micro_lib.internal.client.network;

import io.github.stuff_stuffs.u_micro_lib.internal.common.UMicroLibCommon;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class PlayerBreakSender {
    public static final Identifier CHANNEL = UMicroLibCommon.id("break_start_channel");

    public static void send(final BlockPos pos, final long id, final Direction direction) {
        final PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeEnumConstant(direction);
        buf.writeLong(id);
        ClientPlayNetworking.send(CHANNEL, buf);
    }

    private PlayerBreakSender() {
    }
}
