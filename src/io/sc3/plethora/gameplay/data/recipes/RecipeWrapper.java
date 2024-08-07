package io.sc3.plethora.gameplay.data.recipes;

import com.google.gson.JsonObject;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * @see "dan200.computercraft.data.RecipeWrapper"
 */
public class RecipeWrapper implements RecipeJsonProvider {
    private final RecipeJsonProvider recipe;
    private final NbtCompound resultData;
    private final RecipeSerializer<?> serializer;

    private RecipeWrapper(RecipeJsonProvider recipe, NbtCompound resultData, RecipeSerializer<?> serializer) {
        this.resultData = resultData;
        this.recipe = recipe;
        this.serializer = serializer;
    }

    public static Consumer<RecipeJsonProvider> wrap(RecipeSerializer<?> serializer, Consumer<RecipeJsonProvider> original, NbtCompound resultData) {
        return x -> original.accept(new RecipeWrapper(x, resultData, serializer));
    }

    @Override
    public void serialize(@Nonnull JsonObject jsonObject) {
        recipe.serialize(jsonObject);

        if (resultData != null) {
            JsonObject object = JsonHelper.getObject(jsonObject, "result");
            object.addProperty("nbt", resultData.toString());
        }
    }

    @Nonnull
    @Override
    public Identifier getRecipeId() {
        return recipe.getRecipeId();
    }

    @Nonnull
    @Override
    public RecipeSerializer<?> getSerializer() {
        return serializer;
    }

    @Nullable
    @Override
    public JsonObject toAdvancementJson() {
        return recipe.toAdvancementJson();
    }

    @Nullable
    @Override
    public Identifier getAdvancementId() {
        return recipe.getAdvancementId();
    }
}
