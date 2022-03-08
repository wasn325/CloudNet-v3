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

package eu.cloudnetservice.modules.npc.platform.minestom.listener;

import eu.cloudnetservice.modules.npc.platform.PlatformSelectorEntity;
import eu.cloudnetservice.modules.npc.platform.minestom.MinestomPlatformNPCManagement;
import lombok.NonNull;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.event.instance.InstanceChunkLoadEvent;
import net.minestom.server.event.instance.InstanceChunkUnloadEvent;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;

public class MinestomEntityProtectionListener {

  private final MinestomPlatformNPCManagement management;
  private final EventNode<Event> node = EventNode.all("npc");

  public MinestomEntityProtectionListener(
    MinestomPlatformNPCManagement management) {
    this.management = management;
    init();
  }

  private void init() {
    node.addListener(EntityDamageEvent.class, event -> {
        //only listen for attacks made by a player
        if (event.getEntity().getEntityType() != EntityType.PLAYER) {
          return;
        }
        this.management.trackedEntities().values().stream()
          .filter(PlatformSelectorEntity::spawned)
          .filter(npc -> {
            var eid = event.getEntity().getEntityId();
            return npc.entityId() == eid || npc.infoLineEntityIds().contains(eid);
          })
          .findFirst()
          .ifPresent($ -> event.setCancelled(true));
      })
      .addListener(EntityAttackEvent.class, event -> {
        handleClick((Player) event.getEntity(), event.getEntity().getEntityId(), true);
      })
      .addListener(PlayerEntityInteractEvent.class, event -> {
        handleClick(event.getPlayer(),
          event.getEntity().getEntityId(),
          false);
      })
      .addListener(InventoryPreClickEvent.class, event -> {
        var item = event.getClickedItem();
        var inv = event.getInventory();
        var clicker = event.getPlayer();
        // check if we can handle the event
        if (item != null && item.getMeta() != null && inv != null && clicker instanceof Player) {
          this.management.trackedEntities().values().stream()
            .filter(npc -> npc.selectorInventory().equals(inv))
            .findFirst()
            .ifPresent(npc -> {
              event.setCancelled(true);
              npc.handleInventoryInteract(inv, clicker, item);
            });
        }
      })
      .addListener(InstanceChunkLoadEvent.class, event -> {
        this.management.trackedEntities().values()
          .stream()
          .filter(npc -> !npc.spawned())
          .filter(npc -> {
            var chunkX = npc.location().chunkX();
            var chunkZ = npc.location().chunkZ();
            // validate that the entity is in the chunk being loaded - Location#getChunk causes a load of the chunk
            return event.getChunkX() == chunkX && event.getChunkZ() == chunkZ;
          })
          .forEach(PlatformSelectorEntity::spawn);
      })
      .addListener(InstanceChunkUnloadEvent.class, event -> {
        this.management.trackedEntities().values()
          .stream()
          .filter(npc -> !npc.spawned())
          .filter(npc -> {
            var chunkX = npc.location().chunkX();
            var chunkZ = npc.location().chunkZ();
            // validate that the entity is in the chunk being loaded - Location#getChunk causes a load of the chunk
            return event.getChunkX() == chunkX && event.getChunkZ() == chunkZ;
          })
          .forEach(PlatformSelectorEntity::remove);
      })
    ;

    MinecraftServer.getGlobalEventHandler().addChild(node);
  }

  private void handleClick(@NonNull Player player, int entityId, boolean left) {
    management.trackedEntities().values().stream()
      .filter(npc -> npc.entityId() == entityId)
      .findFirst()
      .ifPresent(entity -> {
        // handle click
        if (left) {
          entity.handleLeftClickAction(player);
        } else {
          entity.handleRightClickAction(player);
        }
      });
  }

}
