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

package eu.cloudnetservice.modules.npc.platform.minestom;

import eu.cloudnetservice.modules.npc.platform.minestom.command.NPCCommand;
import eu.cloudnetservice.modules.npc.platform.minestom.listener.MinestomEntityProtectionListener;
import net.minestom.server.MinecraftServer;
import net.minestom.server.extensions.Extension;

public class MinestomNPCExtension extends Extension {

  @Override
  public void initialize() {

    // init the npc management
    var managemant = new MinestomPlatformNPCManagement(this);
    managemant.registerToServiceRegistry();
    managemant.initialize();

    // register all listeners
    new MinestomEntityProtectionListener(managemant);
    // register the commands
    MinecraftServer.getCommandManager().register(new NPCCommand(this, managemant));
  }

  @Override
  public void terminate() {

  }
}
