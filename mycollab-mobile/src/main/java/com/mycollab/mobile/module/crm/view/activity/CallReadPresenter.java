/**
 * This file is part of mycollab-mobile.
 *
 * mycollab-mobile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mycollab-mobile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with mycollab-mobile.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mycollab.mobile.module.crm.view.activity;

import com.mycollab.common.i18n.GenericI18Enum;
import com.mycollab.core.MyCollabException;
import com.mycollab.db.arguments.NumberSearchField;
import com.mycollab.eventmanager.EventBusFactory;
import com.mycollab.mobile.module.crm.events.ActivityEvent;
import com.mycollab.mobile.module.crm.view.AbstractCrmPresenter;
import com.mycollab.mobile.ui.ConfirmDialog;
import com.mycollab.module.crm.CrmLinkGenerator;
import com.mycollab.module.crm.domain.CallWithBLOBs;
import com.mycollab.module.crm.domain.SimpleCall;
import com.mycollab.module.crm.domain.criteria.CallSearchCriteria;
import com.mycollab.module.crm.i18n.CallI18nEnum;
import com.mycollab.module.crm.service.CallService;
import com.mycollab.security.RolePermissionCollections;
import com.mycollab.spring.AppContextUtil;
import com.mycollab.vaadin.AppContext;
import com.mycollab.vaadin.events.DefaultPreviewFormHandler;
import com.mycollab.vaadin.mvp.ScreenData;
import com.mycollab.vaadin.ui.NotificationUtil;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.UI;

/**
 * @author MyCollab Ltd.
 * @since 4.1
 */
public class CallReadPresenter extends AbstractCrmPresenter<CallReadView> {
    private static final long serialVersionUID = 9005269894958445146L;

    public CallReadPresenter() {
        super(CallReadView.class);
    }

    @Override
    protected void postInitView() {
        view.getPreviewFormHandlers().addFormHandler(new DefaultPreviewFormHandler<SimpleCall>() {
            @Override
            public void onEdit(SimpleCall data) {
                EventBusFactory.getInstance().post(new ActivityEvent.CallEdit(this, data));
            }

            @Override
            public void onDelete(final SimpleCall data) {
                ConfirmDialog.show(UI.getCurrent(),
                        AppContext.getMessage(GenericI18Enum.DIALOG_DELETE_SINGLE_ITEM_MESSAGE),
                        AppContext.getMessage(GenericI18Enum.BUTTON_YES),
                        AppContext.getMessage(GenericI18Enum.BUTTON_NO),
                        new ConfirmDialog.CloseListener() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public void onClose(ConfirmDialog dialog) {
                                if (dialog.isConfirmed()) {
                                    CallService callService = AppContextUtil.getSpringBean(CallService.class);
                                    callService.removeWithSession(data,
                                            AppContext.getUsername(), AppContext.getAccountId());
                                    EventBusFactory.getInstance().post(new ActivityEvent.GotoList(this, null));
                                }
                            }
                        });
            }

            @Override
            public void onClone(SimpleCall data) {
                CallWithBLOBs cloneData = (CallWithBLOBs) data.copy();
                cloneData.setId(null);
                EventBusFactory.getInstance().post(new ActivityEvent.CallEdit(this, cloneData));
            }

            @Override
            public void onCancel() {
                EventBusFactory.getInstance().post(new ActivityEvent.GotoList(this, null));
            }

            @Override
            public void gotoNext(SimpleCall data) {
                CallService callService = AppContextUtil.getSpringBean(CallService.class);
                CallSearchCriteria criteria = new CallSearchCriteria();
                criteria.setSaccountid(new NumberSearchField(AppContext.getAccountId()));
                criteria.setId(new NumberSearchField(data.getId(), NumberSearchField.GREATER()));
                Integer nextId = callService.getNextItemKey(criteria);
                if (nextId != null) {
                    EventBusFactory.getInstance().post(new ActivityEvent.CallRead(this, nextId));
                } else {
                    NotificationUtil.showGotoLastRecordNotification();
                }
            }

            @Override
            public void gotoPrevious(SimpleCall data) {
                CallService callService = AppContextUtil.getSpringBean(CallService.class);
                CallSearchCriteria criteria = new CallSearchCriteria();
                criteria.setSaccountid(new NumberSearchField(AppContext.getAccountId()));
                criteria.setId(new NumberSearchField(data.getId(), NumberSearchField.LESS_THAN()));
                Integer nextId = callService.getPreviousItemKey(criteria);
                if (nextId != null) {
                    EventBusFactory.getInstance().post(new ActivityEvent.CallRead(this, nextId));
                } else {
                    NotificationUtil.showGotoFirstRecordNotification();
                }
            }
        });
    }

    @Override
    protected void onGo(ComponentContainer container, ScreenData<?> data) {
        if (AppContext.canRead(RolePermissionCollections.CRM_CALL)) {
            SimpleCall call;
            if (data.getParams() instanceof Integer) {
                CallService callService = AppContextUtil.getSpringBean(CallService.class);
                call = callService.findById((Integer) data.getParams(), AppContext.getAccountId());
                if (call == null) {
                    NotificationUtil.showRecordNotExistNotification();
                    return;
                }
            } else {
                throw new MyCollabException("Invalid data: " + data);
            }
            view.previewItem(call);
            super.onGo(container, data);

            AppContext.addFragment(CrmLinkGenerator.generateCallPreviewLink(call.getId()), AppContext.getMessage(GenericI18Enum.BROWSER_PREVIEW_ITEM_TITLE,
                    AppContext.getMessage(CallI18nEnum.SINGLE), call.getSubject()));
        } else {
            NotificationUtil.showMessagePermissionAlert();
        }
    }

}
