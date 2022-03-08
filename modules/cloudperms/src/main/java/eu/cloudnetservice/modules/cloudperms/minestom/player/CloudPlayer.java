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

package eu.cloudnetservice.modules.cloudperms.minestom.player;

import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.driver.permission.PermissionManagement;
import eu.cloudnetservice.cloudnet.driver.permission.PermissionUser;
import java.util.UUID;
import net.minestom.server.entity.Player;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.permission.Permission;
import net.minestom.server.permission.PermissionVerifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CloudPlayer extends Player {

  public CloudPlayer(@NotNull UUID uuid,
    @NotNull String username,
    @NotNull PlayerConnection playerConnection) {
    super(uuid, username, playerConnection);
  }

  @Override
  public boolean hasPermission(@NotNull String permissionName) {
    return hasPermission(permissionName, null);
  }

  @Override
  public boolean hasPermission(@NotNull Permission permission) {
    return hasPermission(permission.getPermissionName());
  }

  @Override
  public boolean hasPermission(@NotNull String permissionName,
    @Nullable PermissionVerifier permissionVerifier) {
    PermissionManagement management = CloudNetDriver.instance().permissionManagement();
    if (management == null) {
      return super.hasPermission(permissionName, permissionVerifier);
    }
    PermissionUser user = management.user(this.uuid);
    if (user == null) {
      return super.hasPermission(permissionName, permissionVerifier);
    }
    return management.hasPermission(user, eu.cloudnetservice.cloudnet.driver.permission.Permission.of(permissionName))
      || super.hasPermission(permissionName, permissionVerifier);
  }
}
