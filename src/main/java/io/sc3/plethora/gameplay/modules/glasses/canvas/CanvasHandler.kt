package io.sc3.plethora.gameplay.modules.glasses.canvas

import com.mojang.blaze3d.systems.RenderSystem
import io.sc3.plethora.Plethora
import io.sc3.plethora.gameplay.modules.glasses.networking.CanvasAddPacket
import io.sc3.plethora.gameplay.modules.glasses.networking.CanvasRemovePacket
import io.sc3.plethora.gameplay.modules.glasses.networking.CanvasUpdatePacket
import io.sc3.plethora.gameplay.neural.NeuralComputerHandler.MODULE_DATA
import io.sc3.plethora.gameplay.neural.NeuralHelpers
import io.sc3.plethora.gameplay.registry.PlethoraModules.GLASSES_S
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.nbt.NbtElement.COMPOUND_TYPE
import net.minecraft.nbt.NbtElement.NUMBER_TYPE
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.entity.player.PlayerEntity
import java.util.concurrent.atomic.AtomicInteger

object CanvasHandler {
  const val ID_2D = 0
  const val ID_3D = 1

  // TODO: Configurable framebuffer scale
  const val WIDTH = 512
  const val HEIGHT = 512 / 16 * 9

  private val id = AtomicInteger(0)
  private val server = HashSet<CanvasServer>()

  private val client: Int2ObjectMap<CanvasClient> = Int2ObjectOpenHashMap()

  fun nextId() = id.getAndIncrement()

  fun addServer(canvas: CanvasServer) {
    synchronized(server) {
      server.add(canvas)
      canvas.makeAddPacket()?.let { ServerPlayNetworking.send(canvas.player, CanvasAddPacket.id, it.toBytes()) }
    }
  }

  fun removeServer(canvas: CanvasServer) {
    synchronized(server) {
      server.remove(canvas)
      canvas.makeRemovePacket()?.let { ServerPlayNetworking.send(canvas.player, CanvasRemovePacket.id, it.toBytes()) }
    }
  }

  @JvmStatic
  fun addClient(canvas: CanvasClient) {
    synchronized(client) { client.put(canvas.id, canvas) }
  }

  @JvmStatic
  fun removeClient(canvas: CanvasClient) {
    synchronized(client) { client.remove(canvas.id) }
  }

  @JvmStatic
  fun getClient(id: Int): CanvasClient? {
    synchronized(client) { return client[id] }
  }

  fun clear() {
    synchronized(server) { server.clear() }
    synchronized(client) { client.clear() }
  }

  fun update() {
    synchronized(server) {
      for (canvas in server) {
        canvas.makeUpdatePacket()?.let {
          try {
            ServerPlayNetworking.send(canvas.player, CanvasUpdatePacket.id, it.toBytes())
          } catch (e: Exception) {
            Plethora.log.error("Error sending canvas update packet", e)
          }
        }
      }
    }
  }

  @Environment(EnvType.CLIENT)
  fun getCanvas(client: MinecraftClient): CanvasClient? {
    val player: PlayerEntity? = client.player

    val optStack = NeuralHelpers.getStack(player)
    if (optStack.isEmpty) return null
    val stack = optStack.get()

    val nbt = stack.nbt
    if (nbt == null || !nbt.contains(MODULE_DATA, COMPOUND_TYPE.toInt())) return null

    val modules = nbt.getCompound(MODULE_DATA)
    if (!modules.contains(GLASSES_S, COMPOUND_TYPE.toInt())) return null

    val data = modules.getCompound(GLASSES_S)
    if (!data.contains("id", NUMBER_TYPE.toInt())) return null

    val id = data.getInt("id")
    return getClient(id)
  }

  @JvmStatic
  fun render2DOverlay(client: MinecraftClient, ctx: DrawContext) {
    val canvas = getCanvas(client) ?: return

    // If we've no text renderer then we're probably not quite ready yet
    if (client.textRenderer == null) return

    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)

    val currentFog = RenderSystem.getShaderFogEnd()
    val currentFogColor = RenderSystem.getShaderFogColor()
    RenderSystem.setShaderFogEnd(2000.0f)
    RenderSystem.setShaderFogColor(0.0f, 0.0f, 0.0f, 0.0f)

    val matrices = ctx.matrices
    matrices.push()

    // The hotbar renders at -90 (see InGameGui#renderHotbar)
    matrices.translate(0.0, 0.0, -200.0)
    matrices.scale(client.window.scaledWidth.toFloat() / WIDTH, client.window.scaledHeight.toFloat() / HEIGHT, 1f)

    synchronized(canvas) {
      canvas.getChildren(ID_2D)?.let {
        canvas.drawChildren(it.iterator(), ctx, null)
      }
    }

    // Restore the renderer state
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
    RenderSystem.enableCull()
    RenderSystem.defaultBlendFunc()
    RenderSystem.enableBlend()

    RenderSystem.setShaderFogEnd(currentFog)
    RenderSystem.setShaderFogColor(currentFogColor[0], currentFogColor[1], currentFogColor[2], currentFogColor[3])

    matrices.pop()
  }

  private fun onWorldRender(ctx: WorldRenderContext) {
    val mc = MinecraftClient.getInstance()
    val canvas = getCanvas(mc) ?: return
    val drawContext = DrawContext(mc, ctx.matrixStack(), mc.bufferBuilders.entityVertexConsumers) // TODO(1.20.1): correct?

    synchronized(canvas) {
      canvas.getChildren(ID_3D)?.let {
        canvas.drawChildren(it.iterator(), drawContext, ctx.consumers())
      }
    }

    // TODO: GL state
  }

  @JvmStatic
  fun registerServerEvents() {
    ServerTickEvents.START_SERVER_TICK.register { update() }
  }

  @JvmStatic
  @Environment(EnvType.CLIENT)
  fun registerClientEvents() {
    WorldRenderEvents.AFTER_TRANSLUCENT.register(::onWorldRender)
  }
}
