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

import eu.cloudnetservice.modules.bridge.platform.PlatformPlayerExecutorAdapter;
import eu.cloudnetservice.modules.bridge.player.executor.ServerSelectorType;
import java.time.Duration;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minestom.server.entity.Player;
import net.minestom.server.extensions.Extension;
import org.jetbrains.annotations.Nullable;

public class MinestomDirectPlayerExecutor extends PlatformPlayerExecutorAdapter {

  private final Extension extension;
  private final UUID targetUniqueId;
  private final Supplier<Collection<? extends Player>> playerSupplier;

  public MinestomDirectPlayerExecutor(
    @NonNull Extension extension,
    @NonNull UUID target,
    @NonNull Supplier<Collection<? extends Player>> playerSupplier
  ) {
    this.extension = extension;
    this.targetUniqueId = target;
    this.playerSupplier = playerSupplier;
  }

  @Override
  public @NonNull UUID uniqueId() {
    return this.targetUniqueId;
  }

  @Override
  public void connect(@NonNull String serviceName) {
    // no-op
  }

  @Override
  public void connectSelecting(@NonNull ServerSelectorType selectorType) {
    // no-op
  }

  @Override
  public void connectToFallback() {
    // no-op
  }

  @Override
  public void connectToGroup(@NonNull String group, @NonNull ServerSelectorType selectorType) {
    // no-op
  }

  @Override
  public void connectToTask(@NonNull String task, @NonNull ServerSelectorType selectorType) {
    // no-op
  }

  @Override
  public void kick(@NonNull Component message) {
    this.playerSupplier.get().forEach(player -> player.kick(message));
  }

  @Override
  protected void sendTitle(@NonNull Component title, @NonNull Component subtitle, int fadeIn, int stay, int fadeOut) {
    final Title.Times times = Title.Times.of(Duration.ofMillis(fadeIn), Duration.ofMillis(stay),
      Duration.ofMillis(fadeOut));
    this.playerSupplier.get().forEach(player -> player.showTitle(
      Title.title(title, subtitle, times)
    ));
  }

  @Override
  public void sendMessage(@NonNull Component message) {
    this.playerSupplier.get().forEach(player -> player.sendMessage(message));
  }

  @Override
  public void sendChatMessage(@NonNull Component message, @Nullable String permission) {
    this.playerSupplier.get().forEach(player -> {
      if (permission == null || player.hasPermission(permission)) {
        player.sendMessage(message);
      }
    });
  }

  @Override
  public void sendPluginMessage(@NonNull String tag, byte[] data) {
    this.playerSupplier.get().forEach(player -> player.sendPluginMessage(tag, data));
  }

  @Override
  public void spoofChatInput(@NonNull String command) {
    this.playerSupplier.get().forEach(player -> player.chat(command));
  }

}
