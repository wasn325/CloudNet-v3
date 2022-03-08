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

package eu.cloudnetservice.plugins.simplenametags.minestom;

import eu.cloudnetservice.cloudnet.driver.permission.PermissionGroup;
import eu.cloudnetservice.plugins.simplenametags.SimpleNameTagsManager;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Executor;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Team;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.Nullable;

public class MinestomSimpleNameTagsManager extends SimpleNameTagsManager<Player> {

  public MinestomSimpleNameTagsManager(@NonNull Executor syncTaskExecutor) {
    super(syncTaskExecutor);
  }

  @Override
  public void updateNameTagsFor(@NonNull Player player) {
    this.updateNameTagsFor(player, player.getUuid(), player.getUsername());
  }

  @Override
  public @NonNull UUID playerUniqueId(@NonNull Player player) {
    return player.getUuid();
  }

  @Override
  public void displayName(@NonNull Player player, @NonNull String displayName) {
    player.setDisplayName(Component.text(translateAlternateColorCodes('&', displayName)));
  }

  @Override
  public void resetScoreboard(@NonNull Player player) {

  }

  @Override
  public void registerPlayerToTeam(@NonNull Player player,
    @NonNull Player scoreboardHolder, @NonNull String name,
    @NonNull PermissionGroup group) {
    Team team = MinecraftServer.getTeamManager().createBuilder(name)
      .prefix(Component.text(translateAlternateColorCodes('&', group.prefix())))
      .suffix(Component.text(translateAlternateColorCodes('&', group.suffix())))
      .updateTeamPacket()
      .build();

    //TOOD Color

    team.addMember(LegacyComponentSerializer.legacySection().serialize(player.getName()));

  }

  @Override
  public @NonNull Collection<? extends Player> onlinePlayers() {
    return MinecraftServer.getConnectionManager().getOnlinePlayers();
  }

  @Override
  public @Nullable Player onlinePlayer(@NonNull UUID uniqueId) {
    return MinecraftServer.getConnectionManager().getPlayer(uniqueId);
  }

  private String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
    char[] b = textToTranslate.toCharArray();
    for (int i = 0; i < b.length - 1; i++) {
      if (b[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[i + 1]) > -1) {
        b[i] = ChatColor.COLOR_CHAR;
        b[i + 1] = Character.toLowerCase(b[i + 1]);
      }
    }
    return new String(b);
  }
}
