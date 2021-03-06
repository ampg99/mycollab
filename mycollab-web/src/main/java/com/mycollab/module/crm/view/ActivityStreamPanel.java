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

package com.mycollab.module.crm.view;

import com.hp.gagawa.java.elements.A;
import com.hp.gagawa.java.elements.Img;
import com.hp.gagawa.java.elements.Text;
import com.mycollab.common.ActivityStreamConstants;
import com.mycollab.common.ModuleNameConstants;
import com.mycollab.common.domain.SimpleActivityStream;
import com.mycollab.common.domain.criteria.ActivityStreamSearchCriteria;
import com.mycollab.common.i18n.GenericI18Enum;
import com.mycollab.common.service.ActivityStreamService;
import com.mycollab.configuration.StorageFactory;
import com.mycollab.core.utils.StringUtils;
import com.mycollab.db.arguments.NumberSearchField;
import com.mycollab.db.arguments.SearchCriteria;
import com.mycollab.db.arguments.SetSearchField;
import com.mycollab.html.DivLessFormatter;
import com.mycollab.module.crm.CrmLinkGenerator;
import com.mycollab.module.crm.CrmTypeConstants;
import com.mycollab.module.crm.i18n.CrmCommonI18nEnum;
import com.mycollab.module.crm.ui.CrmAssetsManager;
import com.mycollab.module.user.AccountLinkGenerator;
import com.mycollab.security.RolePermissionCollections;
import com.mycollab.spring.AppContextUtil;
import com.mycollab.vaadin.AppContext;
import com.mycollab.vaadin.TooltipHelper;
import com.mycollab.vaadin.ui.ELabel;
import com.mycollab.vaadin.ui.UIConstants;
import com.mycollab.vaadin.ui.registry.AuditLogRegistry;
import com.mycollab.vaadin.web.ui.WebUIConstants;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.vaadin.peter.buttongroup.ButtonGroup;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.layouts.MHorizontalLayout;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static com.mycollab.vaadin.TooltipHelper.TOOLTIP_ID;

/**
 * @author MyCollab Ltd.
 * @since 1.0
 */
public class ActivityStreamPanel extends CssLayout {
    private static final long serialVersionUID = 1L;
    private static final int MAX_NUMBER_DISPLAY = 20;

    private final CrmActivityStreamPagedList activityStreamList;

    public ActivityStreamPanel() {
        this.activityStreamList = new CrmActivityStreamPagedList();

        this.activityStreamList.addStyleName("stream-list");
        this.addComponent(this.activityStreamList);
        this.setStyleName("crm-activity-list");
    }

    public void display() {
        final ActivityStreamSearchCriteria searchCriteria = new ActivityStreamSearchCriteria();
        searchCriteria.setModuleSet(new SetSearchField<>(ModuleNameConstants.CRM));
        searchCriteria.setSaccountid(new NumberSearchField(AppContext.getAccountId()));
        searchCriteria.addOrderField(new SearchCriteria.OrderField("createdTime", SearchCriteria.DESC));
        this.activityStreamList.setSearchCriteria(searchCriteria);
    }

    private static class CrmActivityStreamPagedList extends VerticalLayout {
        private static final long serialVersionUID = 1L;

        private final CssLayout listContainer = new CssLayout();
        private CssLayout controlBarWrapper = new CssLayout();
        private CssLayout currentFeedBlock;

        private ActivityStreamService activityStreamService;
        private ActivityStreamSearchCriteria searchCriteria;

        private int firstIndex = 0;
        private Date currentDate;

        CrmActivityStreamPagedList() {
            listContainer.setWidth("100%");
            this.addComponent(listContainer);
            activityStreamService = AppContextUtil.getSpringBean(ActivityStreamService.class);
        }

        public void setSearchCriteria(final ActivityStreamSearchCriteria searchCriteria) {
            this.listContainer.removeAllComponents();
            this.searchCriteria = searchCriteria;
            doSearch(true);
        }

        private void doSearch(boolean isMoveForward) {
            this.listContainer.removeAllComponents();

            currentDate = new GregorianCalendar(2100, 1, 1).getTime();
            currentFeedBlock = new CssLayout();

            Integer currentItemsDisplay = 0;

            int tmpFirstIndex = firstIndex;

            while (currentItemsDisplay < MAX_NUMBER_DISPLAY) {
                final List<SimpleActivityStream> currentListData = this.activityStreamService
                        .findAbsoluteListByCriteria(searchCriteria, tmpFirstIndex, MAX_NUMBER_DISPLAY);

                if (currentListData.size() == 0) {
                    break;
                }

                for (SimpleActivityStream item : currentListData) {
                    if (checkReadPermisson(item.getType())) {
                        currentItemsDisplay++;
                        showItem(item);
                    }
                    if (currentItemsDisplay == MAX_NUMBER_DISPLAY) {
                        break;
                    }
                }

                if (isMoveForward) {
                    tmpFirstIndex += MAX_NUMBER_DISPLAY;
                } else {
                    tmpFirstIndex = Math.max(tmpFirstIndex - MAX_NUMBER_DISPLAY, 0);
                    if (tmpFirstIndex == 0) {
                        break;
                    }
                }
            }

            firstIndex = tmpFirstIndex;

            if (hasPrevious() || hasNext()) {
                this.addComponent(createPageControls());
            }
        }

