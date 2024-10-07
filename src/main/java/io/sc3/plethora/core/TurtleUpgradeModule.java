package io.sc3.plethora.core;

import com.mojang.authlib.GameProfile;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Direction;
import io.sc3.plethora.api.IPlayerOwnable;
import io.sc3.plethora.api.IWorldLocation;
import io.sc3.plethora.api.TurtleWorldLocation;
import io.sc3.plethora.api.method.ContextKeys;
import io.sc3.plethora.api.module.IModuleAccess;
import io.sc3.plethora.api.module.IModuleContainer;
import io.sc3.plethora.api.module.IModuleHandler;
import io.sc3.plethora.api.module.SingletonModuleContainer;
import io.sc3.plethora.api.reference.ConstantReference;
import io.sc3.plethora.api.reference.IReference;
import io.sc3.plethora.api.reference.Reference;
import io.sc3.plethora.core.executor.TaskRunner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Wraps a module item as a turtle upgrade.
 */
public class TurtleUpgradeModule implements ITurtleUpgrade {
	private final IModuleHandler handler;
	private final ItemStack stack;
	private final String adjective;

	public TurtleUpgradeModule(@Nonnull ItemStack stack, @Nonnull IModuleHandler handler, @Nonnull String adjective) {
		this.handler = handler;
		this.stack = stack;
		this.adjective = adjective;
	}

  @Nonnull
  public IModuleHandler getHandler() {
    return handler;
  }

  @Nonnull
	@Override
	public Identifier getUpgradeID() {
		return handler.getModule();
	}

	@Nonnull
	@Override
	public String getUnlocalisedAdjective() {
		return adjective;
	}

	@Nonnull
	@Override
	public TurtleUpgradeType getType() {
		return TurtleUpgradeType.PERIPHERAL;
	}

	@Nonnull
	@Override
	public ItemStack getCraftingItem() {
		return stack;
	}

	protected boolean isBlacklisted() {
		return false;
		// TODO: Module blacklist
//		String moduleName = handler.getModule().toString();
//		return ConfigCore.Blacklist.blacklistModulesTurtle.contains(moduleName) || ConfigCore.Blacklist.blacklistModules.contains(moduleName);
	}

	@Override
	public IPeripheral createPeripheral(@Nonnull final ITurtleAccess turtle, @Nonnull final TurtleSide side) {
		if (isBlacklisted()) return null;

		MethodRegistry registry = MethodRegistry.instance;

		final TurtleModuleAccess access = new TurtleModuleAccess(turtle, side, handler);

		final IModuleContainer container = access.getContainer();
		IReference<IModuleContainer> containerRef = new ConstantReference<>() {
      @Nonnull
      @Override
      public IModuleContainer get() throws LuaException {
        if (turtle.getUpgrade(side) != TurtleUpgradeModule.this) throw new LuaException("The upgrade is gone");
        return container;
      }

      @Nonnull
      @Override
      public IModuleContainer safeGet() throws LuaException {
        return get();
      }
    };

		ContextFactory<IModuleContainer> factory = ContextFactory.of(container, containerRef)
			.withCostHandler(DefaultCostHandler.get(turtle))
			.withModules(container, containerRef)
			.addContext(ContextKeys.ORIGIN, new TurtlePlayerOwnable(turtle))
			.addContext(ContextKeys.ORIGIN, new TurtleWorldLocation(turtle))
			.addContext(ContextKeys.ORIGIN, turtle, Reference.id(turtle));

		handler.getAdditionalContext(stack, access, factory);

		Pair<List<RegisteredMethod<?>>, List<UnbakedContext<?>>> paired = registry.getMethodsPaired(factory.getBaked());
		if (paired.getLeft().isEmpty()) return null;

		AttachableWrapperPeripheral peripheral = new AttachableWrapperPeripheral(handler.getModule().toString(), this, paired, new TaskRunner(), factory.getAttachments());
		access.wrapper = peripheral;
		return peripheral;
	}

	@Nonnull
	@Override
	public TurtleCommandResult useTool(@Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side, @Nonnull TurtleVerb verb, @Nonnull Direction direction) {
		return TurtleCommandResult.failure("Cannot use tool");
	}

	@Override
	public void update(@Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side) {
		IPeripheral peripheral = turtle.getPeripheral(side);
		if (peripheral instanceof MethodWrapperPeripheral) {
			((MethodWrapperPeripheral) peripheral).getRunner().update();
		}
	}

	private static final class TurtleModuleAccess implements IModuleAccess {
		private AttachableWrapperPeripheral wrapper;

		private final ITurtleAccess access;
		private final TurtleSide side;
		private final IWorldLocation location;
		private final IModuleContainer container;

		private TurtleModuleAccess(ITurtleAccess access, TurtleSide side, IModuleHandler handler) {
			this.access = access;
			this.side = side;
			location = new TurtleWorldLocation(access);
			container = new SingletonModuleContainer(handler.getModule());
		}

		@Nonnull
		@Override
		public Object getOwner() {
			return access;
		}

		@Nonnull
		@Override
		public IWorldLocation getLocation() {
			return location;
		}

		@Nonnull
		@Override
		public IModuleContainer getContainer() {
			return container;
		}

		@Nonnull
		@Override
		public NbtCompound getData() {
			return access.getUpgradeNBTData(side);
		}

		@Nonnull
		@Override
		public MinecraftServer getServer() {
			return Objects.requireNonNull(location.getWorld().getServer()); // TODO
		}

		@Override
		public void markDataDirty() {
			access.updateUpgradeNBTData(side);
		}

		@Override
		public void queueEvent(@Nonnull String event, @Nullable Object... args) {
			if (wrapper != null) wrapper.queueEvent(event, args);
		}
	}

	public static class TurtlePlayerOwnable implements ConstantReference<TurtlePlayerOwnable>, IPlayerOwnable {
		private final ITurtleAccess access;

		public TurtlePlayerOwnable(ITurtleAccess access) {
			this.access = access;
		}

		@Nullable
		@Override
		public GameProfile getOwningProfile() {
			return access.getOwningPlayer();
		}

		@Nonnull
		@Override
		public TurtlePlayerOwnable get() {
			return this;
		}

		@Nonnull
		@Override
		public TurtlePlayerOwnable safeGet() {
			return this;
		}
	}
}
