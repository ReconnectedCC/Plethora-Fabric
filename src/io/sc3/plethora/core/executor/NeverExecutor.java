package io.sc3.plethora.core.executor;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import io.sc3.plethora.api.method.FutureMethodResult;
import io.sc3.plethora.api.method.IResultExecutor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A result executor which should never be used.
 *
 * This is intended for cases where you will replace the executor before evaluating it (such as on peripherals).
 */
public final class NeverExecutor implements IResultExecutor {
	public static final IResultExecutor INSTANCE = new NeverExecutor();

	private NeverExecutor() {
	}

	@Nullable
	@Override
	public MethodResult execute(@Nonnull FutureMethodResult result, @Nonnull ILuaContext context) throws LuaException {
		throw new LuaException("Cannot execute method");
	}

	@Override
	public void executeAsync(@Nonnull FutureMethodResult result) throws LuaException {
		throw new LuaException("Cannot execute method");
	}
}
