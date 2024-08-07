package io.sc3.plethora.core;

import dan200.computercraft.api.lua.LuaException;
import io.sc3.plethora.api.method.ICostHandler;
import io.sc3.plethora.api.method.IResultExecutor;
import io.sc3.plethora.api.method.IUnbakedContext;
import io.sc3.plethora.api.module.IModuleContainer;
import io.sc3.plethora.api.reference.IReference;

import javax.annotation.Nonnull;

/**
 * A context which doesn't have solidified references.
 */
public final class UnbakedContext<T> implements IUnbakedContext<T> {
	final int target;
	final String[] keys;
	final Object[] references;

	final ICostHandler handler;
	final IReference<IModuleContainer> modules;
	final IResultExecutor executor;

	UnbakedContext(int target, String[] keys, Object[] references, ICostHandler handler, IReference<IModuleContainer> modules, IResultExecutor executor) {
		this.target = target;
		this.handler = handler;
		this.keys = keys;
		this.references = references;
		this.modules = modules;
		this.executor = executor;
	}

	UnbakedContext<?> withIndex(int index) {
		return index == target ? this : new UnbakedContext<>(index, keys, references, handler, modules, executor);
	}

	@Nonnull
	@Override
	public Context<T> bake() throws LuaException {
		Object[] values = new Object[references.length];
		for (int i = 0; i < references.length; i++) {
			Object reference = references[i];
			if (reference instanceof IReference) {
				values[i] = ((IReference<?>) reference).get();
			} else if (reference instanceof ConverterReference) {
				values[i] = ((ConverterReference<?>) reference).tryConvert(values);
			} else {
				values[i] = reference;
			}
		}

		return new Context<>(this, values, modules.get());
	}

	@Nonnull
	@Override
	public Context<T> safeBake() throws LuaException {
		Object[] values = new Object[references.length];
		for (int i = 0; i < references.length; i++) {
			Object reference = references[i];
			if (reference instanceof IReference) {
				values[i] = ((IReference<?>) reference).safeGet();
			} else if (reference instanceof ConverterReference) {
				values[i] = null;
			} else {
				values[i] = reference;
			}
		}

		return new Context<>(this, values, modules.safeGet());
	}

	@Nonnull
	@Override
	public ICostHandler getCostHandler() {
		return handler;
	}

	@Nonnull
	@Override
	public IResultExecutor getExecutor() {
		return executor;
	}
}
