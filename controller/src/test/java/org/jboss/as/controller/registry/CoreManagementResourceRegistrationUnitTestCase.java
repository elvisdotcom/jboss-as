/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.controller.registry;

import static org.junit.Assert.*;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.junit.Before;
import org.junit.Test;

/**
 * TODO class javadoc.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class CoreManagementResourceRegistrationUnitTestCase {

    private ModelNodeRegistration rootRegistration;
    private final PathElement childElement = PathElement.pathElement("child");
    private final PathAddress childAddress = PathAddress.pathAddress(childElement);

    @Before
    public void setup() {
        rootRegistration = ModelNodeRegistration.Factory.create(new TestDescriptionProvider("RootResource"));
    }

    @Test
    public void testFlagsOnRootResource() throws Exception {

        rootRegistration.registerOperationHandler("one", TestHandler.INSTANCE, new TestDescriptionProvider("one"));
        rootRegistration.registerOperationHandler("two", TestHandler.INSTANCE, new TestDescriptionProvider("two"), false,
                OperationEntry.EntryType.PUBLIC, EnumSet.of(OperationEntry.Flag.READ_ONLY));

        Set<OperationEntry.Flag> oneFlags = rootRegistration.getOperationFlags(PathAddress.EMPTY_ADDRESS, "one");
        assertNotNull(oneFlags);
        assertEquals(0, oneFlags.size());

        Set<OperationEntry.Flag> twoFlags = rootRegistration.getOperationFlags(PathAddress.EMPTY_ADDRESS, "two");
        assertNotNull(twoFlags);
        assertEquals(1, twoFlags.size());
    }

    @Test
    public void testFlagsOnChildResource() throws Exception {

        ModelNodeRegistration child = rootRegistration.registerSubModel(childElement, new TestDescriptionProvider("child"));
        child.registerOperationHandler("one", TestHandler.INSTANCE, new TestDescriptionProvider("one"));
        child.registerOperationHandler("two", TestHandler.INSTANCE, new TestDescriptionProvider("two"), false,
                OperationEntry.EntryType.PUBLIC, EnumSet.of(OperationEntry.Flag.READ_ONLY));

        Set<OperationEntry.Flag> oneFlags = child.getOperationFlags(PathAddress.EMPTY_ADDRESS, "one");
        assertNotNull(oneFlags);
        assertEquals(0, oneFlags.size());

        Set<OperationEntry.Flag> twoFlags = child.getOperationFlags(PathAddress.EMPTY_ADDRESS, "two");
        assertNotNull(twoFlags);
        assertEquals(1, twoFlags.size());

        oneFlags = rootRegistration.getOperationFlags(childAddress, "one");
        assertNotNull(oneFlags);
        assertEquals(0, oneFlags.size());

        twoFlags = rootRegistration.getOperationFlags(childAddress, "two");
        assertNotNull(twoFlags);
        assertEquals(1, twoFlags.size());
    }

    @Test
    public void testFlagsInheritance() throws Exception {

        rootRegistration.registerOperationHandler("one", TestHandler.INSTANCE, new TestDescriptionProvider("one"), true,
                OperationEntry.EntryType.PUBLIC, EnumSet.of(OperationEntry.Flag.READ_ONLY));
        rootRegistration.registerOperationHandler("two", TestHandler.INSTANCE, new TestDescriptionProvider("two"), true,
                OperationEntry.EntryType.PUBLIC, EnumSet.of(OperationEntry.Flag.READ_ONLY));
        rootRegistration.registerOperationHandler("three", TestHandler.INSTANCE, new TestDescriptionProvider("three"), true,
                OperationEntry.EntryType.PUBLIC, EnumSet.of(OperationEntry.Flag.READ_ONLY));
        rootRegistration.registerOperationHandler("four", TestHandler.INSTANCE, new TestDescriptionProvider("four"), false,
                OperationEntry.EntryType.PUBLIC, EnumSet.of(OperationEntry.Flag.READ_ONLY));

        ModelNodeRegistration child = rootRegistration.registerSubModel(childElement, new TestDescriptionProvider("child"));
        child.registerOperationHandler("one", TestHandler.INSTANCE, new TestDescriptionProvider("one"));
        child.registerOperationHandler("two", TestHandler.INSTANCE, new TestDescriptionProvider("two"), false,
                OperationEntry.EntryType.PUBLIC, EnumSet.of(OperationEntry.Flag.DEPLOYMENT_UPLOAD));

        Set<OperationEntry.Flag> oneFlags = child.getOperationFlags(PathAddress.EMPTY_ADDRESS, "one");
        assertNotNull(oneFlags);
        assertEquals(0, oneFlags.size());

        Set<OperationEntry.Flag> twoFlags = child.getOperationFlags(PathAddress.EMPTY_ADDRESS, "two");
        assertNotNull(twoFlags);
        assertEquals(1, twoFlags.size());
        assertTrue(twoFlags.contains(OperationEntry.Flag.DEPLOYMENT_UPLOAD));

        Set<OperationEntry.Flag> threeFlags = child.getOperationFlags(PathAddress.EMPTY_ADDRESS, "three");
        assertNotNull(threeFlags);
        assertEquals(1, threeFlags.size());
        assertTrue(threeFlags.contains(OperationEntry.Flag.READ_ONLY));

        oneFlags = rootRegistration.getOperationFlags(childAddress, "one");
        assertNotNull(oneFlags);
        assertEquals(0, oneFlags.size());

        twoFlags = rootRegistration.getOperationFlags(childAddress, "two");
        assertNotNull(twoFlags);
        assertEquals(1, twoFlags.size());
        assertTrue(twoFlags.contains(OperationEntry.Flag.DEPLOYMENT_UPLOAD));

        threeFlags = child.getOperationFlags(childAddress, "three");
        assertNotNull(threeFlags);
        assertEquals(1, threeFlags.size());
        assertTrue(threeFlags.contains(OperationEntry.Flag.READ_ONLY));

        Set<OperationEntry.Flag> fourFlags = child.getOperationFlags(PathAddress.EMPTY_ADDRESS, "four");
        assertNull(fourFlags);

        fourFlags = rootRegistration.getOperationFlags(childAddress, "four");
        assertNull(fourFlags);

        // Sanity check
        fourFlags = rootRegistration.getOperationFlags(PathAddress.EMPTY_ADDRESS, "four");
        assertNotNull(fourFlags);
        assertEquals(1, fourFlags.size());
        assertTrue(fourFlags.contains(OperationEntry.Flag.READ_ONLY));
    }

    private static class TestHandler implements NewStepHandler {

        private static TestHandler INSTANCE = new TestHandler();

        @Override
        public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
            context.completeStep();
        }
    }

    private static class TestDescriptionProvider implements DescriptionProvider {
        private final String description;

        public TestDescriptionProvider(String description) {
            this.description = description;
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return new ModelNode().set(description);
        }
    }
}
