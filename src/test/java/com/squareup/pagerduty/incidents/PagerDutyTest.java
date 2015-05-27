/*
 * Copyright (C) 2014 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.pagerduty.incidents;

import org.junit.Test;

import static com.squareup.pagerduty.incidents.EventAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.junit.Assert.fail;

public final class PagerDutyTest {
  private final RecordingEventService service = new RecordingEventService();
  private final PagerDuty pagerDuty = PagerDuty.realPagerDuty("123456", service);

  @Test public void basicTrigger() {
    Trigger trigger = new Trigger.Builder("Paper cut").build();
    pagerDuty.notify(trigger);

    Event event = service.takeEvent();
    assertThat(event).hasServiceKey("123456")
        .hasDescription("Paper cut")
        .hasIncidentKey(null)
        .hasEventType("trigger")
        .hasNoDetails();
  }

  @Test public void triggerWithBellsAndWhistles() {
    Trigger trigger = new Trigger.Builder("Paper cut")
        .withIncidentKey("ouch")
        .addDetails("Location", "Left index finger")
        .addDetails(TestUtil.map("Foo", "Bar", "Kit", "Kat"))
        .addContext(TestUtil.map("type", "link", "href", "https://square.com"))
        .build();
    pagerDuty.notify(trigger);

    Event event = service.takeEvent();
    assertThat(event).hasServiceKey("123456")
        .hasDescription("Paper cut")
        .hasIncidentKey("ouch")
        .hasEventType("trigger")
        .hasDetails(entry("Location", "Left index finger"), entry("Foo", "Bar"),
            entry("Kit", "Kat"))
        .hasContexts(TestUtil.map("type", "link", "href", "https://square.com"));
  }

  @Test public void basicResolve() {
    Resolution resolution = new Resolution.Builder("ouch").build();
    pagerDuty.notify(resolution);

    Event event = service.takeEvent();
    assertThat(event).hasServiceKey("123456")
        .hasIncidentKey("ouch")
        .hasDescription(null)
        .hasEventType("resolve")
        .hasNoDetails();
  }

  @Test public void resolveWithBellsAndWhistles() {
    Resolution resolution = new Resolution.Builder("ouch")
        .withDescription("Band Aid was applied")
        .addDetails("Location", "Left index finger")
        .addDetails(TestUtil.map("Foo", "Bar", "Kit", "Kat"))
        .build();
    pagerDuty.notify(resolution);

    Event event = service.takeEvent();
    assertThat(event).hasServiceKey("123456")
        .hasIncidentKey("ouch")
        .hasEventType("resolve")
        .hasDescription("Band Aid was applied")
        .hasDetails(entry("Location", "Left index finger"), entry("Foo", "Bar"),
            entry("Kit", "Kat"));
  }

  @Test public void apiKeyRequired() {
    try {
      PagerDuty.create(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("apiKey");
    }
    try {
      PagerDuty.create("  ");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("'apiKey' must not be blank. Was: '  '");
    }
  }
}
