/**
 *
 * Copyright (C) 2009 Cloud Conscious, LLC. <info@cloudconscious.com>
 *
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 */
package org.jclouds.vcloud;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.jclouds.compute.domain.Image;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.ssh.jsch.config.JschSshClientModule;
import org.jclouds.vcloud.domain.ResourceType;
import org.jclouds.vcloud.domain.VApp;
import org.jclouds.vcloud.domain.VAppStatus;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

import com.google.common.base.CaseFormat;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.internal.ImmutableMap;

/**
 * Tests behavior of {@code HostingDotComVCloudClient}
 * 
 * @author Adrian Cole
 */
@Test(groups = "live", sequential = true, testName = "vcloud.HostingDotComVCloudClientLiveTest")
public class VCloudComputeClientLiveTest {
   VCloudComputeClient client;
   VCloudClient tmClient;

   private String id;
   private InetAddress privateAddress;

   public static final String PREFIX = System.getProperty("user.name") + "-terremark";

   private static class Expectation {
      final long hardDisk;
      final String os;

      public Expectation(long hardDisk, String os) {
         this.hardDisk = hardDisk;
         this.os = os;
      }
   }

   private Map<Image, Expectation> expectationMap = ImmutableMap.<Image, Expectation> builder()
            .put(Image.CENTOS_53,
                     new Expectation(4194304 / 2 * 10, "Red Hat Enterprise Linux 5 (64-bit)")).put(
                     Image.RHEL_53,
                     new Expectation(4194304 / 2 * 10, "Red Hat Enterprise Linux 5 (64-bit)")).put(
                     Image.UMBUNTU_90, new Expectation(4194304, "Ubuntu Linux (64-bit)")).put(
                     Image.UMBUNTU_JEOS, new Expectation(4194304, "Ubuntu Linux (32-bit)")).build();

   private Predicate<InetAddress> addressTester;

   @Test
   public void testPowerOn() throws InterruptedException, ExecutionException, TimeoutException,
            IOException {
      Image toTest = Image.CENTOS_53;

      String serverName = getCompatibleServerName(toTest);
      int processorCount = 1;
      int memory = 512;
      long disk = 10 * 1025 * 1024;
      Map<String, String> properties = ImmutableMap.of("foo", "bar");

      id = client.start(serverName, toTest, processorCount, memory, disk, properties).get("id");
      Expectation expectation = expectationMap.get(toTest);

      VApp vApp = tmClient.getVApp(id);
      verifyConfigurationOfVApp(vApp, serverName, expectation.os, processorCount, memory,
               expectation.hardDisk);
      assertEquals(vApp.getStatus(), VAppStatus.ON);
   }

   private String getCompatibleServerName(Image toTest) {
      String serverName = CaseFormat.UPPER_UNDERSCORE
               .to(CaseFormat.LOWER_HYPHEN, toTest.toString()).substring(0,
                        toTest.toString().length() <= 15 ? toTest.toString().length() : 14);
      return serverName;
   }

   @Test(dependsOnMethods = "testPowerOn")
   public void testGetAnyPrivateAddress() {
      privateAddress = client.getAnyPrivateAddress(id);
      assert !addressTester.apply(privateAddress);
   }

   private void verifyConfigurationOfVApp(VApp vApp, String serverName, String expectedOs,
            int processorCount, int memory, long hardDisk) {
      // assertEquals(vApp.getName(), serverName);
      // assertEquals(vApp.getOperatingSystemDescription(), expectedOs);
      assertEquals(
               Iterables.getOnlyElement(
                        vApp.getResourceAllocationByType().get(ResourceType.PROCESSOR))
                        .getVirtualQuantity(), processorCount);
      assertEquals(Iterables.getOnlyElement(
               vApp.getResourceAllocationByType().get(ResourceType.SCSI_CONTROLLER))
               .getVirtualQuantity(), 1);
      assertEquals(Iterables.getOnlyElement(
               vApp.getResourceAllocationByType().get(ResourceType.MEMORY)).getVirtualQuantity(),
               memory);
      assertEquals(Iterables.getOnlyElement(
               vApp.getResourceAllocationByType().get(ResourceType.DISK_DRIVE))
               .getVirtualQuantity(), hardDisk);
   }

   @AfterTest
   void cleanup() throws InterruptedException, ExecutionException, TimeoutException {
      if (id != null)
         client.stop(id);
   }

   @BeforeGroups(groups = { "live" })
   public void setupClient() {
      String endpoint = checkNotNull(System.getProperty("jclouds.test.endpoint"),
               "jclouds.test.endpoint");
      String account = checkNotNull(System.getProperty("jclouds.test.user"), "jclouds.test.user");
      String key = checkNotNull(System.getProperty("jclouds.test.key"), "jclouds.test.key");
      Injector injector = new VCloudContextBuilder(new VCloudPropertiesBuilder(
               URI.create(endpoint), account, key).relaxSSLHostname().build()).withModules(
               new Log4JLoggingModule(), new JschSshClientModule()).buildInjector();
      client = injector.getInstance(VCloudComputeClient.class);
      tmClient = injector.getInstance(VCloudClient.class);
      addressTester = injector.getInstance(Key.get(new TypeLiteral<Predicate<InetAddress>>() {
      }));
   }

}