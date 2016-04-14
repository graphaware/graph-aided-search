/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.integration.es.util;

import com.graphaware.integration.es.domain.ClauseConstants;
import static org.junit.Assert.*;
import java.lang.reflect.*;
import org.junit.Test;

public class ClauseConstantsTest {
    
    @Test
    public void testConstants() {
        try {
            assertUtilityClassWellDefined(ClauseConstants.class);
        } catch (NoSuchMethodException ex) {
            fail("NoSuchMethodException" + ex.getMessage());
        } catch (InvocationTargetException ex) {
            fail("InvocationTargetException" + ex.getMessage());
        } catch (InstantiationException ex) {
            fail("InstantiationException" + ex.getMessage());
        } catch (IllegalAccessException ex) {
            fail("IllegalAccessException" + ex.getMessage());
        }
    }

    public static void assertUtilityClassWellDefined(Class clazz)
            throws NoSuchMethodException, InvocationTargetException,
            InstantiationException, IllegalAccessException {
        assertTrue("class must be public",
                Modifier.isPublic(clazz.getModifiers()));
        assertTrue("class must be final",
                Modifier.isFinal(clazz.getModifiers()));
        assertEquals("There must be only one constructor", 1,
                clazz.getDeclaredConstructors().length);
        final Constructor constructor = clazz.getDeclaredConstructor();
        if (constructor.isAccessible() || !Modifier.isPrivate(constructor.getModifiers())) {
            fail("constructor is not private");
        }
        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);
        for (final Method method : clazz.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers())
                    && method.getDeclaringClass().equals(clazz)) {
                fail("there exists a non-static method:" + method);
            }
        }
    }
}
