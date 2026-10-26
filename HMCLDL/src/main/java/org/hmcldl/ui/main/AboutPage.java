/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2022  huangyuhui <huanghongxun2008@126.com> and contributors
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

import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import org.hmcldl.Metadata;
import org.hmcldl.ui.FXUtils;
import org.hmcldl.ui.construct.ComponentList;
import org.hmcldl.ui.construct.LineButton;

import static org.hmcldl.util.i18n.I18n.i18n;

/// The about page of HMCLDL: launcher identity and the copyright notice only.
public final class AboutPage extends org.hmcldl.ui.construct.SpinnerPane {
    public AboutPage() {
        VBox content = new VBox();
        content.getStyleClass().add("spinner-pane-content");
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        FXUtils.smoothScrolling(scrollPane);
        setContent(scrollPane);

        ComponentList about = new ComponentList();
        {
            var launcher = LineButton.createExternalLinkButton(Metadata.PUBLISH_URL);
            launcher.setLargeTitle(true);
            launcher.setLeading(FXUtils.newBuiltinImage("/assets/img/icon.png"));
            launcher.setTitle(Metadata.FULL_NAME);
            launcher.setSubtitle(Metadata.VERSION);

            about.getContent().setAll(launcher);
        }

        ComponentList legal = new ComponentList();
        {
            var copyright = new LineButton();
            copyright.setLargeTitle(true);
            copyright.setTitle(i18n("about.copyright"));
            copyright.setSubtitle(i18n("about.copyright.statement"));

            var hmcldl = new LineButton();
            hmcldl.setLargeTitle(true);
            hmcldl.setTitle("HMCLDL");
            hmcldl.setSubtitle(i18n("about.copyright.hmcldl"));

            legal.getContent().setAll(copyright, hmcldl);
        }

        content.getChildren().setAll(
                ComponentList.createComponentListTitle(i18n("about")),
                about,
                ComponentList.createComponentListTitle(i18n("about.legal")),
                legal
        );
    }
}
