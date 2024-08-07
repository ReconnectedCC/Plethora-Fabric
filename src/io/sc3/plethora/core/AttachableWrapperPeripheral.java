package io.sc3.plethora.core;

import dan200.computercraft.api.peripheral.IComputerAccess;
import net.minecraft.util.Pair;
import io.sc3.plethora.api.method.IAttachable;
import io.sc3.plethora.core.executor.TaskRunner;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link MethodWrapperPeripheral} which has support for mounting {@link IAttachable} objects.
 */
public class AttachableWrapperPeripheral extends MethodWrapperPeripheral {
    private final Collection<IAttachable> attachments;
    private final AtomicInteger count = new AtomicInteger(0);

    public AttachableWrapperPeripheral(
        String name, Object owner, Pair<List<RegisteredMethod<?>>, List<UnbakedContext<?>>> methods,
        TaskRunner runner, Collection<IAttachable> attachments
    ) {
        super(name, owner, methods, runner);
        this.attachments = attachments;
    }

    @Override
    public void attach(@Nonnull IComputerAccess access) {
        super.attach(access);

        int count = this.count.getAndIncrement();
        if (count == 0) {
            for (IAttachable attachable : attachments) {
                attachable.attach();
            }
        }
    }

    @Override
    public void detach(@Nonnull IComputerAccess access) {
        super.detach(access);

        int count = this.count.decrementAndGet();
        if (count == 0) {
            for (IAttachable attachable : attachments) {
                attachable.detach();
            }
        }
    }
}
