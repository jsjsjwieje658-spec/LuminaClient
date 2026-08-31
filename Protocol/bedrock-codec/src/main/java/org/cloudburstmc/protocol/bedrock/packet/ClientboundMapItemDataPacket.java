package org.cloudburstmc.protocol.bedrock.packet;

import it.unimi.dsi.fastutil.longs.LongList;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.MapDecoration;
import org.cloudburstmc.protocol.bedrock.data.MapTrackedObject;
import org.cloudburstmc.protocol.common.PacketSignal;

import java.util.List;

@Data
@EqualsAndHashCode(doNotUseGetters = true)
@ToString(doNotUseGetters = true)
public class ClientboundMapItemDataPacket implements BedrockPacket {
    @Nullable
    public LongList trackedEntityIds;
    @Nullable
    public List<MapTrackedObject> trackedObjects;
    @Nullable
    public List<MapDecoration> decorations;
    public long uniqueMapId;
    public int dimensionId;
    public boolean locked;
    /**
     * The world-relative position of the map's origin.
     *
     * @since 1.19.20
     */
    public Vector3i origin;
    @Nullable
    public Byte scale;
    @Nullable
    public Integer height;
    @Nullable
    public Integer width;
    @Nullable
    public Integer xOffset;
    @Nullable
    public Integer yOffset;
    public int[] colors;

    @Override
    public final PacketSignal handle(BedrockPacketHandler handler) {
        return handler.handle(this);
    }

    public BedrockPacketType getPacketType() {
        return BedrockPacketType.CLIENTBOUND_MAP_ITEM_DATA;
    }

    @Override
    public ClientboundMapItemDataPacket clone() {
        try {
            return (ClientboundMapItemDataPacket) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}

