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

package eu.cloudnetservice.modules.cloudperms.minestom.listener;

import eu.cloudnetservice.cloudnet.driver.permission.PermissionManagement;
import eu.cloudnetservice.modules.cloudperms.CloudPermissionsHelper;
import eu.cloudnetservice.modules.cloudperms.minestom.MinestomPermissionsHelper;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.extensions.Extension;
import net.minestom.server.extras.MojangAuth;

public class MinestomCloudPermissionsPlayerListener {

  private final Extension extension;
  private final PermissionManagement permissionsManagement;
  private final EventNode<PlayerEvent> node = EventNode.type("cperms", EventFilter.PLAYER);

  public MinestomCloudPermissionsPlayerListener(Extension extension, PermissionManagement permissionsManagement) {
    this.extension = extension;
    this.permissionsManagement = permissionsManagement;
    init();
  }

  private void init() {
    node.addListener(AsyncPlayerPreLoginEvent.class, event -> {
        CloudPermissionsHelper.initPermissionUser(
          this.permissionsManagement,
          event.getPlayer().getUuid(),
          LegacyComponentSerializer.legacySection().serialize(event.getPlayer().getName()),
          message -> {
            event.getPlayer().kick(message);
          },
          MojangAuth.isEnabled());
      })
      .addListener(PlayerDisconnectEvent.class, event -> {
        CloudPermissionsHelper.handlePlayerQuit(this.permissionsManagement, event.getPlayer().getUuid());
      });

    MinecraftServer.getGlobalEventHandler().addChild(node);
  }

}
