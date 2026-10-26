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
package org.jackhuang.hmcl.ui.main;

import javafx.beans.property.ReadOnlyObjectProperty;
import org.jackhuang.hmcl.ui.Controllers;
import org.jackhuang.hmcl.ui.FXUtils;
import org.jackhuang.hmcl.ui.SVG;
import org.jackhuang.hmcl.ui.animation.AnimationUtils;
import org.jackhuang.hmcl.ui.construct.AdvancedListBox;
import org.jackhuang.hmcl.ui.construct.AdvancedListItem;
import org.jackhuang.hmcl.ui.decorator.DecoratorAnimatedPage;
import org.jackhuang.hmcl.ui.decorator.DecoratorPage;
import org.jackhuang.hmcl.ui.download.DownloadPage;

/// The root page of HMCLDL: the game download page is the home page,
/// with a minimal sidebar offering downloads and settings only.
public class RootPage extends DecoratorAnimatedPage implements DecoratorPage {
    private DownloadPage downloadPage = null;

    public RootPage() {
        getStyleClass().remove("gray-background");
        getLeft().getStyleClass().add("gray-background");
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

            // the only main entry: download games
            AdvancedListItem downloadItem = new AdvancedListItem();
            downloadItem.setLeftIcon(SVG.DOWNLOAD);
            downloadItem.setTitle(org.jackhuang.hmcl.util.i18n.I18n.i18n("hmcldl.download.game"));
            downloadItem.setOnAction(e -> {
                // already on the download page; nothing to navigate
            });

            // settings in the bottom left corner
            AdvancedListItem launcherSettingsItem = new AdvancedListItem();
            launcherSettingsItem.setLeftIcon(SVG.SETTINGS);
            launcherSettingsItem.setTitle(org.jackhuang.hmcl.util.i18n.I18n.i18n("settings"));
            launcherSettingsItem.setOnAction(e -> {
                Controllers.navigate(Controllers.getSettingsPage());
            });
            if (AnimationUtils.isAnimationEnabled()) {
                FXUtils.prepareOnMouseEnter(launcherSettingsItem, Controllers::prepareSettingsPage);
            }

            // the left sidebar: downloads on top, settings pinned to the bottom
            AdvancedListBox sideBar = new AdvancedListBox()
                    .add(downloadItem);
            AdvancedListBox bottomBar = new AdvancedListBox()
                    .add(launcherSettingsItem);

            // the page's left container is a VBox; let the download list expand
            // so that the settings list sticks to the bottom
            javafx.scene.layout.VBox.setVgrow(sideBar, javafx.scene.layout.Priority.ALWAYS);

            // the root page, with the sidebar in left, download page in center.
            setLeft(sideBar, bottomBar);
            setCenter(getSkinnable().getDownloadPage());
        }
    }
}
