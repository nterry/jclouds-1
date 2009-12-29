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
package org.jclouds.vcloud.terremark.xml;

import static org.testng.Assert.assertEquals;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;

import org.jclouds.http.functions.BaseHandlerTest;
import org.jclouds.http.functions.ParseSax;
import org.jclouds.http.functions.config.ParserModule;
import org.jclouds.vcloud.VCloudMediaType;
import org.jclouds.vcloud.domain.ResourceAllocation;
import org.jclouds.vcloud.domain.ResourceType;
import org.jclouds.vcloud.domain.VAppStatus;
import org.jclouds.vcloud.domain.internal.NamedResourceImpl;
import org.jclouds.vcloud.endpoints.internal.VAppRoot;
import org.jclouds.vcloud.terremark.domain.TerremarkVApp;
import org.jclouds.vcloud.terremark.domain.TerremarkVirtualSystem;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Provides;
import com.google.inject.internal.Iterables;

/**
 * Tests behavior of {@code TerremarkVAppHandler}
 * 
 * @author Adrian Cole
 */
@Test(groups = "unit", testName = "vcloud.TerremarkVAppHandlerTest")
public class TerremarkVAppHandlerTest extends BaseHandlerTest {

   @BeforeTest
   @Override
   protected void setUpInjector() {
      injector = Guice.createInjector(new ParserModule(), new AbstractModule() {

         @Override
         protected void configure() {
         }

         @SuppressWarnings("unused")
         @Provides
         @VAppRoot
         String provide() {
            return "https://services.vcloudexpress.terremark.com/api/v0.8/vapp";
         }

      });
      factory = injector.getInstance(ParseSax.Factory.class);
   }

   public void testApplyInputStream() {
      InputStream is = getClass().getResourceAsStream("/terremark/launched_vapp.xml");

      TerremarkVApp result = (TerremarkVApp) factory.create(
               injector.getInstance(TerremarkVAppHandler.class)).parse(is);
      assertEquals(result.getId(), 13775 + "");

      assertEquals(result.getName(), "adriantest");
      assertEquals(result.getStatus(), VAppStatus.UNRESOLVED);

      assertEquals(result.getSize(), new Long(4));

      assertEquals(result.getLocation(), URI
               .create("https://services.vcloudexpress.terremark.com/api/v0.8/vapp/13775"));
      assertEquals(result.getVDC(), new NamedResourceImpl("32", null,
               "application/vnd.vmware.vcloud.vdc+xml", URI
                        .create("https://services.vcloudexpress.terremark.com/api/v0.8/vdc/32")));

   }

   public void testGetVApp() throws UnknownHostException {
      InputStream is = getClass().getResourceAsStream("/terremark/get_vapp.xml");

      TerremarkVApp result = (TerremarkVApp) factory.create(
               injector.getInstance(TerremarkVAppHandler.class)).parse(is);
      assertEquals(result.getId(), 13850 + "");

      assertEquals(result.getName(), "adriantest1");
      assertEquals(result.getStatus(), VAppStatus.OFF);

      assertEquals(result.getSize().longValue(), 4194304l);
      assertEquals(result.getOperatingSystemDescription(), "Ubuntu Linux (32-bit)");

      assertEquals(result.getLocation(), URI
               .create("https://services.vcloudexpress.terremark.com/api/v0.8/vapp/13850"));
      assertEquals(result.getVDC(), new NamedResourceImpl("32", null, VCloudMediaType.VDC_XML, URI
               .create("https://services.vcloudexpress.terremark.com/api/v0.8/vdc/32")));

      assertEquals(result.getSystem(), new TerremarkVirtualSystem(null, null, null, null, null,
               null, null, null, null, null, null, "Virtual Hardware Family", 0, null, null, null,
               null, null, "adriantest1", "vmx-07"));
      assertEquals(result.getNetworkToAddresses().get("Internal"), ImmutableList.of(InetAddress
               .getByName("10.114.34.132")));

      ResourceAllocation cpu = new ResourceAllocation(1, "1 virtual CPU(s)",
               "Number of Virtual CPUs", ResourceType.PROCESSOR, null, null, null, null, null,
               null, 1, "hertz * 10^6");

      ResourceAllocation controller = new ResourceAllocation(3, "SCSI Controller 0",
               "SCSI Controller", ResourceType.SCSI_CONTROLLER, "lsilogic", null, 0, null, null,
               null, 1, null);
      ResourceAllocation memory = new ResourceAllocation(2, "512MB of memory", "Memory Size",
               ResourceType.MEMORY, null, null, null, null, null, null, 512, "byte * 2^20");
      ResourceAllocation disk = new ResourceAllocation(9, "Hard Disk 1", null,
               ResourceType.DISK_DRIVE, null, "4194304", null, 0, 3, null, 4194304, null);
      assertEquals(result.getResourceAllocations(), ImmutableSortedSet.of(cpu, controller, memory,
               disk));
      assertEquals(Iterables.getOnlyElement(
               result.getResourceAllocationByType().get(ResourceType.PROCESSOR))
               .getVirtualQuantity(), 1);
      assertEquals(Iterables.getOnlyElement(
               result.getResourceAllocationByType().get(ResourceType.SCSI_CONTROLLER))
               .getVirtualQuantity(), 1);
      assertEquals(Iterables.getOnlyElement(
               result.getResourceAllocationByType().get(ResourceType.MEMORY)).getVirtualQuantity(),
               512);
      assertEquals(Iterables.getOnlyElement(
               result.getResourceAllocationByType().get(ResourceType.DISK_DRIVE))
               .getVirtualQuantity(), 4194304);
      assertEquals(result.getSize().longValue(), Iterables.getOnlyElement(
               result.getResourceAllocationByType().get(ResourceType.DISK_DRIVE))
               .getVirtualQuantity());
   }

