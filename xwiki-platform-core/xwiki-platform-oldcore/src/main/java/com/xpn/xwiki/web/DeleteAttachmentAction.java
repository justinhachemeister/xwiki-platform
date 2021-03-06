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
package com.xpn.xwiki.web;

import javax.script.ScriptContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.xwiki.model.EntityType;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceManager;
import org.xwiki.resource.entity.EntityResourceReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Delete attachment xwiki action.
 *
 * @version $Id$
 */
public class DeleteAttachmentAction extends XWikiAction
{
    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        // CSRF prevention
        if (!csrfTokenCheck(context)) {
            return false;
        }

        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        XWikiAttachment attachment = null;
        XWiki xwiki = context.getWiki();
        String filename;

        // Delete from the trash
        if (request.getParameter("trashId") != null) {
            long trashId = NumberUtils.toLong(request.getParameter("trashId"));
            DeletedAttachment da = xwiki.getAttachmentRecycleBinStore().getDeletedAttachment(trashId, context, true);
            // If the attachment hasn't been previously deleted (i.e. it's not in the deleted attachment store) then
            // don't try to delete it and instead redirect to the attachment list.
            if (da != null) {
                com.xpn.xwiki.api.DeletedAttachment daapi = new com.xpn.xwiki.api.DeletedAttachment(da, context);
                if (!daapi.canDelete()) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS,
                        XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                        "You are not allowed to delete an attachment from the trash "
                            + "immediately after it has been deleted from the wiki");
                }
                if (!da.getDocName().equals(doc.getFullName())) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                        XWikiException.ERROR_XWIKI_APP_URL_EXCEPTION,
                        "The specified trash entry does not match the current document");
                }
                // TODO: Add a confirmation check
                xwiki.getAttachmentRecycleBinStore().deleteFromRecycleBin(trashId, context, true);
            }
            sendRedirect(response, Utils.getRedirect("attach", context));
            return false;
        }

        if (context.getMode() == XWikiContext.MODE_PORTLET) {
            filename = request.getParameter("filename");
        } else {
            // Note: We use getRequestURI() because the spec says the server doesn't decode it, as
            // we want to use our own decoding.
            String requestUri = request.getRequestURI();
            filename = getFileName();
        }

        XWikiDocument newdoc = doc.clone();

        // An attachment can be indicated either using an id, or using the filename.
        if (request.getParameter("id") != null) {
            int id = NumberUtils.toInt(request.getParameter("id"));
            if (newdoc.getAttachmentList().size() > id) {
                attachment = newdoc.getAttachmentList().get(id);
            }
        } else {
            attachment = newdoc.getAttachment(filename);
        }

        // No such attachment
        if (attachment == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);

            ScriptContext scriptContext = getCurrentScriptContext();
            if (scriptContext != null) {
                scriptContext.setAttribute("message",
                    localizePlainOrKey("core.action.deleteAttachment.failed", filename), ScriptContext.ENGINE_SCOPE);
                scriptContext.setAttribute("details",
                    localizePlainOrKey("platform.core.action.deleteAttachment.noAttachment"),
                    ScriptContext.ENGINE_SCOPE);
            }

            return true;
        }

        newdoc.setAuthorReference(context.getUserReference());

        // Set "deleted attachment" as the version comment.
        String comment;
        if (attachment.isImage(context)) {
            comment = localizePlainOrKey("core.comment.deleteImageComment", filename);
        } else {
            comment = localizePlainOrKey("core.comment.deleteAttachmentComment", filename);
        }

        try {
            newdoc.removeAttachment(attachment);
            xwiki.saveDocument(newdoc, comment, context);
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            ScriptContext scriptContext = getCurrentScriptContext();
            if (scriptContext != null) {
                scriptContext.setAttribute("message",
                    localizePlainOrKey("core.action.deleteAttachment.failed", filename), ScriptContext.ENGINE_SCOPE);
                scriptContext.setAttribute("details", ExceptionUtils.getRootCauseMessage(ex),
                    ScriptContext.ENGINE_SCOPE);
            }

            return true;
        }

        // forward to attach page
        if (!((Boolean) context.get("ajax")).booleanValue()) {
            String redirect = Utils.getRedirect("attach", context);
            sendRedirect(response, redirect);
        }

        return false;
    }

    @Override
    public String render(XWikiContext context)
    {
        return "error";
    }

    /**
     * @return the filename of the attachment.
     */
    private String getFileName()
    {
        // Extract the Attachment file name from the parsed request URL that was done before this Action is called
        ResourceReference resourceReference = Utils.getComponent(ResourceReferenceManager.class).getResourceReference();
        EntityResourceReference entityResource = (EntityResourceReference) resourceReference;
        return entityResource.getEntityReference().extractReference(EntityType.ATTACHMENT).getName();
    }
}
