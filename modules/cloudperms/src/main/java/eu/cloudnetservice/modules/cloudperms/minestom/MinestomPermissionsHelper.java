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
import net.minestom.server.entity.Player;
import net.minestom.server.permission.Permission;
import org.jetbrains.annotations.ApiStatus.Internal;

public class MinestomPermissionsHelper {

  @Internal
  public static void initPlayer(Player player){
    var user = CloudNetDriver.instance().permissionManagement().user(player.getUuid());
    if(user == null)return;

    user.permissions()
      .forEach(permission -> {
        if(permission.potency() > 0){
          player.addPermission(new Permission(permission.name()));
        }else{ // Remove Permissions the user doesn't have TODO test this
          if(player.hasPermission(permission.name())){
            player.removePermission(new Permission(permission.name()));
          }
        }
      });

    user.groups().forEach(group -> {
      CloudNetDriver.instance().permissionManagement().group(group.group())
        .permissions().forEach(permission -> {
          if(permission.potency() > 0){
            player.addPermission(new Permission(permission.name()));
          }else{ // Remove Permissions the user doesn't have TODO test this
            if(player.hasPermission(permission.name())){
              player.removePermission(new Permission(permission.name()));
            }
          }
        });
    });

    player.refreshCommands();

  }

}
