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

package eu.cloudnetservice.modules.cloudperms.minestom;

import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.modules.cloudperms.minestom.listener.MinestomCloudPermissionsPlayerListener;
import eu.cloudnetservice.modules.cloudperms.minestom.player.CloudPlayer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.extensions.Extension;

public class MinestomCloudPermissionsExtension extends Extension {

  @Override
  public void initialize() {

    new MinestomCloudPermissionsPlayerListener(this, CloudNetDriver.instance().permissionManagement());

    MinecraftServer.getConnectionManager().setPlayerProvider(CloudPlayer::new);

    /*CloudNetDriver.instance().eventManager()
      .registerListener(
        new PermissionsUpdateListener<>(
          runnable -> MinecraftServer.getSchedulerManager().buildTask(runnable).schedule(),
          MinestomPermissionsHelper::initPlayer,
          Player::getUuid,
          MinecraftServer.getConnectionManager()::getPlayer,
          MinecraftServer.getConnectionManager()::getOnlinePlayers
        )
      );*/
  }

  @Override
  public void terminate() {
    CloudNetDriver.instance().eventManager().unregisterListeners(this.getClass().getClassLoader());
    Wrapper.instance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
  }
}
