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

import eu.cloudnetservice.modules.bridge.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.platform.helper.ServerPlatformHelper;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerServerInfo;
import eu.cloudnetservice.wrapper.Wrapper;
import java.util.Locale;
import lombok.NonNull;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.extensions.Extension;

public class MinestomPlayerManagementListener {

  private final Extension extension;
  private final PlatformBridgeManagement<Player, NetworkPlayerServerInfo> management;
  private final EventNode<PlayerEvent> node = EventNode.type("bridge", EventFilter.PLAYER);

  public MinestomPlayerManagementListener(@NonNull Extension extension,
    @NonNull PlatformBridgeManagement<Player, NetworkPlayerServerInfo> management) {
    this.extension = extension;
    this.management = management;
    initListeners();
  }

  private void initListeners() {
    node.addListener(AsyncPlayerPreLoginEvent.class, event -> {
        var task = this.management.selfTask();
        if (task != null) {
          // check if the current task is present
          var player = event.getPlayer();
          // check if maintenance is activated
          if (task.maintenance() && !event.getPlayer().hasPermission("cloudnet.bridge.maintenance")) {
            player.kick(this.management.configuration().message(
              Locale.forLanguageTag(event.getPlayer().getSettings().getLocale()),
              "server-join-cancel-because-maintenance"));
            return;
          }
          // check if a custom permission is required to join
          var permission = task.properties().getString("requiredPermission");
          if (permission != null && !event.getPlayer().hasPermission(permission)) {
            player.kick(this.management.configuration().message(
              Locale.forLanguageTag(event.getPlayer().getSettings().getLocale()),
              "server-join-cancel-because-permission"));
            return;
          }
        }

        ServerPlatformHelper.sendChannelMessageLoginSuccess(
          event.getPlayer().getUuid(),
          this.management.createPlayerInformation(event.getPlayer()));
        // update the service info in the next tick
        MinecraftServer.getSchedulerManager()
          .buildTask(() -> Wrapper.instance().publishServiceInfoUpdate())
          .schedule();
      })
      .addListener(PlayerDisconnectEvent.class, event -> {
        ServerPlatformHelper.sendChannelMessageDisconnected(
          event.getPlayer().getUuid(),
          this.management.ownNetworkServiceInfo());
        // update the service info in the next tick
        MinecraftServer.getSchedulerManager()
          .buildTask(() -> Wrapper.instance().publishServiceInfoUpdate())
          .schedule();
      });
    MinecraftServer.getGlobalEventHandler().addChild(node);
  }

}
