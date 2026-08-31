package org.cloudburstmc.protocol.bedrock.packet;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.common.PacketSignal;

import java.util.Set;

@Data
@EqualsAndHashCode(doNotUseGetters = true)
@ToString(doNotUseGetters = true)
/*
 * ClientMovementPredictionSyncPacket
 */
public class MovementPredictionSyncPacket implements BedrockPacket {
    public long runtimeEntityId;

    public final Set<EntityFlag> flags = new ObjectOpenHashSet<>();
    public Vector3f boundingBox;

    public float speed;
    public float underwaterSpeed;
    public float lavaSpeed;
    public float jumpStrength;
    public float health;
    public float hunger;
    /**
     * @since v975
     */
    @Deprecated
    public float unknown1;
    /**
     * @since v975
     */
    @Deprecated
    public float unknown2;
    /**
     * @since v975
     */
    @Deprecated
    public float unknown3;
    public boolean flying;


    @Override
    public final PacketSignal handle(BedrockPacketHandler handler) {
        return handler.handle(this);
    }

    public BedrockPacketType getPacketType() {
        return BedrockPacketType.MOVEMENT_PREDICTION_SYNC;
    }

    @Override
    public MovementPredictionSyncPacket clone() {
        try {
            return (MovementPredictionSyncPacket) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}

