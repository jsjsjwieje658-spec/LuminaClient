package org.cloudburstmc.protocol.bedrock.packet;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.ContainerMixData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.MaterialReducer;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.PotionMixData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.*;
import org.cloudburstmc.protocol.common.PacketSignal;

import java.util.List;

@Data
@ToString(doNotUseGetters = true)
@EqualsAndHashCode(doNotUseGetters = true)
public class CraftingDataPacket implements BedrockPacket {
    /**
     * @deprecated since v2168
     */
    public final List<RecipeData> craftingData = new ObjectArrayList<>();
    /**
     * @since v2168
     */
    public final List<ShapedRecipeData> shapedData = new ObjectArrayList<>();
    /**
     * @since v2168
     */
    public final List<ShapelessRecipeData> shapelessData = new ObjectArrayList<>();
    /**
     * @since v2168
     */
    public final List<MultiRecipeData> multiData = new ObjectArrayList<>();
    /**
     * @since v2168
     */
    public final List<ShapelessRecipeData> shapelessUserData = new ObjectArrayList<>();
    /**
     * @since v2168
     */
    public final List<ShapelessRecipeData> shapelessChemistryData = new ObjectArrayList<>();
    /**
     * @since v2168
     */
    public final List<ShapedRecipeData> shapedChemistryData = new ObjectArrayList<>();
    /**
     * @since v2168
     */
    public final List<SmithingTransformRecipeData> smithingTransformData = new ObjectArrayList<>();
    /**
     * @since v2168
     */
    public final List<SmithingTrimRecipeData> smithingTrimData = new ObjectArrayList<>();
    public final List<PotionMixData> potionMixData = new ObjectArrayList<>();
    public final List<ContainerMixData> containerMixData = new ObjectArrayList<>();
    /**
     * @since v465
     */
    public final List<MaterialReducer> materialReducers = new ObjectArrayList<>();
    public boolean cleanRecipes;

    @Override
    public final PacketSignal handle(BedrockPacketHandler handler) {
        return handler.handle(this);
    }

    public BedrockPacketType getPacketType() {
        return BedrockPacketType.CRAFTING_DATA;
    }

    @Override
    public CraftingDataPacket clone() {
        try {
            return (CraftingDataPacket) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}

