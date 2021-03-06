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
 */
package org.xwiki.model.reference;

import org.xwiki.model.EntityType;
import org.xwiki.stability.Unstable;

/**
 * References a property in a class in a page (the description of the property).
 * 
 * @version $Id$
 * @since 10.6RC1
 */
@Unstable
public class PageClassPropertyReference extends EntityReference
{
    /**
     * Constructor which would raise exceptions if the source entity reference does not have the appropriate type or
     * parent, etc.
     * 
     * @param reference the raw reference to build this object reference from
     */
    public PageClassPropertyReference(EntityReference reference)
    {
        super(reference);
    }

    /**
     * Clone an ClassPropertyReference, but replace one of the parent in the chain by a new one.
     *
     * @param reference the reference that is cloned
     * @param oldReference the old parent that will be replaced
     * @param newReference the new parent that will replace oldReference in the chain
     */
    protected PageClassPropertyReference(EntityReference reference, EntityReference oldReference,
        EntityReference newReference)
    {
        super(reference, oldReference, newReference);
    }

    /**
     * Builds a property reference for the passed property in the passed object.
     * 
     * @param propertyName the name of the property to create reference for
     * @param classReference the reference to the class whose property is
     */
    public PageClassPropertyReference(String propertyName, PageReference classReference)
    {
        super(propertyName, EntityType.PAGE_CLASS_PROPERTY, classReference);
    }

    /**
     * Clone an PageClassPropertyReference, but use the specified parent for its new parent.
     *
     * @param reference the reference to clone
     * @param parent the new parent to use
     * @since 10.8RC1
     */
    public PageClassPropertyReference(EntityReference reference, EntityReference parent)
    {
        super(reference, parent);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to check the type to be a property type.
     * 
     * @see org.xwiki.model.reference.EntityReference#setType(org.xwiki.model.EntityType)
     */
    @Override
    protected void setType(EntityType type)
    {
        if (type != EntityType.PAGE_CLASS_PROPERTY) {
            throw new IllegalArgumentException("Invalid type [" + type + "] for a class property reference");
        }

        super.setType(type);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to ensure that the parent of a property is always an object.
     * </p>
     * 
     * @see org.xwiki.model.reference.EntityReference#setParent(org.xwiki.model.reference.EntityReference)
     */
    @Override
    protected void setParent(EntityReference parent)
    {
        if (parent instanceof PageReference) {
            super.setParent(parent);

            return;
        }

        if (parent == null || parent.getType() != EntityType.PAGE) {
            throw new IllegalArgumentException(
                "Invalid parent reference [" + parent + "] in a class property reference");
        }

        super.setParent(new PageReference(parent));
    }

    @Override
    public PageClassPropertyReference replaceParent(EntityReference oldParent, EntityReference newParent)
    {
        if (newParent == oldParent) {
            return this;
        }

        return new PageClassPropertyReference(this, oldParent, newParent);
    }

    @Override
    public PageClassPropertyReference replaceParent(EntityReference newParent)
    {
        if (newParent == getParent()) {
            return this;
        }

        return new PageClassPropertyReference(this, newParent);
    }
}
