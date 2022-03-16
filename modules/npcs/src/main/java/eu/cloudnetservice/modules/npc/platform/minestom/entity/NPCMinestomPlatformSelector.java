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

package eu.cloudnetservice.modules.npc.platform.minestom.entity;

import eu.cloudnetservice.modules.npc.NPC;
import eu.cloudnetservice.modules.npc.platform.minestom.MinestomPlatformNPCManagement;
import lombok.NonNull;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player.Hand;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.fakeplayer.FakePlayer;
import net.minestom.server.extensions.Extension;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.timer.TaskSchedule;

public class NPCMinestomPlatformSelector extends MinestomPlatformSelectorEntity {

  protected volatile FakePlayer entity;

  public NPCMinestomPlatformSelector(
    @NonNull MinestomPlatformNPCManagement npcManagement,
    @NonNull Extension extension, @NonNull NPC npc) {
    super(npcManagement, extension, npc);
  }


  @Override
  public int entityId() {
    return this.entity == null ? -1 : this.entity.getEntityId();
  }

  @Override
  public @NonNull String scoreboardRepresentation() {
    return this.entity.getUsername();
  }

  @Override
  public boolean spawned() {
    return this.entity != null;
  }

  @Override
  protected void spawn0() {
    FakePlayer.initPlayer(
      this.uniqueId,
      this.npc.displayName(),
      (player) -> {
        this.entity = player;
        this.entity.teleport(this.npcManagement.toPlatformLocation(this.npc.location()));
        this.npc.profileProperties()
          .forEach(prop -> this.entity.setSkin(new PlayerSkin(prop.value(), prop.signature())));
        if (this.npc.lookAtPlayer()) {
          MinecraftServer.getSchedulerManager().scheduleTask(() -> {
            this.npcWorld.getNearbyEntities(this.npcLocation, 10.0)
              .stream()
              .filter(e -> e.getEntityType() == EntityType.PLAYER)
              .findFirst().ifPresent(entity -> this.entity.lookAt(entity));
          }, TaskSchedule.millis(0), TaskSchedule.millis(20));
        }
        this.entity.getController().sendArmAnimation(Hand.MAIN); //Test this if needed
        this.entity.setGlowing(this.npc.glowing());
        this.entity.setFlyingWithElytra(this.npc.flyingWithElytra());

        for (var entry : this.npc.items().entrySet()) {
          var material = Material.fromNamespaceId("minecraft:" + entry.getValue());
          if (material != null) {
            switch (entry.getKey()) {
              case 0:
                this.entity.setItemInMainHand(ItemStack.of(material));
                break;
              case 1:
                this.entity.setItemInOffHand(ItemStack.of(material));
                break;
              case 2:
                this.entity.setBoots(ItemStack.of(material));
                break;
              case 3:
                this.entity.setLeggings(ItemStack.of(material));
                break;
              case 4:
                this.entity.setChestplate(ItemStack.of(material));
                break;
              case 5:
                this.entity.setHelmet(ItemStack.of(material));
                break;
              default:
                break;
            }
          }
        }
      }
    );
  }


  @Override
  protected void remove0() {
    this.entity.remove();
    this.entity = null;
  }

  @Override
  protected void addGlowingEffect() {

  }
}
