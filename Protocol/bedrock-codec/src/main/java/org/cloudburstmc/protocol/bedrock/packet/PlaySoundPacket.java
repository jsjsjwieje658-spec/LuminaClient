package org.cloudburstmc.protocol.bedrock.packet;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.common.PacketSignal;

@Data
@EqualsAndHashCode(doNotUseGetters = true)
@ToString(doNotUseGetters = true)
public class PlaySoundPacket implements BedrockPacket {
    public String sound;
    public Vector3f position;
    public float volume;
    public float pitch;
    /**
     * @since v2168
     */
    public int loopCount;
    /**
     * @since v975
     */
    @Nullable
    public Long serverSoundHandle;
    /**
     * @since v2192
     */
    public boolean bypassListenerRangeCheck;
    /**
     * @since v2192
     */
    @Nullable
    public Float playbackPositionSeconds;

    @Override
    public final PacketSignal handle(BedrockPacketHandler handler) {
        return handler.handle(this);
    }

    public BedrockPacketType getPacketType() {
        return BedrockPacketType.PLAY_SOUND;
    }

    @Override
    public PlaySoundPacket clone() {
        try {
            return (PlaySoundPacket) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}

