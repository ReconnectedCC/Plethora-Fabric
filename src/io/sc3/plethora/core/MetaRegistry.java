package io.sc3.plethora.core;

import com.google.common.collect.Lists;
import io.sc3.plethora.api.meta.IMetaProvider;
import io.sc3.plethora.api.meta.IMetaRegistry;
import io.sc3.plethora.api.meta.TypedMeta;
import io.sc3.plethora.api.method.ContextKeys;
import io.sc3.plethora.api.method.IPartialContext;
import io.sc3.plethora.core.collections.ClassIteratorIterable;
import io.sc3.plethora.core.collections.SortedMultimap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static io.sc3.plethora.Plethora.log;

public final class MetaRegistry implements IMetaRegistry {
    public static final MetaRegistry instance = new MetaRegistry();

    private static final Map<IMetaProvider<?>, TargetedRegisteredValue<? extends IMetaProvider<?>>> all = new HashMap<>();
    final SortedMultimap<Class<?>, IMetaProvider<?>> providers = SortedMultimap.create(Comparator.comparingInt(IMetaProvider::getPriority));

    void registerMetaProvider(@Nonnull TargetedRegisteredValue<? extends IMetaProvider<?>> provider) {
        Objects.requireNonNull(provider, "provider cannot be null");
        all.put(provider.value(), provider);
    }

    public <T extends IMetaProvider<?>> void registerMetaProvider(@Nonnull String name, @Nullable String mod, @Nonnull Class<?> target, @Nonnull T value) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(target, "target cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        registerMetaProvider(new TargetedRegisteredValue<>(name, mod, target, value));
    }

    void build() {
        TargetedRegisteredValue.buildCache(all.values(), providers);
    }

    public String getName(@Nonnull IMetaProvider<?> provider) {
        TargetedRegisteredValue<? extends IMetaProvider<?>> item = all.get(provider);
        return item == null ? provider.getClass().getName() : item.getRegName();
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public <T> TypedMeta<T, ?> getMeta(@Nonnull PartialContext<T> context) {
        Objects.requireNonNull(context, "context cannot be null");

        String[] keys = context.keys;
        Object[] values = context.values;

        // TODO: Handle priority across each conversion correctly

        HashTypedMeta<T, Object> result = null;
        Map<String, ?> first = null;
        for (int i = values.length - 1; i >= 0; i--) {
            if (!ContextKeys.TARGET.equals(keys[i])) continue;

            Object child = values[i];
            IPartialContext<?> childContext = context.withIndex(i);

            for (IMetaProvider provider : getMetaProviders(child.getClass())) {
                Map<String, ?> res = provider.getMeta(childContext);
                if (res == null) {
                    log.error("Meta provider {} returned null", getName(provider));
                    continue;
                }

                if (res.isEmpty()) continue;

                if (result != null) {
                    result.putAll(res);
                } else if (first != null) {
                    result = new HashTypedMeta<>(first.size() + res.size());
                    result.putAll(first);
                    result.putAll(res);
                } else {
                    first = res;
                }
            }
        }

        if (result != null) return result;
        if (first != null) return new WrapperTypedMeta<>(first);
        return WrapperTypedMeta.empty();
    }

    @Nonnull
    @Override
    public List<IMetaProvider<?>> getMetaProviders(@Nonnull Class<?> target) {
        Objects.requireNonNull(target, "target cannot be null");

        List<IMetaProvider<?>> result = Lists.newArrayList();

        for (Class<?> klass : new ClassIteratorIterable(target)) {
            result.addAll(providers.get(klass));
        }

        return Collections.unmodifiableList(result);
    }

    private static class HashTypedMeta<T, V> extends HashMap<String, V> implements TypedMeta<T, V> {
        private static final long serialVersionUID = 2925566988195565014L;

        HashTypedMeta(int initialCapacity) {
            super(initialCapacity);
        }

        public HashTypedMeta(Map<? extends String, ? extends V> m) {
            super(m);
        }
    }

    private static final class WrapperTypedMeta<T, V> implements TypedMeta<T, V> {
        private static final WrapperTypedMeta<?, ?> EMPTY = new WrapperTypedMeta<>(Collections.emptyMap());

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public static <T> TypedMeta<T, ?> empty() {
            return (TypedMeta) EMPTY;
        }

        private final Map<String, V> wrapper;

        WrapperTypedMeta(Map<String, V> wrapper) {
            this.wrapper = wrapper;
        }

        @Override
        public int size() {
            return wrapper.size();
        }

        @Override
        public boolean isEmpty() {
            return wrapper.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return wrapper.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return wrapper.containsValue(value);
        }

        @Override
        public V get(Object key) {
            return wrapper.get(key);
        }

        @Override
        public V put(String key, V value) {
            return wrapper.put(key, value);
        }

        @Override
        public V remove(Object key) {
            return wrapper.remove(key);
        }

        @Override
        public void putAll(@Nonnull Map<? extends String, ? extends V> m) {
            wrapper.putAll(m);
        }

        @Override
        public void clear() {
            wrapper.clear();
        }

        @Nonnull
        @Override
        public Set<String> keySet() {
            return wrapper.keySet();
        }

        @Nonnull
        @Override
        public Collection<V> values() {
            return wrapper.values();
        }

        @Nonnull
        @Override
        public Set<Map.Entry<String, V>> entrySet() {
            return wrapper.entrySet();
        }
    }
}
