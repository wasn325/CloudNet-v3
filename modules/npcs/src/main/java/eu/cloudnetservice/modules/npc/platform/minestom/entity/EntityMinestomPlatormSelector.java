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
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.metadata.villager.VillagerMeta;
import net.minestom.server.entity.metadata.villager.VillagerMeta.Profession;
import net.minestom.server.entity.metadata.villager.VillagerMeta.VillagerData;
import net.minestom.server.extensions.Extension;

public class EntityMinestomPlatormSelector extends MinestomPlatformSelectorEntity {

  protected volatile LivingEntity entity;

  public EntityMinestomPlatormSelector(
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
    return this.entity.getUuid().toString();
  }

  @Override
  public boolean spawned() {
    return this.entity != null;
  }

  @Override
  protected void spawn0() {
    var type = EntityType.fromNamespaceId(this.npc.entityType());
    this.entity = new LivingEntity(type);
    //send updates when entity is ready
    this.entity.getEntityMeta().setNotifyAboutChanges(false);
    this.entity.setInstance(this.npcWorld, this.npcLocation);
    this.entity.setCustomName(Component.text(this.npc.displayName()));
    this.entity.setCustomNameVisible(!this.npc.hideEntityName());

    if (type == EntityType.VILLAGER) {
      VillagerMeta meta = (VillagerMeta) this.entity.getEntityMeta();
      VillagerData data = meta.getVillagerData();
      data.setProfession(Profession.FARMER);
      meta.setVillagerData(data);
    }

    var meta = this.entity.getEntityMeta();
    meta.setSilent(true);
    meta.setOnFire(false);
    meta.setNotifyAboutChanges(true);
    // way better than nms :)

  }

  @Override
  protected void remove0() {
    this.entity.remove();
    this.entity = null;
  }

  @Override
  protected void addGlowingEffect() {
    if (this.entity == null) {
      return;
    }
    this.entity.getEntityMeta().setHasGlowingEffect(true);
  }

  @Override
  protected double heightAddition(int lineNumber) {
    var initialAddition = super.heightAddition(lineNumber);
    return (this.entity.getEyeHeight() - (this.entity.getEntityType() == EntityType.WITHER ? 0.4 : 0.55))
      + initialAddition;
  }
}
