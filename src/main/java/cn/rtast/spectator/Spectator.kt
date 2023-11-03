/*
 * Copyright 2023 RTAkland
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package cn.rtast.spectator

import com.mojang.brigadier.context.CommandContext
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.registry.RegistryKey
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.world.GameMode
import net.minecraft.world.World

class Spectator : DedicatedServerModInitializer {

    companion object {
        val playerStates = mutableListOf<PlayerProfile>()
    }

    override fun onInitializeServer() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(CommandManager.literal("s").executes { this.record(it);1 })
        }
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(CommandManager.literal("c").executes { this.back(it);1 })
        }

        println("Spectator 已加载.")
    }

    private fun record(ctx: CommandContext<ServerCommandSource>) {
        val player = ctx.source.player!!
        val playerName = player.name.string
        if (playerStates.any { it.playerName == playerName }) {
            player.sendMessage(
                Text.literal("设置记录点失败: 有已经存在的记录点, 使用/c来回到上个记录点")
                    .styled { it.withColor(Formatting.RED) }, true
            )
        } else {
            val pos = player.pos
            val gameMode = player.interactionManager.gameMode
            val dimension = player.world.registryKey
            val x = pos.x
            val y = pos.y
            val z = pos.z
            val yaw = player.yaw
            val pitch = player.pitch
            val state = PlayerProfile(
                playerName, PlayerPos(x, y, z, yaw, pitch), gameMode, dimension
            )
            ctx.source.player!!.changeGameMode(GameMode.SPECTATOR)
            playerStates.add(state)
            player.sendMessage(
                Text.literal("设置记录点成功: (${dimension.value.path}) (${x.toInt()} ${y.toInt()} ${z.toInt()}) (${player.interactionManager.gameMode.name})")
                    .styled { it.withColor(Formatting.GREEN) }, true
            )
        }
    }

    private fun back(ctx: CommandContext<ServerCommandSource>) {
        val player = ctx.source.player!!
        val playerName = player.name.string
        val playerProfile = playerStates.firstOrNull { it.playerName == playerName }
        if (playerProfile == null) {
            player.sendMessage(
                Text.literal("返回记录点失败: 没有设置记录点!").styled { it.withColor(Formatting.RED) }, true
            )
        } else {
            val lastDimension = ctx.source.server.getWorld(playerProfile.dimension)!!.toServerWorld()
            val x = playerProfile.pos.x
            val y = playerProfile.pos.y
            val z = playerProfile.pos.z
            val yaw = playerProfile.pos.yaw
            val pitch = playerProfile.pos.pitch
            player.teleport(lastDimension, x, y, z, yaw, pitch)
            player.changeGameMode(playerProfile.gameMode)
            player.sendMessage(
                Text.literal("返回记录点成功: (${playerProfile.dimension.value.path}) (${x.toInt()} ${y.toInt()} ${z.toInt()}) (${playerProfile.gameMode.name})")
                    .styled { it.withColor(Formatting.GREEN) }, true
            )
            playerStates.removeIf { it.playerName == playerName }
        }
    }

    data class PlayerProfile(
        val playerName: String,
        val pos: PlayerPos,
        val gameMode: GameMode,
        val dimension: RegistryKey<World>
    )

    data class PlayerPos(
        val x: Double,
        val y: Double,
        val z: Double,
        val yaw: Float,
        val pitch: Float
    )
}
