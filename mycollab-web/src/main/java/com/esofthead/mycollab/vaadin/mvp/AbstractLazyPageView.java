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
package com.esofthead.mycollab.vaadin.mvp;

import com.esofthead.mycollab.vaadin.ui.AssetResource;
import com.esofthead.mycollab.vaadin.ui.WebResourceIds;
import com.vaadin.ui.Image;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * @author MyCollab Ltd.
 * @since 4.1.2
 */
public abstract class AbstractLazyPageView extends AbstractPageView implements LazyPageView {
    private static final long serialVersionUID = 1L;

    private boolean isRunning = false;
    private ProgressIndicator progressIndicator = null;

    @Override
    public void lazyLoadView() {
        if (!isRunning) {
            this.removeAllComponents();
            isRunning = true;
            new Thread() {
                @Override
                public void run() {
                    final UI currentUI = UI.getCurrent();
                    currentUI.access(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                progressIndicator = new ProgressIndicator();
                                currentUI.addWindow(progressIndicator);
                                currentUI.push();
                                displayView();
                            } finally {
                                currentUI.removeWindow(progressIndicator);
                                isRunning = false;
                                currentUI.push();
                            }
                        }

                    });
                }
            }.start();
        }
    }

    abstract protected void displayView();

    private static class ProgressIndicator extends Window {
        private static final long serialVersionUID = -6157950150738214354L;

        public ProgressIndicator() {
            super();
            this.setDraggable(false);
            this.setClosable(false);
            this.setResizable(false);
            this.setStyleName("lazyload-progress");
            this.center();
            this.setModal(true);

            Image loadingIcon = new Image(null, new AssetResource(WebResourceIds._lazy_load_icon));
            this.setContent(loadingIcon);
        }
    }
}