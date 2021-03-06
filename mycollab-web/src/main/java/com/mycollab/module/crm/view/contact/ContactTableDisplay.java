/**
 * This file is part of mycollab-web.
 *
 * mycollab-web is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mycollab-web is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with mycollab-web.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mycollab.module.crm.view.contact;

import com.mycollab.common.TableViewField;
import com.mycollab.module.crm.CrmTooltipGenerator;
import com.mycollab.module.crm.data.CrmLinkBuilder;
import com.mycollab.module.crm.domain.SimpleContact;
import com.mycollab.module.crm.domain.criteria.ContactSearchCriteria;
import com.mycollab.module.crm.service.ContactService;
import com.mycollab.spring.AppContextUtil;
import com.mycollab.vaadin.AppContext;
import com.mycollab.vaadin.web.ui.CheckBoxDecor;
import com.mycollab.vaadin.web.ui.EmailLink;
import com.mycollab.vaadin.web.ui.LabelLink;
import com.mycollab.vaadin.web.ui.UserLink;
import com.mycollab.vaadin.web.ui.table.DefaultPagedBeanTable;
import com.vaadin.ui.Label;

import java.util.List;

/**
 * @author MyCollab Ltd.
 * @since 1.0
 */
public class ContactTableDisplay extends DefaultPagedBeanTable<ContactService, ContactSearchCriteria, SimpleContact> {
    private static final long serialVersionUID = 1L;

    public ContactTableDisplay(List<TableViewField> displayColumns) {
        this(null, displayColumns);
    }

    public ContactTableDisplay(TableViewField requiredColumn, List<TableViewField> displayColumns) {
        this(null, requiredColumn, displayColumns);

    }

    public ContactTableDisplay(String viewId, TableViewField requiredColumn, List<TableViewField> displayColumns) {
        super(AppContextUtil.getSpringBean(ContactService.class), SimpleContact.class, viewId, requiredColumn, displayColumns);

        addGeneratedColumn("selected", (source, itemId, columnId) -> {
            final SimpleContact contact = getBeanByIndex(itemId);
            final CheckBoxDecor cb = new CheckBoxDecor("", contact.isSelected());
            cb.addValueChangeListener(valueChangeEvent -> {
                fireSelectItemEvent(contact);
                fireTableEvent(new TableClickEvent(ContactTableDisplay.this, contact, "selected"));
            });
            contact.setExtraData(cb);
            return cb;
        });

        addGeneratedColumn("contactName", (source, itemId, columnId) -> {
            final SimpleContact contact = getBeanByIndex(itemId);

            LabelLink b = new LabelLink(contact.getContactName(), CrmLinkBuilder.generateContactPreviewLinkFull(contact.getId()));
            b.setDescription(CrmTooltipGenerator.generateToolTipContact(AppContext.getUserLocale(), AppContext.getDateFormat(),
                    contact, AppContext.getSiteUrl(), AppContext.getUserTimeZone()));
            return b;
        });

        addGeneratedColumn("createdtime", (source, itemId, columnId) -> {
            final SimpleContact contact = getBeanByIndex(itemId);
            return new Label(AppContext.formatDateTime(contact.getCreatedtime()));
        });

        addGeneratedColumn("email", (source, itemId, columnId) -> {
            final SimpleContact contact = getBeanByIndex(itemId);
            return new EmailLink(contact.getEmail());
        });

        addGeneratedColumn("accountName", (source, itemId, columnId) -> {
            final SimpleContact contact = getBeanByIndex(itemId);
            if (contact.getAccountName() != null) {
                return new LabelLink(contact.getAccountName(), CrmLinkBuilder.generateAccountPreviewLinkFull(contact.getAccountid()));
            } else {
                return new Label();
            }
        });

        addGeneratedColumn("birthday", (source, itemId, columnId) -> {
            final SimpleContact contact = getBeanByIndex(itemId);
            return new Label(AppContext.formatDate(contact.getBirthday()));
        });

        addGeneratedColumn("assignUserFullName", (source, itemId, columnId) -> {
            final SimpleContact contact = getBeanByIndex(itemId);
            return new UserLink(contact.getAssignuser(), contact.getAssignUserAvatarId(), contact.getAssignUserFullName());
        });

        this.setWidth("100%");
    }
}