   public void testGetVApp2disks() throws UnknownHostException {
      InputStream is = getClass().getResourceAsStream("/terremark/get_vapp2disks.xml");

      TerremarkVApp vApp = (TerremarkVApp) factory.create(
               injector.getInstance(TerremarkVAppHandler.class)).parse(is);
      assertEquals(vApp.getId(), 15639 + "");

      assertEquals(vApp.getName(), "eduardo");
      assertEquals(vApp.getStatus(), VAppStatus.OFF);

      assertEquals(vApp.getSize().longValue(), 30408704);
      assertEquals(vApp.getOperatingSystemDescription(), "Ubuntu Linux (32-bit)");

      assertEquals(vApp.getLocation(), URI
               .create("https://services.vcloudexpress.terremark.com/api/v0.8/vapp/15639"));
      assertEquals(vApp.getVDC(), new NamedResourceImpl("32", null, VCloudMediaType.VDC_XML, URI
               .create("https://services.vcloudexpress.terremark.com/api/v0.8/vdc/32")));

      assertEquals(vApp.getSystem(), new TerremarkVirtualSystem(null, null, null, null, null,
               null, null, null, null, null, null, "Virtual Hardware Family", 0, null, null, null,
               null, null, "eduardo", "vmx-07"));
      assertEquals(vApp.getNetworkToAddresses().get("Internal"), ImmutableList.of(InetAddress
               .getByName("10.114.34.131")));

      ResourceAllocation cpu = new ResourceAllocation(1, "2 virtual CPU(s)",
               "Number of Virtual CPUs", ResourceType.PROCESSOR, null, null, null, null, null,
               null, 2, "hertz * 10^6");

      ResourceAllocation controller = new ResourceAllocation(3, "SCSI Controller 0",
               "SCSI Controller", ResourceType.SCSI_CONTROLLER, "lsilogic", null, 0, null, null,
               null, 1, null);
      ResourceAllocation memory = new ResourceAllocation(2, "1024MB of memory", "Memory Size",
               ResourceType.MEMORY, null, null, null, null, null, null, 1024, "byte * 2^20");
      ResourceAllocation disk = new ResourceAllocation(9, "Hard Disk 1", null,
               ResourceType.DISK_DRIVE, null, "4194304", null, 0, 3, null, 4194304, null);
      ResourceAllocation disk2 = new ResourceAllocation(9, "Hard Disk 2", null,
               ResourceType.DISK_DRIVE, null, "26214400", null, 1, 3, null, 26214400, null);

      assertEquals(vApp.getResourceAllocations(), ImmutableSortedSet.of(cpu, controller, memory,
               disk, disk2));
      assertEquals(Iterables.getOnlyElement(
               vApp.getResourceAllocationByType().get(ResourceType.PROCESSOR))
               .getVirtualQuantity(), 2);
      assertEquals(Iterables.getOnlyElement(
               vApp.getResourceAllocationByType().get(ResourceType.SCSI_CONTROLLER))
               .getVirtualQuantity(), 1);
      assertEquals(Iterables.getOnlyElement(
               vApp.getResourceAllocationByType().get(ResourceType.MEMORY)).getVirtualQuantity(),
               1024);
      
      // extract the disks on the vApp sorted by addressOnParent
      List<ResourceAllocation> disks = Lists.newArrayList(vApp.getResourceAllocationByType()
               .get(ResourceType.DISK_DRIVE));

      assertEquals(disks.get(0).getVirtualQuantity(), 4194304);
      assertEquals(disks.get(1).getVirtualQuantity(), 26214400);

   }

}