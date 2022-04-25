/*
 * Copyright 2019-2022 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cloudnetservice.modules.bridge.platform.minestom;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.bridge.BridgeServiceHelper;
import eu.cloudnetservice.modules.bridge.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerServerInfo;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.bridge.player.ServicePlayer;
import eu.cloudnetservice.modules.bridge.player.executor.PlayerExecutor;
import eu.cloudnetservice.modules.bridge.util.BridgeHostAndPortUtil;
import eu.cloudnetservice.wrapper.Wrapper;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.NonNull;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.extensions.Extension;
import net.minestom.server.permission.PermissionHandler;
import net.minestom.server.ping.ServerListPingType;
import org.jetbrains.annotations.Nullable;

public class MinestomBridgeManagement extends PlatformBridgeManagement<Player, NetworkPlayerServerInfo> {

  private static final BiFunction<Player, String, Boolean> PERM_FUNCTION = PermissionHandler::hasPermission;

  private final Extension extension;
  private final PlayerExecutor directGlobalExecutor;

  public MinestomBridgeManagement(Extension extension) {
    super(Wrapper.instance());
    this.extension = extension;
    this.directGlobalExecutor = new MinestomDirectPlayerExecutor(
      this.extension,
      PlayerExecutor.GLOBAL_UNIQUE_ID,
      MinecraftServer.getConnectionManager()::getOnlinePlayers
    );

    //As the motd is set upon the ping event we have to listen for it manually
    ServerListPingEvent event = new ServerListPingEvent(ServerListPingType.MODERN_FULL_RGB);
    MinecraftServer.getGlobalEventHandler().call(event);
    //Returns the motd from the impl/extension or the default A Minecraft Server motd

    BridgeServiceHelper.MOTD.set(legacySection().serialize(event.getResponseData().getDescription())); //TODO test this
    BridgeServiceHelper.MAX_PLAYERS.set(event.getResponseData().getMaxPlayer()); //maybe find a better way
  }

  @Override
  public void registerServices(@NonNull ServiceRegistry registry) {
    registry.registerProvider(PlayerManager.class, "PlayerManager", this.playerManager);
    registry.registerProvider(PlatformBridgeManagement.class, "MinestomBridgeManagement", this);
  }

  @Override
  public @NonNull ServicePlayer wrapPlayer(@NonNull Player player) {
    return new ServicePlayer(player.getUuid(), legacySection().serialize(player.getName()));
  }

  @Override
  public @NonNull NetworkPlayerServerInfo createPlayerInformation(
    @NonNull Player player) {
    return new NetworkPlayerServerInfo(
      player.getUuid(),
      legacySection().serialize(player.getName()),
      null,
      BridgeHostAndPortUtil.fromSocketAddress(player.getPlayerConnection().getRemoteAddress()),
      this.ownNetworkServiceInfo
    );
  }

  @Override
  public @NonNull BiFunction<Player, String, Boolean> permissionFunction() {
    return PERM_FUNCTION;
  }

  @Override
  public boolean isOnAnyFallbackInstance(@NonNull Player player) {
    return this.isOnAnyFallbackInstance(this.ownNetworkServiceInfo.serverName(), null, player::hasPermission);
  }

  @Override
  public @NonNull Optional<ServiceInfoSnapshot> fallback(
    @NonNull Player player) {
    return this.fallback(player, this.ownNetworkServiceInfo.serverName());
  }

  @Override
  public @NonNull Optional<ServiceInfoSnapshot> fallback(@NonNull Player player, @Nullable String currServer) {
    return this.fallback(player.getUuid(), currServer, null, player::hasPermission);
  }

  @Override
  public void handleFallbackConnectionSuccess(@NonNull Player player) {
    this.handleFallbackConnectionSuccess(player.getUuid());
  }

  @Override
  public void removeFallbackProfile(@NonNull Player player) {
    this.removeFallbackProfile(player.getUuid());
  }

  @Override
  public @NonNull PlayerExecutor directPlayerExecutor(@NonNull UUID uniqueId) {
    return uniqueId.equals(PlayerExecutor.GLOBAL_UNIQUE_ID)
      ? this.directGlobalExecutor
      : new MinestomDirectPlayerExecutor(this.extension, uniqueId, () ->
        Collections.singleton(MinecraftServer.getConnectionManager().getPlayer(uniqueId)));
  }

  @Override
  public void appendServiceInformation(@NonNull ServiceInfoSnapshot snapshot) {
    super.appendServiceInformation(snapshot);
    // append the Minestom specific information
    snapshot.properties().append("Online-Count", MinecraftServer.getConnectionManager().getOnlinePlayers().size());
    snapshot.properties().append("Version", MinecraftServer.VERSION_NAME);
    // players
    snapshot.properties().append("Players", MinecraftServer.getConnectionManager().getOnlinePlayers().stream()
      .map(this::createPlayerInformation)
      .collect(Collectors.toList()));
  }
}
