/*
 * Copyright 2018 the original author or authors.
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
package ratpack.micrometer;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.Status;

import javax.inject.Inject;

class PrometheusHandler implements Handler {
  private final PrometheusMeterRegistry registry;

  @Inject
  public PrometheusHandler(PrometheusMeterRegistry registry) {
    this.registry = registry;
  }

  @Override
  public void handle(Context ctx) {
    ctx.getResponse().contentType(TextFormat.CONTENT_TYPE_004).status(Status.OK).send(registry.scrape());
  }
}
