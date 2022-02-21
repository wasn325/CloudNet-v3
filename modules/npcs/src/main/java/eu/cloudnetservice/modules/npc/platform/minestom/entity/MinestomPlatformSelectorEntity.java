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

import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.bridge.BridgeServiceHelper;
import eu.cloudnetservice.modules.bridge.BridgeServiceProperties;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.npc.NPC;
import eu.cloudnetservice.modules.npc.configuration.InventoryConfiguration;
import eu.cloudnetservice.modules.npc.configuration.ItemLayout;
import eu.cloudnetservice.modules.npc.platform.PlatformSelectorEntity;
import eu.cloudnetservice.modules.npc.platform.minestom.MinestomPlatformNPCManagement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.hologram.Hologram;
import net.minestom.server.extensions.Extension;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.Nullable;

public abstract class MinestomPlatformSelectorEntity implements
  PlatformSelectorEntity<Pos, Instance, Player, ItemStack, Inventory> {

  protected final NPC npc;
  protected final Extension extension;
  protected final Pos npcLocation;
  protected final Instance npcWorld;
  protected final MinestomPlatformNPCManagement npcManagement;

  protected final UUID uniqueId;
  protected final String scoreboardTeamName;

  protected final Set<Integer> infoLineEntityIds = new HashSet<>();
  protected final Set<InfoLineWrapper> infoLines = new HashSet<>();
  protected final Map<UUID, ServiceItemWrapper> serviceItems = new LinkedHashMap<>();

  protected volatile Inventory inventory;

  protected MinestomPlatformSelectorEntity(
    @NonNull MinestomPlatformNPCManagement npcManagement,
    @NonNull Extension extension,
    @NonNull NPC npc
  ) {
    this.npc = npc;
    this.extension = extension;
    this.npcManagement = npcManagement;
    this.npcLocation = npcManagement.toPlatformLocation(npc.location());
    this.npcWorld = MinecraftServer.getInstanceManager().getInstance(UUID.fromString(npc.location().world()));
    // construct the unique id randomly
    this.uniqueId = new UUID(ThreadLocalRandom.current().nextLong(), 0);
    this.scoreboardTeamName = this.uniqueId.toString().substring(0, 16);
  }

  @Override
  public void spawn() {
    MinecraftServer.getSchedulerManager()
      .scheduleNextTick(() -> {
        // create the inventory view
        this.rebuildInventory(this.npcManagement.inventoryConfiguration());
        // spawn the selector entity
        this.spawn0();

        // Skip this, as every Entity has this function built in
        /*// do the scoreboard related stuff now
        var scoreboard = this.npcManagement.scoreboard();
        // check if a team for the glowing color is already registered
        var team = scoreboard.getTeam(this.scoreboardTeamName);
        if (team == null) {
          team = scoreboard.registerNewTeam(this.scoreboardTeamName);
        }
        // set the name tag visibility of the team
        team.setNameTagVisibility(this.npc.hideEntityName() ? NameTagVisibility.NEVER : NameTagVisibility.ALWAYS);
        // register the spawned entity to the team
        team.addEntry(this.scoreboardRepresentation());
        // check if the entity should have a glowing color
        if (SET_COLOR != null && this.npc.glowing()) {
          // try to set the team color
          var color = ChatColor.getByChar(this.npc.glowingColor());
          if (color != null) {
            try {
              SET_COLOR.invoke(team, color);
            } catch (Throwable throwable) {
              throw new IllegalStateException("Unable to set team color", throwable);
            }
          }
          // let the entity glow!
          this.addGlowingEffect();
        }*/

        // spawn the info lines
        for (var i = npc.infoLines().size() - 1; i >= 0; i--) {
          var armorStand = new Hologram(
            this.npcWorld,
            this.npcLocation.withY(this.heightAddition(i)),
            Component.text("")
          );

          // if it is the top info line try to spawn the item above it
          if (i == npc.infoLines().size() - 1) {
            var materialName = this.npc.floatingItem();
            if (materialName != null) {
              var material = Material.fromNamespaceId("minecraft:" + materialName);
              if (material != null) {
                var item = new ItemEntity(ItemStack.of(material));
                item.setInstance(this.npcWorld, armorStand.getPosition());
                item.setPickable(false);
                item.setMergeable(false);
                // set the passenger
                armorStand.getEntity().addPassenger(item);
              }
            }
          }

          // register the info line
          var wrapper = new InfoLineWrapper(this.npc.infoLines().get(i), armorStand);
          wrapper.rebuildInfoLine();
          // register the line
          this.infoLines.add(wrapper);
          this.infoLineEntityIds.add(armorStand.getEntity().getEntityId());
        }
      });
  }

  @Override
  public void remove() {
    this.doRemove();
  }

  protected void doRemove() {
    for (var infoLine : this.infoLines) {
      // remove the armor stand passenger (if any)
      // remove the info line armor stand
      infoLine.armorStand.remove();
    }
    this.infoLines.clear();
    this.infoLineEntityIds.clear();
    // remove the actual selector npc
    this.remove0();
  }

  @Override
  public void update() {
    // rebuild all items - we can do that async
    this.serviceItems.values().forEach(wrapper -> this.trackService(wrapper.service()));
    // rebuild everything else sync
    MinecraftServer.getSchedulerManager()
      .scheduleNextTick(() -> {
        this.rebuildInventory(this.npcManagement.inventoryConfiguration());
        this.rebuildInfoLines();
      });
  }

  @Override
  public void trackService(@NonNull ServiceInfoSnapshot service) {
    // get the current item
    var wrapper = this.serviceItems.get(service.serviceId().uniqueId());
    // build the item for the service
    var configuration = this.npcManagement.inventoryConfiguration();
    var layouts = configuration.getHolder(service.configuration().groups().toArray(new String[0]));
    // get the service state
    var state = BridgeServiceHelper.guessStateFromServiceInfoSnapshot(service);
    ItemLayout layout;
    switch (state) {
      case EMPTY_ONLINE:
        layout = layouts.emptyLayout();
        break;
      case FULL_ONLINE:
        if (configuration.showFullServices()) {
          layout = layouts.fullLayout();
          break;
        } else {
          return;
        }
      case ONLINE:
        layout = layouts.onlineLayout();
        break;
      default:
        return;
    }
    // build the item stack from the layout
    var item = this.buildItemStack(layout, service);
    if (item != null) {
      if (wrapper == null) {
        // store a new wrapper
        this.serviceItems.put(service.serviceId().uniqueId(), new ServiceItemWrapper(item, service));
      } else {
        // update the item wrapper
        wrapper.itemStack(item);
        wrapper.service(service);
      }
      // push the service update
      this.rebuildInfoLines();
      this.rebuildInventory(configuration);
    } else if (wrapper != null) {
      // unable to build a new item - remove the current one
      this.serviceItems.remove(service.serviceId().uniqueId());
    }
  }

  @Override
  public void stopTrackingService(@NonNull ServiceInfoSnapshot service) {
    MinecraftServer.getSchedulerManager()
      .scheduleNextTick(() -> {
        // get the old item wrapper
        var wrapper = this.serviceItems.remove(service.serviceId().uniqueId());
        if (wrapper != null) {
          // the service got tracked before - rebuild the inventory and info lines
          this.rebuildInfoLines();
          this.rebuildInventory(this.npcManagement.inventoryConfiguration());
        }
      });
  }

  @Override
  public void handleLeftClickAction(@NonNull Player player) {
    this.handleClickAction(player, this.npc.leftClickAction());
  }

  @Override
  public void executeAction(@NonNull Player player, @NonNull NPC.ClickAction action) {
    this.handleClickAction(player, action);
  }

  @Override
  public void handleRightClickAction(@NonNull Player player) {
    this.handleClickAction(player, this.npc.rightClickAction());
  }

  @Override
  public void handleInventoryInteract(@NonNull Inventory inv, @NonNull Player player, @NonNull ItemStack clickedItem) {
    // find the server associated with the clicked item
    for (var wrapper : this.serviceItems.values()) {
      if (wrapper.itemStack().equals(clickedItem)) {
        // close the inventory
        player.closeInventory();
        // connect the player
        this.playerManager().playerExecutor(player.getUuid()).connect(wrapper.service().name());
        break;
      }
    }
  }

  @Override
  public @NonNull Inventory selectorInventory() {
    return this.inventory;
  }

  @Override
  public @NonNull Set<Integer> infoLineEntityIds() {
    return this.infoLineEntityIds;
  }

  @Override
  public @NonNull NPC npc() {
    return this.npc;
  }

  @Override
  public @NonNull Pos location() {
    return this.npcLocation;
  }

  @Override
  public @NonNull Instance world() {
    return this.npcWorld;
  }

  @Override
  public boolean canSpawn() {
    return this.npcWorld != null && this.npcWorld.isChunkLoaded(this.npcLocation);
  }

  protected void handleClickAction(@NonNull Player player, @NonNull NPC.ClickAction action) {
    LogManager.logger("Test").info("handling npc action " + action + "for player " + player.getName());
    switch (action) {
      case OPEN_INVENTORY -> player.openInventory(this.inventory);
      case DIRECT_CONNECT_RANDOM -> {
        List<ServiceItemWrapper> wrappers = new ArrayList<>(this.serviceItems.values());
        // connect the player to the first element if present
        if (!wrappers.isEmpty()) {
          var wrapper = wrappers.get(ThreadLocalRandom.current().nextInt(0, wrappers.size()));
          this.playerManager().playerExecutor(player.getUuid()).connect(wrapper.service().name());
        }
      }
      case DIRECT_CONNECT_LOWEST_PLAYERS -> this.serviceItems.values().stream()
        .map(ServiceItemWrapper::service)
        .min(Comparator.comparingInt(service -> BridgeServiceProperties.ONLINE_COUNT.read(service).orElse(0)))
        .ifPresent(ser -> this.playerManager().playerExecutor(player.getUuid()).connect(ser.name()));
      case DIRECT_CONNECT_HIGHEST_PLAYERS -> this.serviceItems.values().stream()
        .map(ServiceItemWrapper::service)
        .max(Comparator.comparingInt(service -> BridgeServiceProperties.ONLINE_COUNT.read(service).orElse(0)))
        .ifPresent(ser -> this.playerManager().playerExecutor(player.getUuid()).connect(ser.name()));
      default -> { }
    }
  }

  protected @Nullable ItemStack buildItemStack(@NonNull ItemLayout layout, @Nullable ServiceInfoSnapshot service) {
    var material = Material.fromNamespaceId(NamespaceID.from("minecraft", layout.material()));
    if (material != null) {
      var item = ItemStack.of(material);
      var meta = item.getMeta().with(builder ->
        builder.displayName(Component.text(
            BridgeServiceHelper.fillCommonPlaceholders(
              layout.displayName(),
              this.npc.targetGroup(),
              service
            )
          ))
          .lore(layout.lore().stream()
            .map(line -> BridgeServiceHelper.fillCommonPlaceholders(line, this.npc.targetGroup(), service))
            .map(Component::text)
            .collect(Collectors.toList())
          ));
      // return the item with the modified meta
      return item.withMeta(meta);
    }
    return null;
  }

  protected void rebuildInfoLines() {
    this.infoLines.forEach(InfoLineWrapper::rebuildInfoLine);
  }

  protected void rebuildInventory(@NonNull InventoryConfiguration configuration) {
    // calculate the inventory size
    var inventorySize = configuration.inventorySize();
    if (configuration.dynamicSize()) {
      inventorySize = this.serviceItems.size();
      // try to make it to the next higher possible inventory size
      while (inventorySize == 0 || (inventorySize < 54 && inventorySize % 9 != 0)) {
        inventorySize++;
      }
    }
    // create the inventory
    if (this.inventory == null || this.inventory.getSize() != inventorySize) {
      this.inventory = new Inventory(findBySize(inventorySize), this.npc.displayName());
    }
    // remove all current contents
    this.inventory.clear();
    // add the fixed items
    for (var entry : configuration.fixedItems().entrySet()) {
      // check if the item would exceed the inventory size
      if (entry.getKey() < inventorySize) {
        // build and set the item
        var item = this.buildItemStack(entry.getValue(), null);
        if (item != null) {
          this.inventory.setItemStack(entry.getKey(), item);
        }
      }
    }
    // add the service items
    for (var wrapper : this.serviceItems.values()) {
      if (!this.inventory.addItemStack(wrapper.itemStack())) {
        // the inventory is full
        break;
      }
    }
  }

  private InventoryType findBySize(int size) {
    return Arrays.stream(InventoryType.values())
      .filter(type -> type.getSize() == size)
      .findFirst()
      .orElse(InventoryType.CHEST_6_ROW);
  }

  protected @NonNull PlayerManager playerManager() {
    return CloudNetDriver.instance().servicesRegistry().firstService(PlayerManager.class);
  }

  protected double heightAddition(int lineNumber) {
    var entry = this.npcManagement.applicableNPCConfigurationEntry();
    return entry == null ? lineNumber : entry.infoLineDistance() * lineNumber;
  }

  protected abstract void spawn0();

  protected abstract void remove0();

  protected abstract void addGlowingEffect();

  protected static final class ServiceItemWrapper {

    private volatile ItemStack itemStack;
    private volatile ServiceInfoSnapshot serviceInfoSnapshot;

    public ServiceItemWrapper(@NonNull ItemStack itemStack, @NonNull ServiceInfoSnapshot serviceInfoSnapshot) {
      this.itemStack = itemStack;
      this.serviceInfoSnapshot = serviceInfoSnapshot;
    }

    public @NonNull ItemStack itemStack() {
      return this.itemStack;
    }

    public void itemStack(@NonNull ItemStack itemStack) {
      this.itemStack = itemStack;
    }

    public @NonNull ServiceInfoSnapshot service() {
      return this.serviceInfoSnapshot;
    }

    public void service(@NonNull ServiceInfoSnapshot serviceInfoSnapshot) {
      this.serviceInfoSnapshot = serviceInfoSnapshot;
    }
  }

  protected final class InfoLineWrapper {

    private final String basedInfoLine;
    private final Hologram armorStand;

    public InfoLineWrapper(String basedInfoLine, Hologram armorStand) {
      this.basedInfoLine = basedInfoLine;
      this.armorStand = armorStand;
    }

    private void rebuildInfoLine() {
      var npc = MinestomPlatformSelectorEntity.this.npc;
      // update based on the tracked services
      var tracked = MinestomPlatformSelectorEntity.this.serviceItems.values();
      // general info
      var onlinePlayers = Integer.toString(tracked.stream()
        .map(MinestomPlatformSelectorEntity.ServiceItemWrapper::service)
        .mapToInt(snapshot -> BridgeServiceProperties.ONLINE_COUNT.read(snapshot).orElse(0))
        .sum());
      var maxPlayers = Integer.toString(tracked.stream()
        .map(MinestomPlatformSelectorEntity.ServiceItemWrapper::service)
        .mapToInt(snapshot -> BridgeServiceProperties.MAX_PLAYERS.read(snapshot).orElse(0))
        .sum());
      var onlineServers = Integer.toString(tracked.size());
      // rebuild the info line
      var newInfoLine = this.basedInfoLine
        .replace("%group%", npc.targetGroup()).replace("%g%", npc.targetGroup())
        .replace("%online_players%", onlinePlayers).replace("%o_p%", onlinePlayers)
        .replace("%max_players%", maxPlayers).replace("%m_p%", maxPlayers)
        .replace("%online_servers%", onlineServers).replace("%o_s%", onlineServers);
      // set the custom name of the armor stand
      this.armorStand.setText(Component.text(newInfoLine));
    }
  }

}
