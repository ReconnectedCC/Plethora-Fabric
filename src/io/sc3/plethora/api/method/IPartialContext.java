package io.sc3.plethora.api.method;

import net.minecraft.util.Identifier;
import io.sc3.plethora.api.meta.TypedMeta;
import io.sc3.plethora.api.module.IModuleContainer;

import javax.annotation.Nonnull;

/**
 * A context for a method call
 */
public interface IPartialContext<T> {
    /**
     * Get the target for this context.
     *
     * @return The target for this context.
     */
    @Nonnull
    T getTarget();

    /**
     * Get surrounding context for an object.
     * This is generally "parent" objects: an inventory might have a tile entity in its context.
     * This does not include the target.
     *
     * @param klass The type of the klass to get.
     * @return The context object or {@code null} if it doesn't exist.
     * @see #hasContext(Class)
     */
    <V> V getContext(@Nonnull Class<V> klass);

    /**
     * Check if a context exists.
     * This is generally "parent" objects: an inventory might have a tile entity in its context.
     * This does not include the target.
     *
     * @param klass The type of the klass to get.
     * @return If this context exists. It is more performant to check if {@link #getContext(Class)} returns null.
     * @see #getContext(Class)
     */
    <V> boolean hasContext(@Nonnull Class<V> klass);

    /**
     * Get surrounding context for an object.
     * This is generally "parent" objects: an inventory might have a tile entity in its context.
     * This does not include the target.
     *
     * @param contextKey A particular context key to look under.
     * @param klass      The type of the klass to get.
     * @return The context object or {@code null} if it doesn't exist.
     * @see #hasContext(Class)
     */
    <V> V getContext(@Nonnull String contextKey, @Nonnull Class<V> klass);

    /**
     * Get surrounding context for an object.
     * This is generally "parent" objects: an inventory might have a tile entity in its context.
     * This does not include the target.
     *
     * @param contextKey A particular context key to look under.
     * @param klass      The type of the klass to get.
     * @return The context object or {@code null} if it doesn't exist.
     * @see #hasContext(Class)
     */
    default <V> V getContext(@Nonnull Identifier contextKey, @Nonnull Class<V> klass) {
        return getContext(contextKey.toString(), klass);
    }

    /**
     * Check if a context exists.
     * This is generally "parent" objects: an inventory might have a tile entity in its context.
     * This does not include the target.
     *
     * @param contextKey A particular context key to look under.
     * @param klass      The type of the klass to get.
     * @return If this context exists. It is more performant to check if {@link #getContext(Class)} returns null.
     * @see #getContext(Class)
     */
    <V> boolean hasContext(@Nonnull String contextKey, @Nonnull Class<V> klass);

    /**
     * Check if a context exists.
     * This is generally "parent" objects: an inventory might have a tile entity in its context.
     * This does not include the target.
     *
     * @param contextKey A particular context key to look under.
     * @param klass      The type of the klass to get.
     * @return If this context exists. It is more performant to check if {@link #getContext(Class)} returns null.
     * @see #getContext(Class)
     */
    default <V> boolean hasContext(@Nonnull Identifier contextKey, @Nonnull Class<V> klass) {
        return hasContext(contextKey.toString(), klass);
    }

    /**
     * Make a child context
     *
     * @param target The child's target
     * @return The child context
     */
    @Nonnull
    <U> IPartialContext<U> makePartialChild(@Nonnull U target);

    /**
     * Get the cost handler associated with this object.
     *
     * @return The cost handler
     */
    @Nonnull
    ICostHandler getCostHandler();

    /**
     * Get a collection of all modules in this context.
     *
     * This will not change over the course of the context's lifetime.
     *
     * @return A read-only set of all available modules.
     */
    @Nonnull
    IModuleContainer getModules();

    /**
     * Get the metadata for this object
     *
     * @return The object's metadata
     * @see io.sc3.plethora.api.meta.IMetaProvider
     */
    @Nonnull
    TypedMeta<T, ?> getMeta();
}
