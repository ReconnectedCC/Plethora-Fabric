package io.sc3.plethora.api.reference;

import dan200.computercraft.api.lua.LuaException;

import javax.annotation.Nonnull;

/**
 * A reference to an object
 */
public interface IReference<T> {
    /**
     * Get the object if it still exists.
     *
     * Note, this method is NOT thread safe and MUST be called from the canvas thread. Use {@link #safeGet()} if
     * you need a safe version.
     *
     * @return The object if it still exists
     * @throws LuaException if the object doesn't exist
     */
    @Nonnull
    T get() throws LuaException;

    /**
     * Get the object if it still exists.
     *
     * This method MUST be thread safe, though the result object may not be safe to use on any thread. You should
     * always use {@link #get()} if calling from the canvas thread.
     *
     * @return The object if it still exists
     * @throws LuaException if the object doesn't exist
     */
    @Nonnull
    T safeGet() throws LuaException;

    /**
     * Whether this reference will always return the same object (or an equivalent one).
     *
     * @return If this reference is constant.
     * @see ConstantReference
     * @see DynamicReference
     */
    boolean isConstant();
}
