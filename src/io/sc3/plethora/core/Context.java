package io.sc3.plethora.core;

import io.sc3.plethora.api.method.ContextKeys;
import io.sc3.plethora.api.method.IContext;
import io.sc3.plethora.api.method.TypedLuaObject;
import io.sc3.plethora.api.module.IModuleContainer;
import io.sc3.plethora.api.reference.IReference;
import io.sc3.plethora.api.reference.Reference;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public final class Context<T> extends PartialContext<T> implements IContext<T> {
	private final UnbakedContext<T> parent;

	Context(@Nonnull UnbakedContext<T> parent, @Nonnull Object[] context, @Nonnull IModuleContainer modules) {
		super(parent.target, parent.keys, context, parent.handler, modules);
		this.parent = parent;
	}

	@Override
	@SuppressWarnings("unchecked")
	Context<?> withIndex(int index) {
		return index == target ? this : new Context(parent.withIndex(index), values, modules);
	}

	@Nonnull
	@Override
	public <U> Context<U> makeChild(U target, @Nonnull IReference<U> targetReference) {
		Objects.requireNonNull(target, "target cannot be null");
		Objects.requireNonNull(targetReference, "targetReference cannot be null");

		ArrayList<String> keys = new ArrayList<>(this.keys.length + 1);
		ArrayList<Object> references = new ArrayList<>(parent.references.length + 1);
		ArrayList<Object> values = new ArrayList<>(this.values.length + 1);

		Collections.addAll(keys, this.keys);
		Collections.addAll(references, parent.references);
		Collections.addAll(values, this.values);

		for (int i = keys.size() - 1; i >= 0; i--) {
			if (!ContextKeys.TARGET.equals(keys.get(i))) continue;
			keys.set(i, ContextKeys.GENERIC);
		}

		// Add the new target and convert it.
		keys.add(ContextKeys.TARGET);
		references.add(targetReference);
		values.add(target);
		ConverterRegistry.instance.extendConverted(keys, values, references, this.values.length);

		return new Context<>(
			new UnbakedContext<>(this.keys.length, keys.toArray(new String[0]), references.toArray(),
				handler, parent.modules, parent.executor),
			values.toArray(), modules
		);
	}

	@Nonnull
	@Override
	public <U extends IReference<U>> Context<U> makeChild(@Nonnull U target) {
		return makeChild(target, target);
	}

	@Nonnull
	@Override
	public <U> Context<U> makeChildId(@Nonnull U target) {
		return makeChild(target, Reference.id(target));
	}

	@Nonnull
	@Override
	public UnbakedContext<T> unbake() {
		return parent;
	}

	@Nonnull
	@Override
	public TypedLuaObject<T> getObject() {
		return new MethodWrapperLuaObject<>(MethodRegistry.instance.getMethodsPaired(this));
	}
}
