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

import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.modules.bridge.platform.PlatformBridgeManagement;
import net.minestom.server.extensions.Extension;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.bungee.BungeeCordProxy;
import net.minestom.server.extras.velocity.VelocityProxy;

public class MinestomBridgeExtension extends Extension {

  @Override
  public void initialize() {
    PlatformBridgeManagement<?, ?> management = new MinestomBridgeManagement(this);
    management.registerServices(Wrapper.instance().serviceRegistry());
    management.postInit();
    // minestom listeners
    new MinestomPlayerManagementListener(this, management);

    // Force Bungeecord Support, if Velocity and Bungeecord aren't enabled
    if (!VelocityProxy.isEnabled() && !BungeeCordProxy.isEnabled()) {
      BungeeCordProxy.enable();
    }

    if (MojangAuth.isEnabled()) {
      LogManager.logger(MinestomBridgeExtension.class).warning("Please disable MojangAuth as it might cause problems with your Proxy!");
    }
  }

  @Override
  public void terminate() {

  }
}
