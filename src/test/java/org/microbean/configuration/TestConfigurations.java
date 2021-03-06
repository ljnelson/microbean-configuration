/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2017-2018 microBean.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.microbean.configuration;

import java.io.Serializable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import org.microbean.configuration.api.AmbiguousConfigurationValuesException;
import org.microbean.configuration.api.ConfigurationValue;
import org.microbean.configuration.api.TypeLiteral;

import org.microbean.configuration.spi.AbstractConfiguration;
import org.microbean.configuration.spi.Configuration;
import org.microbean.configuration.spi.ConfigurationCoordinates;
import org.microbean.configuration.spi.SystemPropertiesConfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import static org.junit.Assume.assumeNotNull;

public class TestConfigurations {

  private Configurations configurations;
  
  public TestConfigurations() {
    super();
  }

  @Before
  public void setUpConfigurations() {
    final Map<String, String> westRegionalCoordinates = new HashMap<>();
    westRegionalCoordinates.put("region", "west");
    final Properties westRegionalProperties = new Properties();
    westRegionalProperties.put("db.url", "jdbc:west");
    final Configuration westRegionalConfiguration = new PropertiesConfiguration(westRegionalCoordinates, westRegionalProperties);

    final Map<String, String> westRegionalAndTestEnvironmentCoordinates = new HashMap<>();
    westRegionalAndTestEnvironmentCoordinates.put("region", "west");
    westRegionalAndTestEnvironmentCoordinates.put("environment", "test");
    final Properties westRegionalAndTestEnvironmentProperties = new Properties();
    westRegionalAndTestEnvironmentProperties.put("db.url", "jdbc:west:test");
    final Configuration westRegionalAndTestEnvironmentConfiguration = new PropertiesConfiguration(westRegionalAndTestEnvironmentCoordinates, westRegionalAndTestEnvironmentProperties);

    final Map<String, String> westRegionalAndTestEnvironmentAndExperimentalPhaseCoordinates = new HashMap<>();
    westRegionalAndTestEnvironmentAndExperimentalPhaseCoordinates.put("region", "west");
    westRegionalAndTestEnvironmentAndExperimentalPhaseCoordinates.put("environment", "test");
    westRegionalAndTestEnvironmentAndExperimentalPhaseCoordinates.put("phase", "experimental");
    final Properties westRegionalAndTestEnvironmentAndExperimentalPhaseProperties = new Properties();
    westRegionalAndTestEnvironmentAndExperimentalPhaseProperties.put("db.url", "jdbc:west:test:experimental");
    final Configuration westRegionalAndTestEnvironmentAndExperimentalPhaseConfiguration = new PropertiesConfiguration(westRegionalAndTestEnvironmentAndExperimentalPhaseCoordinates, westRegionalAndTestEnvironmentAndExperimentalPhaseProperties);

    final Map<String, String> experimentalPhaseCoordinates = new HashMap<>();
    experimentalPhaseCoordinates.put("phase", "experimental");
    final Properties experimentalPhaseProperties = new Properties();
    experimentalPhaseProperties.put("db.url", "jdbc:experimental");
    final Configuration experimentalPhaseConfiguration = new PropertiesConfiguration(experimentalPhaseCoordinates, experimentalPhaseProperties);

    final Map<String, String> experimentalPhaseAndTestEnvironmentCoordinates = new HashMap<>();
    experimentalPhaseAndTestEnvironmentCoordinates.put("phase", "experimental");
    experimentalPhaseAndTestEnvironmentCoordinates.put("environment", "test");
    final Properties experimentalPhaseAndTestEnvironmentProperties = new Properties();
    experimentalPhaseAndTestEnvironmentProperties.put("db.url", "jdbc:experimental:test");
    final Configuration experimentalPhaseAndTestEnvironmentConfiguration = new PropertiesConfiguration(experimentalPhaseAndTestEnvironmentCoordinates, experimentalPhaseAndTestEnvironmentProperties);

    final Map<String, String> testEnvironmentCoordinates = new HashMap<>();
    testEnvironmentCoordinates.put("environment", "test");
    final Properties testEnvironmentProperties = new Properties();
    testEnvironmentProperties.put("db.url", "jdbc:test");
    final Configuration testEnvironmentConfiguration = new PropertiesConfiguration(testEnvironmentCoordinates, testEnvironmentProperties);

    final Set<Configuration> subConfigurations = new HashSet<>();
    subConfigurations.add(westRegionalConfiguration);
    subConfigurations.add(westRegionalAndTestEnvironmentConfiguration);
    subConfigurations.add(westRegionalAndTestEnvironmentAndExperimentalPhaseConfiguration);
    subConfigurations.add(experimentalPhaseConfiguration);
    subConfigurations.add(experimentalPhaseAndTestEnvironmentConfiguration);
    subConfigurations.add(testEnvironmentConfiguration);
    subConfigurations.add(new SystemPropertiesConfiguration());
    subConfigurations.add(new ConfigurationCoordinates());
    this.configurations = new Configurations(subConfigurations, null, null);
  }


