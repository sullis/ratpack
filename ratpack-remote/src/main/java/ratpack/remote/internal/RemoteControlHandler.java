/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.remote.internal;

import groovyx.remote.CommandChain;
import groovyx.remote.groovy.server.ContextFactory;
import groovyx.remote.server.Receiver;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.registry.Registry;
import ratpack.registry.RegistryBuilder;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicReference;

import static ratpack.handling.Handlers.*;

public class RemoteControlHandler implements Handler {

  public static final String RESPONSE_CONTENT_TYPE = "application/groovy-remote-control-result";
  public static final String REQUEST_CONTENT_TYPE = "application/groovy-remote-control-command";

  private final Registry registry;
  private final Handler rest;

  private final AtomicReference<Registry> registryReference = new AtomicReference<>();
  private final Handler handler;

  public RemoteControlHandler(String endpointPath, Registry registry, Handler rest) {
    this.registry = registry;
    this.rest = rest;
    this.handler = chain(
      path(
        endpointPath,
        chain(
          post(),
          contentTypes(REQUEST_CONTENT_TYPE),
          accepts(RESPONSE_CONTENT_TYPE),
          new CommandHandler()
        )
      ),
      new InsertHandler()
    );
  }

  private class InsertHandler implements Handler {
    @Override
    public void handle(Context context) throws Exception {
      Registry registryInjection = registryReference.get();
      if (registryInjection == null) {
        rest.handle(context);
      } else {
        context.insert(RegistryBuilder.join(context, registryInjection), rest);
      }
    }
  }

  private class CommandHandler implements Handler {
    @Override
    public void handle(Context context) throws Exception {
      final Registry commandRegistry = RegistryBuilder.join(context, registry);
      final RegistryBuilder registryBuilder = RegistryBuilder.builder();

      Receiver receiver = new RatpackReceiver(new ContextFactory() {
        @Override
        public Object getContext(CommandChain chain) {
          return new DelegatingCommandDelegate(registryBuilder, commandRegistry) {
            @Override
            public void clearRegistry() {
              registryReference.set(null);
            }
          };
        }
      });

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      receiver.execute(context.getRequest().getInputStream(), outputStream);

      Registry newRegistry = registryBuilder.build();
      if (!newRegistry.isEmpty()) {
        registryReference.set(newRegistry);
      }

      context.getResponse().send(RESPONSE_CONTENT_TYPE, outputStream.toByteArray());
    }
  }

  @Override
  public void handle(final Context context) throws Exception {
    handler.handle(context);
  }

}
