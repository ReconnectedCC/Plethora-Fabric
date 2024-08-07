package io.sc3.plethora.core;

import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import io.sc3.plethora.api.EntityWorldLocation;
import io.sc3.plethora.api.IWorldLocation;
import io.sc3.plethora.api.method.ContextKeys;
import io.sc3.plethora.api.method.CostHelpers;
import io.sc3.plethora.api.method.ICostHandler;
import io.sc3.plethora.api.module.IModuleAccess;
import io.sc3.plethora.api.module.IModuleContainer;
import io.sc3.plethora.api.module.IModuleHandler;
import io.sc3.plethora.api.module.SingletonModuleContainer;
import io.sc3.plethora.api.reference.ConstantReference;
import io.sc3.plethora.api.reference.IReference;
import io.sc3.plethora.api.reference.Reference;
import io.sc3.plethora.api.vehicle.IVehicleAccess;
import io.sc3.plethora.api.vehicle.IVehicleUpgradeHandler;
import io.sc3.plethora.core.executor.TaskRunner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

public class VehicleUpgradeModule implements IVehicleUpgradeHandler {
	private final IModuleHandler handler;

	public VehicleUpgradeModule(IModuleHandler handler) {
		this.handler = handler;
	}

	@Nonnull
	@Override
	public TransformedModel getModel(@Nonnull IVehicleAccess access) {
		return handler.getModel(); // TODO
	}

	@Override
	public void update(@Nonnull IVehicleAccess vehicle, @Nonnull IPeripheral peripheral) {
		if (peripheral instanceof MethodWrapperPeripheral) {
			// TODO
			// ((MethodWrapperPeripheral) peripheral).getRunner().update();
		}
	}

	@Nullable
	@Override
	public IPeripheral create(@Nonnull IVehicleAccess vehicle) {
		final Identifier thisModule = handler.getModule();

		String moduleName = thisModule.toString();
		// TODO: Module blacklist
//		if (ConfigCore.Blacklist.blacklistModulesVehicle.contains(moduleName) || ConfigCore.Blacklist.blacklistModules.contains(moduleName)) {
//			return null;
//		}

		MethodRegistry registry = MethodRegistry.instance;
		Entity entity = vehicle.getVehicle();

		ICostHandler cost = CostHelpers.getCostHandler(entity, null);

		final VehicleModuleAccess access = new VehicleModuleAccess(vehicle, handler);

		final IModuleContainer container = access.getContainer();
		IReference<IModuleContainer> containerRef = new ConstantReference<IModuleContainer>() {
			@Nonnull
			@Override
			public IModuleContainer get() {
				// if (turtle.getUpgrade(side) != TurtleUpgradeModule.this) throw new LuaException("The upgrade is gone");
				// TODO: Correctly invalidate this peripheral when it is detached.
				return container;
			}

			@Nonnull
			@Override
			public IModuleContainer safeGet() {
				return get();
			}
		};

		ContextFactory<IModuleContainer> factory = ContextFactory.of(container, containerRef)
			.withCostHandler(cost)
			.withModules(container, containerRef)
			.addContext(ContextKeys.ORIGIN, new EntityWorldLocation(entity))
			.addContext(ContextKeys.ORIGIN, vehicle, Reference.id(vehicle))
			.addContext(ContextKeys.ORIGIN, vehicle.getVehicle(), Reference.entity(vehicle.getVehicle()));

		handler.getAdditionalContext(ItemStack.EMPTY, access, factory); // TODO: ItemStack

		Pair<List<RegisteredMethod<?>>, List<UnbakedContext<?>>> paired = registry.getMethodsPaired(factory.getBaked());
		if (paired.getLeft().isEmpty()) return null;

		AttachableWrapperPeripheral peripheral = new AttachableWrapperPeripheral(moduleName, this, paired, new TaskRunner(), factory.getAttachments());
		access.wrapper = peripheral;
		return peripheral;
	}

	private static final class VehicleModuleAccess implements IModuleAccess {
		private AttachableWrapperPeripheral wrapper;

		private final IVehicleAccess access;
		private final IWorldLocation location;
		private final IModuleContainer container;

		private VehicleModuleAccess(IVehicleAccess access, IModuleHandler handler) {
			this.access = access;
			location = new EntityWorldLocation(access.getVehicle());
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
			return access.getData();
		}

		@Nonnull
		@Override
		public MinecraftServer getServer() {
			return Objects.requireNonNull(location.getWorld().getServer()); // TODO
		}

		@Override
		public void markDataDirty() {
			access.markDataDirty();
		}

		@Override
		public void queueEvent(@Nonnull String event, @Nullable Object... args) {
			if (wrapper != null) wrapper.queueEvent(event, args);
		}
	}
}