        private void navigateToPrevious() {
            firstIndex = Math.max(firstIndex - MAX_NUMBER_DISPLAY, 0);
            doSearch(false);
        }

        private void navigateToNext() {
            firstIndex += MAX_NUMBER_DISPLAY;
            doSearch(true);
        }

        private boolean checkReadPermisson(String type) {
            if (CrmTypeConstants.ACCOUNT.equals(type)
                    && !AppContext.canRead(RolePermissionCollections.CRM_ACCOUNT)) {
                return false;
            } else if (CrmTypeConstants.CONTACT.equals(type)
                    && !AppContext.canRead(RolePermissionCollections.CRM_CONTACT)) {
                return false;
            } else if (CrmTypeConstants.CAMPAIGN.equals(type)
                    && !AppContext.canRead(RolePermissionCollections.CRM_CAMPAIGN)) {
                return false;
            } else if (CrmTypeConstants.LEAD.equals(type)
                    && !AppContext.canRead(RolePermissionCollections.CRM_LEAD)) {
                return false;
            } else if (CrmTypeConstants.OPPORTUNITY.equals(type)
                    && !AppContext.canRead(RolePermissionCollections.CRM_OPPORTUNITY)) {
                return false;
            } else if (CrmTypeConstants.CASE.equals(type)
                    && !AppContext.canRead(RolePermissionCollections.CRM_CASE)) {
                return false;
            } else if (CrmTypeConstants.TASK.equals(type)
                    && !AppContext.canRead(RolePermissionCollections.CRM_TASK)) {
                return false;
            } else if (CrmTypeConstants.MEETING.equals(type)
                    && !AppContext
                    .canRead(RolePermissionCollections.CRM_MEETING)) {
                return false;
            } else if (CrmTypeConstants.CALL.equals(type)
                    && !AppContext.canRead(RolePermissionCollections.CRM_CALL)) {
                return false;
            }
            return true;
        }

        private void showItem(final SimpleActivityStream activityStream) {
            final Date itemCreatedDate = activityStream.getCreatedtime();
            if (!DateUtils.isSameDay(currentDate, itemCreatedDate)) {
                currentFeedBlock = new CssLayout();
                currentFeedBlock.setStyleName("feed-block");
                feedBlocksPut(currentDate, itemCreatedDate, currentFeedBlock);
                currentDate = itemCreatedDate;
            }

            // --------------Item hidden div tooltip----------------
            String itemType = CrmLocalizationTypeMap.getType(activityStream.getType());
            String assigneeValue = buildAssigneeValue(activityStream);
            String itemValue = buildItemValue(activityStream);

            AuditLogRegistry auditLogRegistry = AppContextUtil.getSpringBean(AuditLogRegistry.class);
            StringBuilder content = new StringBuilder();

            if (ActivityStreamConstants.ACTION_CREATE.equals(activityStream.getAction())) {
                content.append(AppContext.getMessage(CrmCommonI18nEnum.WIDGET_ACTIVITY_CREATE_ACTION,
                        assigneeValue, itemType, itemValue));
            } else if (ActivityStreamConstants.ACTION_UPDATE.equals(activityStream.getAction())) {
                content.append(AppContext.getMessage(CrmCommonI18nEnum.WIDGET_ACTIVITY_UPDATE_ACTION,
                        assigneeValue, itemType, itemValue));
                if (activityStream.getAssoAuditLog() != null) {
                    content.append(auditLogRegistry.generatorDetailChangeOfActivity(activityStream));
                }
            } else if (ActivityStreamConstants.ACTION_COMMENT.equals(activityStream.getAction())) {
                content.append(AppContext.getMessage(CrmCommonI18nEnum.WIDGET_ACTIVITY_COMMENT_ACTION, assigneeValue, itemType, itemValue));
                if (activityStream.getAssoAuditLog() != null) {
                    content.append("<p><ul><li>\"").append(activityStream.getAssoAuditLog().getChangeset()).append("\"</li></ul></p>");
                }
            } else if (ActivityStreamConstants.ACTION_DELETE.equals(activityStream.getAction())) {
                content.append(AppContext.getMessage(CrmCommonI18nEnum.WIDGET_ACTIVITY_DELETE_ACTION,
                        assigneeValue, itemType, itemValue));
            }

            Label activityLink = new Label(content.toString(), ContentMode.HTML);
            CssLayout streamWrapper = new CssLayout();
            streamWrapper.setWidth("100%");
            streamWrapper.addStyleName("stream-wrapper");
            streamWrapper.addComponent(activityLink);
            currentFeedBlock.addComponent(streamWrapper);
        }

