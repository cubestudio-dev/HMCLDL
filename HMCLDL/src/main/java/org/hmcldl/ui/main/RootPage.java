/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2021  huangyuhui <huanghongxun2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.hmcldl.ui.main;

import javafx.beans.property.ReadOnlyObjectProperty;
import org.hmcldl.ui.download.DownloadPage;
import org.hmcldl.ui.decorator.DecoratorAnimatedPage;
import org.hmcldl.ui.decorator.DecoratorPage;

/// The root page of HMCLDL: the game download page IS the main interface,
/// shown without any side menu. Settings is opened from the gear button
/// in the title bar, and closed with the back button there.
public class RootPage extends DecoratorAnimatedPage implements DecoratorPage {
    private DownloadPage downloadPage = null;

    public RootPage() {
        getStyleClass().remove("gray-background");
    }

    @Override
    public ReadOnlyObjectProperty<State> stateProperty() {
        return getDownloadPage().stateProperty();
    }

    @Override
    protected Skin createDefaultSkin() {
        return new Skin(this);
    }

    public DownloadPage getDownloadPage() {
        if (downloadPage == null) {
            downloadPage = new DownloadPage();
            this.downloadPage = downloadPage;
        }
        return downloadPage;
    }

    private static class Skin extends DecoratorAnimatedPageSkin<RootPage> {

        protected Skin(RootPage control) {
            super(control);

            // The download page fills the whole window; no side menu.
            // Settings is reached through the gear button in the title bar.
            setCenter(getSkinnable().getDownloadPage());
        }
    }
}
