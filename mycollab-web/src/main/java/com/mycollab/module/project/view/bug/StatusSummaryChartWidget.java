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
package com.mycollab.module.project.view.bug;

import com.mycollab.common.domain.GroupItem;
import com.mycollab.ui.chart.PieChartWrapper;
import com.mycollab.db.arguments.SetSearchField;
import com.mycollab.core.utils.BeanUtility;
import com.mycollab.eventmanager.EventBusFactory;
import com.mycollab.module.project.events.BugEvent;
import com.mycollab.module.project.i18n.OptionI18nEnum;
import com.mycollab.module.project.i18n.OptionI18nEnum.BugStatus;
import com.mycollab.module.tracker.domain.criteria.BugSearchCriteria;
import com.mycollab.module.tracker.service.BugService;
import com.mycollab.spring.AppContextUtil;
import com.mycollab.vaadin.mvp.ViewComponent;
import org.jfree.data.general.DefaultPieDataset;

import java.util.List;

/**
 * @author MyCollab Ltd.
 * @since 1.0
 */
@ViewComponent
public class StatusSummaryChartWidget extends PieChartWrapper<BugSearchCriteria> implements IStatusSummaryChartWidget {
    private static final long serialVersionUID = 1L;

    public StatusSummaryChartWidget() {
        super(BugStatus.class, 350, 280);
    }

    @Override
    protected List<GroupItem> loadGroupItems() {
        BugService bugService = AppContextUtil.getSpringBean(BugService.class);
        return bugService.getStatusSummary(searchCriteria);
    }

    @Override
    protected DefaultPieDataset createDataset() {
        // create the dataset...
        final DefaultPieDataset dataset = new DefaultPieDataset();

        BugStatus[] bugStatuses = OptionI18nEnum.bug_statuses;
        for (BugStatus status : bugStatuses) {
            boolean isFound = false;
            for (GroupItem item : groupItems) {
                if (status.name().equals(item.getGroupid())) {
                    dataset.setValue(status.name(), item.getValue());
                    isFound = true;
                    break;
                }
            }

            if (!isFound) {
                dataset.setValue(status.name(), 0);
            }
        }

        return dataset;
    }

    @Override
    public void clickLegendItem(String key) {
        BugSearchCriteria cloneSearchCriteria = BeanUtility.deepClone(searchCriteria);
        cloneSearchCriteria.setStatuses(new SetSearchField<>(key));
        EventBusFactory.getInstance().post(new BugEvent.GotoList(this, cloneSearchCriteria));
    }
}