        private CssLayout createPageControls() {
            this.removeComponent(controlBarWrapper);
            this.controlBarWrapper = new CssLayout();
            this.controlBarWrapper.setWidth("100%");
            this.controlBarWrapper.setStyleName("page-controls");
            ButtonGroup controlBtns = new ButtonGroup();
            controlBtns.setStyleName(WebUIConstants.BUTTON_ACTION);

            MButton prevBtn = new MButton(AppContext.getMessage(GenericI18Enum.BUTTON_NAV_NEWER), clickEvent -> navigateToPrevious())
                    .withStyleName(WebUIConstants.BUTTON_ACTION).withWidth("64px");
            prevBtn.setEnabled(hasPrevious());

            MButton nextBtn = new MButton(AppContext.getMessage(GenericI18Enum.BUTTON_NAV_OLDER), clickEvent -> navigateToNext())
                    .withStyleName(WebUIConstants.BUTTON_ACTION).withWidth("64px");
            nextBtn.setEnabled(hasNext());

            controlBtns.addButton(prevBtn);
            controlBtns.addButton(nextBtn);

            controlBarWrapper.addComponent(controlBtns);
            return controlBarWrapper;
        }

        private boolean hasNext() {
            return !this.activityStreamService.findAbsoluteListByCriteria(
                    this.searchCriteria, firstIndex + MAX_NUMBER_DISPLAY, 1).isEmpty();
        }

        private boolean hasPrevious() {
            return (this.firstIndex > 0);
        }

        private String buildAssigneeValue(SimpleActivityStream activityStream) {
            DivLessFormatter div = new DivLessFormatter();
            Img userAvatar = new Img("", StorageFactory.getAvatarPath(activityStream.getCreatedUserAvatarId(), 16))
                    .setCSSClass(UIConstants.CIRCLE_BOX);
            A userLink = new A().setId("tag" + TOOLTIP_ID).setHref(AccountLinkGenerator.generatePreviewFullUserLink(
                    AppContext.getSiteUrl(), activityStream.getCreateduser())).appendText(StringUtils.trim
                    (activityStream.getCreatedUserFullName(), 30, true));

            userLink.setAttribute("onmouseover", TooltipHelper.userHoverJsFunction(activityStream.getCreateduser()));
            userLink.setAttribute("onmouseleave", TooltipHelper.itemMouseLeaveJsFunction());
            div.appendChild(userAvatar, DivLessFormatter.EMPTY_SPACE(), userLink);

            return div.write();
        }

        private String buildItemValue(SimpleActivityStream activityStream) {
            DivLessFormatter div = new DivLessFormatter();
            Text itemImg = new Text(CrmAssetsManager.getAsset(activityStream.getType()).getHtml());
            A itemLink = new A().setId("tag" + TOOLTIP_ID).setHref(CrmLinkGenerator.generateCrmItemLink(
                    activityStream.getType(), Integer.parseInt(activityStream.getTypeid())));

            itemLink.setAttribute("onmouseover", TooltipHelper.crmHoverJsFunction(activityStream.getType(), activityStream.getTypeid()));
            itemLink.setAttribute("onmouseleave", TooltipHelper.itemMouseLeaveJsFunction());
            itemLink.appendText(activityStream.getNamefield());

            div.appendChild(itemImg, DivLessFormatter.EMPTY_SPACE(), itemLink);
            return div.write();
        }

        private void feedBlocksPut(Date currentDate, Date nextDate, CssLayout currentBlock) {
            MHorizontalLayout blockWrapper = new MHorizontalLayout().withSpacing(false).withFullWidth().withStyleName
                    ("feed-block-wrap");

            blockWrapper.setDefaultComponentAlignment(Alignment.TOP_LEFT);
            Calendar cal1 = Calendar.getInstance();
            cal1.setTime(currentDate);

            Calendar cal2 = Calendar.getInstance();
            cal2.setTime(nextDate);

            if (cal1.get(Calendar.YEAR) != cal2.get(Calendar.YEAR)) {
                int currentYear = cal2.get(Calendar.YEAR);
                Label yearLbl = ELabel.html("<div>" + String.valueOf(currentYear) + "</div>").withStyleName
                        ("year-lbl").withWidthUndefined();
                this.listContainer.addComponent(yearLbl);
            } else {
                blockWrapper.setMargin(new MarginInfo(true, false, false, false));
            }
            Label dateLbl = new Label(DateFormatUtils.format(nextDate, "dd/MM"));
            dateLbl.setSizeUndefined();
            dateLbl.setStyleName("date-lbl");
            blockWrapper.with(dateLbl, currentBlock).expand(currentBlock);

            this.listContainer.addComponent(blockWrapper);
        }
    }
}
