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

package eu.cloudnetservice.plugins.chat;

import eu.cloudnetservice.common.document.gson.JsonDocument;
import java.nio.file.Paths;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.extensions.Extension;
import org.bukkit.ChatColor;

public class MinestomChatExtension extends Extension {

  private String format;
  private JsonDocument config;
  private EventNode<PlayerEvent> node = EventNode.type("chat", EventFilter.PLAYER);

  @Override
  public void initialize() {
    config = JsonDocument.newDocument(Paths.get(getDataDirectory().toString(), "config.json"));
    format = config.getString("format", "%display%%name% &8:&f %message%");
    if (!config.contains("format")) {
      config.append("format", "%display%%name% &8:&f %message%");
    }
    config.write(Paths.get(getDataDirectory().toString(), "config.json"));

    node.addListener(PlayerChatEvent.class, event -> {
      var player = event.getPlayer();
      var formattedMessage = ChatFormatter.buildFormat(
        player.getUuid(),
        player.getUsername(),
        LegacyComponentSerializer.legacySection()
          .serialize(player.getDisplayName() != null ? player.getDisplayName() : player.getName()),
        this.format,
        event.getMessage(),
        player::hasPermission,
        ((altColorChar, textToTranslate) -> {
          char[] b = textToTranslate.toCharArray();
          for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[i + 1]) > -1) {
              b[i] = ChatColor.COLOR_CHAR;
              b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
          }
          return new String(b);
        }));

      if (formattedMessage == null) {
        event.setCancelled(true);
      } else {
        event.setChatFormat((playerChatEvent -> Component.text(formattedMessage)));
      }
    });

    MinecraftServer.getGlobalEventHandler().addChild(node);

  }

  @Override
  public void terminate() {

  }
}