  /*
   * Test methods.
   */
  

  /**
   * Tests {@link Configurations} behavior when there is an almost
   * absurdly simple exact match.
   */
  @Test
  public void testFirstSpike() {
    final String value = this.configurations.getValue("java.vendor");
    assertEquals(System.getProperty("java.vendor"), value);
  }

  /**
   * Tests {@link Configurations} behavior when technically speaking
   * there is not an exact match (the caller has {@code
   * {environment=test}} as its coordinates), but there is a suitable
   * one.
   */
  @Test
  public void testWithCoordinates() {
    final Map<String, String> coordinates = new HashMap<>();
    coordinates.put("environment", "test");
    final String value = this.configurations.getValue(coordinates, "java.vendor");
    assertEquals(System.getProperty("java.vendor"), value);
  }

  @Test
  public void testMoreComplicatedSuitableMatch() {
    final Map<String, String> coordinates = new HashMap<>();
    coordinates.put("environment", "test");
    coordinates.put("phase", "experimental");
    final String value = this.configurations.getValue(coordinates, "db.url");
    assertEquals("jdbc:experimental:test", value);    
  }

  @Test(expected = AmbiguousConfigurationValuesException.class)
  public void testErrorCondition1() {
    final Map<String, String> coordinates = new HashMap<>();
    coordinates.put("region", "west");
    coordinates.put("phase", "experimental");
    final String value = this.configurations.getValue(coordinates, "db.url"); // should fail
  }

  @Test
  public void testAcquireCoordinates() {
    System.setProperty("configurationCoordinates", "{a=b,c=d}");
    final Map<String, String> coordinates = this.configurations.getValue("configurationCoordinates", new TypeLiteral<Map<String, String>>() {
        private static final long serialVersionUID = 1L; }.getType());
    assertNotNull(coordinates);
    assertEquals(2, coordinates.size());
    assertEquals("b", coordinates.get("a"));
    assertEquals("d", coordinates.get("c"));
    System.clearProperty("configurationCoordinates");
  }

  @Test
  public void testElExpansion() {
    assertEquals("a " + System.getProperty("java.home") + " c", this.configurations.interpolate("a ${configurations[\"java.home\"]} c"));
  }

  @Test
  public void testArbitration() {
    assumeNotNull(System.getenv("PATH"));
    String value = new Configurations().getValue("PATH");
    assertEquals(System.getenv("PATH"), value);
    assertNull(System.getProperty("PATH"));
    System.setProperty("PATH", "me first");
    value = new Configurations().getValue("PATH");
    assertEquals("me first", value);
  }


  /*
   * Inner and nested classes.
   */

  
  public static final class PropertiesConfiguration extends AbstractConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private final Map<String, String> coordinates;
    
    private final Properties properties;
    
    public PropertiesConfiguration(final Map<String, String> coordinates, final Properties properties) {
      super();
      this.coordinates = coordinates == null ? Collections.emptyMap() : coordinates;
      this.properties = properties == null ? new Properties() : properties;
    }

    public Properties getProperties() {
      return this.properties;
    }

    @Override
    public ConfigurationValue getValue(final Map<String, String> applicationCoordinates, final String name) {
      final Properties properties = this.getProperties();
      assert properties != null;
      final Set<String> stringPropertyNames = properties.stringPropertyNames();
      assert stringPropertyNames != null;
      if (stringPropertyNames.contains(name)) {
        return new ConfigurationValue(this, this.coordinates, name, properties.getProperty(name), false);
      } else {
        return null;
      }
    }

    @Override
    public Set<String> getNames() {
      final Properties properties = this.getProperties();
      assert properties != null;
      return Collections.unmodifiableSet(properties.stringPropertyNames());
    }
    
  }

}
