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

package eu.cloudnetservice.modules.npc.platform.minestom.command;

import com.github.juliarn.npc.profile.Profile;
import com.google.common.base.Enums;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;
import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.modules.npc.NPC;
import eu.cloudnetservice.modules.npc.NPC.ClickAction;
import eu.cloudnetservice.modules.npc.NPC.NPCType;
import eu.cloudnetservice.modules.npc.NPC.ProfileProperty;
import eu.cloudnetservice.modules.npc.platform.PlatformSelectorEntity;
import eu.cloudnetservice.modules.npc.platform.minestom.MinestomPlatformNPCManagement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.NonNull;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.command.builder.arguments.ArgumentStringArray;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.registry.ArgumentEntityType;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.extensions.Extension;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NPCCommand extends Command {

  private static final @NotNull Collection<@NotNull Material> MATERIALS = Material.values();

  private static final List<String> TRUE_FALSE = Arrays.asList("true", "yes", "y", "false", "no", "n");

  private static final List<String> NPC_TYPES = Arrays.stream(NPCType.values())
    .map(Enum::name)
    .toList();
  private static final List<String> CLICK_ACTIONS = Arrays.stream(ClickAction.values())
    .map(Enum::name)
    .toList();
  private static final Map<String, Integer> VALID_ITEM_SLOTS = ImmutableMap.<String, Integer>builder()
    .put("MAIN_HAND", 0)
    .put("OFF_HAND", 1)
    .put("BOOTS", 2)
    .put("LEGGINS", 3)
    .put("CHESTPLATE", 4)
    .put("HELMET", 5)
    .build();

  private static final String COPIED_NPC_KEY = "npc_copy_entry";

  private static final Tag<String> builderTag = Tag.String(COPIED_NPC_KEY);

  private final Extension extension;
  private final MinestomPlatformNPCManagement management;

  public NPCCommand(@NonNull Extension extension, @NonNull MinestomPlatformNPCManagement management) {
    super("npc", "cn", "cloudnpc");
    this.extension = extension;
    this.management = management;

    setCondition(Conditions::playerOnly);
    setDefaultExecutor(this::sendHelp);

    Command create = new Command("create");
    ArgumentString target = ArgumentType.String("target");
    ArgumentString owner = ArgumentType.String("skinOwnerName");
    ArgumentEntityType mobType = ArgumentType.EntityType("entityType");
    ArgumentStringArray display = ArgumentType.StringArray("displayName");
    create.setDefaultExecutor(this::sendHelp);
    create.addSyntax(this::createNPC, target, mobType, display);
    create.addSyntax(this::createNPC, target, owner, display);
    addSubcommand(create);

    Command remove = new Command("remove", "rm");
    remove.setDefaultExecutor(this::sendHelp);
    remove.addSyntax(this::removeNPC);
    addSubcommand(remove);

    Command clean = new Command("cleanup", "cu");
    clean.setDefaultExecutor(this::sendHelp);
    clean.addSyntax(this::cleanNPC);
    addSubcommand(clean);

    Command copy = new Command("cp", "copy");
    copy.setDefaultExecutor(this::sendHelp);
    copy.addSyntax(this::copyNPC);
    addSubcommand(copy);

    Command ccp = new Command("ccp", "clearclipboard");
    ccp.setDefaultExecutor(this::sendHelp);
    ccp.addSyntax((sender, context) -> {
      sender.removeTag(builderTag);
      sender.sendMessage("§7Your clipboard was cleared §asuccessfully§7.");
    });
    addSubcommand(ccp);

    Command cut = new Command("cut");
    cut.setDefaultExecutor(this::sendHelp);
    cut.addSyntax(this::cutNPC);
    addSubcommand(cut);

    Command paste = new Command("paste");
    paste.setDefaultExecutor(this::sendHelp);
    paste.addSyntax(this::pasteNPC);
    addSubcommand(paste);

    Command list = new Command("list");
    list.setDefaultExecutor(this::sendHelp);
    list.addSyntax(this::listNPC);
    addSubcommand(list);

    ArgumentString action = ArgumentType.String("action");
    ArgumentString value = ArgumentType.String("value");
    Command edit = new Command("edit");
    edit.setDefaultExecutor(this::sendHelp);
    edit.addSyntax(this::editNPC, action, value);
    addSubcommand(edit);
  }

  private void sendHelp(CommandSender sender, CommandContext context) {
    sender.sendMessage("§8> §7/cn create <targetGroup> <skinOwnerName> <displayName>");
    sender.sendMessage("§8> §7/cn create <targetGroup> <entityType> <displayName>");
    sender.sendMessage("§8> §7/cn edit <option> <value...>");
    sender.sendMessage("§8> §7/cn remove");
    sender.sendMessage("§8> §7/cn cleanup");
    sender.sendMessage("§8> §7/cn list");
    sender.sendMessage("§8> §7/cn <copy/cut/paste>");
  }

  private void createNPC(CommandSender sender, CommandContext context) {

    var entry = this.management.applicableNPCConfigurationEntry();
    if (entry == null) {
      sender.sendMessage("§cThere is no applicable npc configuration entry for this service (yet)!");
      return;
    }

    String target = context.get("target");
    String[] display = context.get("displayName");
    final String displayName = String.join(" ", display).length() <= 16 ? String.join(" ", display) : "Name_Too_Long";
    if (context.has("skinOwnerName")) {
      var skin = PlayerSkin.fromUsername(context.get("skinOwnerName"));
      var npc = NPC.builder()
        .profileProperties(
          Collections.singleton(new ProfileProperty("skin", skin.textures(), skin.signature()))
        )
        .displayName(displayName)
        .targetGroup(target)
        .location(this.management.toWorldPosition(((Player) sender).getPosition(), ((Player) sender).getInstance(),
          entry.targetGroup()));
      this.management.createNPC(npc.build());
    } else {
      EntityType type = context.get("entityType");
      var npc = NPC.builder()
        .entityType(type.name())
        .displayName(displayName)
        .targetGroup(target)
        .location(this.management.toWorldPosition(((Player) sender).getPosition(), ((Player) sender).getInstance(),
          entry.targetGroup()));
      this.management.createNPC(npc.build());
    }

    // done :)
    sender.sendMessage("§7The service selector mob was created §asuccessfully§7!");
  }

  private void removeNPC(CommandSender sender, CommandContext context) {
    var player = (Player) sender; //We can safely cast the sender as the commands only allow players
    var npc = this.getNearestNPC(player.getPosition(), Objects.requireNonNull(player.getInstance()));
    if (npc == null) {
      sender.sendMessage("§cNo npc in range found! Make sure the npc you want to edit is in a 5 block radius.");
      return;
    }
    // remove the npc
    this.management.deleteNPC(npc);
    sender.sendMessage("§cThe npc was removed successfully! This may take a few seconds to show effect!");
  }

  private void cleanNPC(CommandSender sender, CommandContext context) {
    this.management.trackedEntities().values().stream()
      .map(PlatformSelectorEntity::npc)
      .filter(npc -> MinecraftServer.getInstanceManager().getInstance(UUID.fromString(npc.location().world())) == null)
      .forEach(npc -> {
        this.management.deleteNPC(npc);
        sender.sendMessage(String.format(
          "§cAn entity in the world §6%s §cwas removed! This may take a few seconds to show effect!",
          npc.location().world()));
      });
  }

  private void copyNPC(CommandSender sender, CommandContext context) {
    var player = (Player) sender;
    var npc = this.getNearestNPC(player.getPosition(), Objects.requireNonNull(player.getInstance()));
    if (npc == null) {
      sender.sendMessage("§cNo npc in range found! Make sure the npc you want to edit is in a 5 block radius.");
      return;
    }
    // check if the player has already a npc in the clipboard
    if (!player.hasTag(builderTag)) {
      player.setTag(builderTag, JsonDocument.newDocument(NPC.builder(npc)).toString());
      // player.setMetadata(COPIED_NPC_KEY, new FixedMetadataValue(this.plugin, NPC.builder(npc)));
      sender.sendMessage("§7The npc was copied §asuccessfully §7to your clipboard");
    } else {
      sender.sendMessage("§cThere is a npc already in your clipboard! Paste it or clear your clipboard.");
    }
  }

  private void cutNPC(CommandSender sender, CommandContext context) {
    var player = (Player) sender;
    var npc = this.getNearestNPC(player.getPosition(), Objects.requireNonNull(player.getInstance()));
    if (npc == null) {
      sender.sendMessage("§cNo npc in range found! Make sure the npc you want to edit is in a 5 block radius.");
      return;
    }
    // check if the player has already a npc in the clipboard
    if (!player.hasTag(builderTag)) {
      // remove the npc
      this.management.deleteNPC(npc);
      // add the metadata
      player.setTag(builderTag, JsonDocument.newDocument(NPC.builder(npc)).toString());
      sender.sendMessage("§7The npc was cut §asuccessfully §7to your clipboard");
    } else {
      sender.sendMessage("§cThere is a npc already in your clipboard! Paste it or clear your clipboard.");
    }
  }

  private void pasteNPC(CommandSender sender, CommandContext context) {
    var player = (Player) sender;
    if (!player.hasTag(builderTag)) {
      sender.sendMessage("§cThere is no npc in your clipboard!");
      return;
    }
    var values = player.getTag(builderTag);

    var entry = this.management.applicableNPCConfigurationEntry();
    if (entry == null) {
      sender.sendMessage("§cThere is no applicable npc configuration entry for this service (yet)!");
      return;
    }

    // paste the npc
    var npc = JsonDocument.newDocument(values).toInstanceOf(NPC.Builder.class)
      .location(this.management.toWorldPosition(player.getPosition(), player.getInstance(), entry.targetGroup()))
      .build();
    this.management.createNPC(npc);
    sender.sendMessage("§7The service selector mob was pasted §asuccessfully§7!");
    // clear the clipboard
    player.removeTag(builderTag);
  }

  private void listNPC(CommandSender sender, CommandContext context) {
    sender.sendMessage(String.format("§7There are §6%s §7selector mobs:", this.management.npcs().size()));
    for (var npc : this.management.npcs()) {
      sender.sendMessage(String.format(
        "§8> §6\"%s\" §8@ §7%s§8/§7%s §8- §7%d, %d, %d in \"%s\"",
        npc.displayName(),
        npc.npcType(),
        npc.npcType() == NPCType.ENTITY ? npc.entityType() : "props: " + npc.profileProperties().size(),
        (int) npc.location().x(),
        (int) npc.location().y(),
        (int) npc.location().z(),
        npc.location().world()));
    }
  }

  private void editNPC(CommandSender sender, CommandContext context) {
    var player = (Player) sender;
    var npc = this.getNearestNPC(player.getPosition(), Objects.requireNonNull(player.getInstance()));
    if (npc == null) {
      sender.sendMessage("§cNo npc in range found! Make sure the npc you want to edit is in a 5 block radius.");
      return;
    }
    NPC updatedNpc;
    String value = context.get("value");

    switch ((String)context.get("action")) {
      case "display" -> {
        var displayName = value;
        if (displayName.length() > 16) {
          sender.sendMessage("§cThe display name can only contain up to 16 chars.");
          return;
        }
        // re-create the npc with the given options
        updatedNpc = NPC.builder(npc)
          .displayName(this.translateAlternateColorCodes('&', displayName))
          .build();
      }

      // enable that the npc looks at the player
      case "lap", "lookatplayer" -> {
        if (this.canChangeSetting(sender, npc)) {
          updatedNpc = NPC.builder(npc).lookAtPlayer(this.parseBoolean(value)).build();
        } else {
          return;
        }
      }

      // if the npc should imitate the player
      case "ip", "imitateplayer" -> {
        if (this.canChangeSetting(sender, npc)) {
          updatedNpc = NPC.builder(npc).imitatePlayer(this.parseBoolean(value)).build();
        } else {
          return;
        }
      }

      // if the npc should use the skin of the player being spawned to
      case "ups", "useplayerskin" -> {
        if (this.canChangeSetting(sender, npc)) {
          updatedNpc = NPC.builder(npc).usePlayerSkin(this.parseBoolean(value)).build();
        } else {
          return;
        }
      }

      // sets the glowing color
      // Currently skip this
      /*case "gc", "glowingcolor" -> {
        // try to parse the color
        var chatColor = Color;
        if (chatColor == null) {
          sender.sendMessage(String.format(
            "§cNo such chat color char §6%s§c! Use one of §8[§60-9§8, §6a-f§8, §6r§8]§c.",
            value));
          return;
        }
        // validate the color
        if (chatColor.isFormat()) {
          sender.sendMessage("§cPlease use a color char, not a chat formatting char!");
          return;
        }
        // disable glowing if the color is reset
        if (chatColor == ChatColor.RESET) {
          updatedNpc = NPC.builder(npc).glowing(false).build();
        } else {
          updatedNpc = NPC.builder(npc).glowing(true).glowingColor(String.valueOf(chatColor.getChar())).build();
        }
      }*/

      // set if the npc name tag should be hidden
      case "hen", "hideentityname" -> {
        updatedNpc = NPC.builder(npc).hideEntityName(this.parseBoolean(value)).build();
      }

      // sets if the npc should "fly" with an elytra
      case "fwe", "flyingwithelytra" -> {
        if (this.canChangeSetting(sender, npc)) {
          var enabled = this.parseBoolean(value);
          updatedNpc = NPC.builder(npc).flyingWithElytra(enabled).build();
          // warn about weird behaviour in combination with other settings
          if (enabled) {
            sender.sendMessage("§cEnabling elytra-flying might lead to weird-looking behaviour when imitate "
              + "and lookAt player is enabled! Consider disabling these options.");
          }
        } else {
          return;
        }
      }

      // the floating item of the npc
      case "fi", "floatingitem" -> {
        // convert null to "no item"
        if (value.equalsIgnoreCase("null")) {
          updatedNpc = NPC.builder(npc).floatingItem(null).build();
          break;
        }
        // get the material of the item
        var material = org.bukkit.Material.matchMaterial(value);
        if (material == null) {
          sender.sendMessage(String.format("§cNo material found by query: §6%s§c.", value));
          return;
        } else {
          updatedNpc = NPC.builder(npc).floatingItem(material.name()).build();
        }
      }

      // the left click action
      case "lca", "leftclickaction" -> {
        var action = Enums.getIfPresent(ClickAction.class, value.toUpperCase()).orNull();
        if (action == null) {
          sender.sendMessage(String.format(
            "§cNo such click action. Use one of: §6%s§c.",
            String.join(", ", CLICK_ACTIONS)));
          return;
        } else {
          updatedNpc = NPC.builder(npc).leftClickAction(action).build();
        }
      }

      // the right click action
      case "rca", "rightclickaction" -> {
        var action = Enums.getIfPresent(ClickAction.class, value.toUpperCase()).orNull();
        if (action == null) {
          sender.sendMessage(String.format(
            "§cNo such click action. Use one of: §6%s§c.",
            String.join(", ", CLICK_ACTIONS)));
          return;
        } else {
          updatedNpc = NPC.builder(npc).rightClickAction(action).build();
        }
      }

      // sets the items
      case "items" -> {
        if (value.length() != 4) {
          sender.sendMessage("§cInvalid usage! Use §6/cn edit items <slot> <material>§c!");
          return;
        }
        // parse the slot
        var slot = VALID_ITEM_SLOTS.get(value.toUpperCase());
        if (slot == null) {
          sender.sendMessage(String.format(
            "§cNo such item slot! Use one of §6%s§7.",
            String.join(", ", VALID_ITEM_SLOTS.keySet())));
          return;
        }
        // parse the item
        var item = org.bukkit.Material.matchMaterial(value);
        if (item == null) {
          sender.sendMessage("§cNo such material!");
          return;
        }
        // a little hack here :)
        npc.items().put(slot, item.name());
        updatedNpc = npc;
      }

      // edit the info lines
      case "il", "infolines" -> {
        if (value.length() < 4) { //TODO Array
          sender.sendMessage("§cInvalid usage! Use §6/cn edit il <index> <new line content>§c!");
          return;
        }
        // parse the index
        var index = Ints.tryParse(value);
        if (index == null) {
          sender.sendMessage(String.format("§cUnable to parse index from string §6%s§c.", value));
          return;
        }
        // get the new line content
        var content = value; //TODO Array
        if (content.equals("null")) {
          // remove the info line if there
          if (npc.infoLines().size() > index) {
            npc.infoLines().remove((int) index);
            updatedNpc = npc;
          } else {
            sender.sendMessage(String.format("§cNo info line at index §6%d§c.", index));
            return;
          }
        } else {
          content = translateAlternateColorCodes('&', content);
          // set the info line add the location or add it
          if (npc.infoLines().size() > index) {
            npc.infoLines().set(index, content);
          } else {
            npc.infoLines().add(content);
          }
          updatedNpc = npc;
        }
      }

      // change the profile (will force-set the entity type to npc)
      case "profile" -> {
        var profile = new Profile(value);
        if (!profile.complete()) {
          sender.sendMessage(String.format("§cUnable to complete profile of §6%s§c!", value));
          return;
        } else {
          updatedNpc = NPC.builder(npc).profileProperties(profile.getProperties().stream()
              .map(property -> new ProfileProperty(property.getName(), property.getValue(), property.getSignature()))
              .collect(Collectors.toSet()))
            .build();
        }
      }

      // change the entity type (will force-set the entity type to entity)
      case "et", "entitytype" -> {
        var entityType = Enums.getIfPresent(org.bukkit.entity.EntityType.class, value.toUpperCase()).orNull();
        if (entityType == null) {
          sender.sendMessage(String.format("§cNo such entity type: §6%s§c.", value.toUpperCase()));
          return;
        } else {
          updatedNpc = NPC.builder(npc).entityType(entityType.name()).build();
        }
      }

      // sets the target group of the npc
      case "tg", "targetgroup" -> updatedNpc = NPC.builder(npc).targetGroup(value).build();

      // unknown option
      default -> {
        sender.sendMessage(String.format("§cNo option with name §6%s §cfound!", ((String) context.get("action")).toLowerCase()));
        return;
      }
    }

    // update & notify
    this.management.createNPC(updatedNpc);
    sender.sendMessage(String.format(
      "§7The option §6%s §7was updated §asuccessfully§7! It may take a few seconds for the change to become visible.",
      ((String) context.get("action")).toLowerCase()));

  }

  private @Nullable NPC getNearestNPC(@NonNull Pos location, @NonNull Instance instance) {
    return this.management.trackedEntities().values().stream()
      .filter(PlatformSelectorEntity::spawned)
      .filter(entity -> entity.world().getUniqueId().equals(instance.getUniqueId()))
      .filter(entity -> entity.location().distanceSquared(location) <= 10)
      .min(Comparator.comparingDouble(entity -> entity.location().distanceSquared(location)))
      .map(PlatformSelectorEntity::npc)
      .orElse(null);
  }

  private boolean parseBoolean(@NonNull String input) {
    return input.contains("true") || input.contains("yes") || input.startsWith("y");
  }

  private boolean canChangeSetting(@NonNull CommandSender sender, @NonNull NPC npc) {
    if (npc.npcType() != NPCType.PLAYER) {
      sender.sendMessage(String.format("§cThis option is not available for the npc type §6%s§c!", npc.entityType()));
      return false;
    }
    return true;
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
