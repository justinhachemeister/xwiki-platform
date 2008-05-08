/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
 *
 */
package org.xwiki.velocity.introspection;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.util.ClassUtils;
import org.apache.velocity.util.RuntimeServicesAware;
import org.apache.velocity.util.introspection.Uberspect;
import org.apache.velocity.util.introspection.UberspectLoggable;

/**
 * <p>
 * Since the current version of the Velocity engine (1.5) does not allow chaining uberspectors, this
 * class is a workaround. It manually constructs the uberspectors chain, loading the classes in the
 * order defined in the <code>"runtime.introspector.uberspect.chainClasses"</code> property and
 * after that simply forwarding all calls to the top of the chain. Note that the calls will be made
 * from the rightmost class to the leftmost one.
 * </p>
 * <p>
 * This is not actually part of the chain, but is more of a handle that allows the calls intended
 * for only one uberspector to reach the chain. It duplicates some of the code from the velocity
 * runtime initialization code, hoping that a future version of the engine will support chaining
 * natively.
 * </p>
 * <p>
 * The chain is defined using the configuration parameter
 * <code>runtime.introspector.uberspect.chainClasses</code>. This property should contain a list
 * of canonical class names. Any wrong entry in the list will be ignored. If this property is not
 * defined or contains only wrong classnames, then by default a <code>SecureUberspector</code> is
 * used as the only entry in the chain. The first (leftmost) uberspector must not be chainable (as
 * it will not need to forward calls). If a uberspector in the middle of the chain is not chainable,
 * then it will break the chain at that point (all previos uberspectors will be discarded from the
 * chain).
 * </p>
 * 
 * @since 1.5M1
 * @see ChainableUberspector
 */
public class ChainingUberspector extends ChainableUberspectorBase implements Uberspect,
    RuntimeServicesAware, UberspectLoggable
{
    /** The runtime is needed for accessing the configuration. */
    RuntimeServices runtime;

    /** The key of the parameter that allows defining the list of chained uberspectors. */
    public static final String UBERSPECT_CHAIN_CLASSNAMES =
        "runtime.introspector.uberspect.chainClasses";

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.velocity.util.RuntimeServicesAware#setRuntimeServices(org.apache.velocity.runtime.RuntimeServices)
     */
    public void setRuntimeServices(RuntimeServices rs)
    {
        this.runtime = rs;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation initializes the uberspector chain.
     * </p>
     * 
     * @see org.apache.velocity.util.introspection.UberspectImpl#init()
     */
    @Override
    public void init()
    {
        log.debug("Initializing the chaining uberspector.");
        // Create the chain
        // TODO Since we're in Plexus already, should we use components?
        String[] chainClassnames =
            runtime.getConfiguration().getStringArray(UBERSPECT_CHAIN_CLASSNAMES);
        for (String classname : chainClassnames) {
            initializeUberspector(classname);
        }
        // If the chain is empty, use a SecureUberspector
        if (inner == null) {
            log.error("No chained uberspectors defined! "
                + "This uberspector is just a placeholder that relies on a real uberspector "
                + "to actually allow method calls. Using SecureUberspect.");
            initializeUberspector("org.apache.velocity.util.introspection.SecureUberspector");
        }
        // Initialize all the uberspectors in the chain
        try {
            inner.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Instantiates an uberspector class and adds it to the chain. No other initialization (except
     * chaining and what is present in the constructor) is done.
     * 
     * @param classname The name of the uberspector class to add to the chain.
     */
    protected void initializeUberspector(String classname)
    {
        // Avoids direct recursive calls
        if (!StringUtils.isEmpty(classname)
            && !classname.equals(this.getClass().getCanonicalName())) {
            Object o = null;

            try {
                o = ClassUtils.getNewInstance(classname);
            } catch (ClassNotFoundException cnfe) {
                log.error("The specified class for Uberspect [" + classname
                    + "] does not exist or is not accessible to the current classloader.");
            } catch (IllegalAccessException e) {
                log.error("The specified class for Uberspect [" + classname
                    + "] does not have a public default constructor.");
            } catch (InstantiationException e) {
                log.error("The specified class for Uberspect [" + classname
                    + "] cannot be instantiated.");
            } catch (ExceptionInInitializerError e) {
                log.error("Exception while instantiating the Uberspector [" + classname + "]: "
                    + e.getMessage());
            }

            if (!(o instanceof Uberspect)) {
                if (o != null) {
                    log.error("The specified class for Uberspect [" + classname
                        + "] does not implement " + Uberspect.class.getName());
                }
                return;
            }

            Uberspect u = (Uberspect) o;

            // Set the log and runtime services, if applicable
            if (u instanceof UberspectLoggable) {
                ((UberspectLoggable) u).setLog(log);
            }
            if (u instanceof RuntimeServicesAware) {
                ((RuntimeServicesAware) u).setRuntimeServices(runtime);
            }

            // Link it in the chain
            if (u instanceof ChainableUberspector) {
                ((ChainableUberspector) u).wrap(inner);
            }
            inner = u;
        }
    }
}
