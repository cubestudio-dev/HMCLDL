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
package org.jackhuang.hmcl.ui.download;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import org.jackhuang.hmcl.Metadata;
import org.jackhuang.hmcl.download.*;
import org.jackhuang.hmcl.download.game.GameRemoteVersion;
import org.jackhuang.hmcl.game.GameComponentType;
import org.jackhuang.hmcl.game.GameInstanceID;
import org.jackhuang.hmcl.game.HMCLDependencyManager;
import org.jackhuang.hmcl.game.HMCLGameBuilder;
import org.jackhuang.hmcl.game.HMCLGameRepository;
import org.jackhuang.hmcl.setting.DownloadProviders;
import org.jackhuang.hmcl.task.Schedulers;
import org.jackhuang.hmcl.task.Task;
import org.jackhuang.hmcl.ui.Controllers;
import org.jackhuang.hmcl.ui.FXUtils;
import org.jackhuang.hmcl.ui.decorator.DecoratorAnimatedPage;
import org.jackhuang.hmcl.ui.decorator.DecoratorPage;
import org.jackhuang.hmcl.ui.wizard.Navigation;
import org.jackhuang.hmcl.ui.wizard.WizardController;
import org.jackhuang.hmcl.ui.wizard.WizardProvider;
import org.jackhuang.hmcl.util.SettingsMap;
import org.jackhuang.hmcl.util.i18n.I18n;
import org.jackhuang.hmcl.theme.Themes;
import org.jetbrains.annotations.Nullable;

import static org.jackhuang.hmcl.ui.FXUtils.runInFX;
import static org.jackhuang.hmcl.util.i18n.I18n.i18n;

/// The home page of HMCLDL: downloading game versions and mod loaders.
///
/// Only the vanilla game version list is offered; after a version is selected,
/// the install wizard lets the user pick Forge / NeoForge / Fabric / Quilt /
/// OptiFine / LiteLoader components.
public class DownloadPage extends DecoratorAnimatedPage implements DecoratorPage {
    private final ReadOnlyObjectWrapper<DecoratorPage.State> state = new ReadOnlyObjectWrapper<>();
    private final BorderPane content = new BorderPane();
    private final DownloadNavigator versionPageNavigator = new DownloadNavigator();
    private VersionsPage versionsPage;

    public DownloadPage() {
        TitleNode titleNode = new TitleNode();
        state.setValue(new State(null, titleNode, false, false, true));

        versionsPage = new VersionsPage(versionPageNavigator,
                i18n("install.installer.choose", i18n("install.installer.game")),
                "",
                DownloadProviders.getDownloadProvider(),
                GameComponentType.GAME,
                versionPageNavigator::onGameSelected);
        content.setCenter(versionsPage);
        setCenter(content);
    }

    /// Builds the title-bar node: launcher icon on the left, full name on the right.
    private static final class TitleNode extends javafx.scene.layout.HBox {
        TitleNode() {
            setSpacing(8);
            setPadding(new Insets(0, 0, 0, 2));
            setAlignment(Pos.CENTER_LEFT);

            ImageView titleIcon = new ImageView(FXUtils.newBuiltinImage("/assets/img/icon-title.png"));
            Label titleLabel = new Label(Metadata.FULL_TITLE);
            if (I18n.isUpsideDown()) {
                titleIcon.setRotate(180);
                titleLabel.setRotate(180);
            }
            titleLabel.getStyleClass().add("jfx-decorator-title");
            titleLabel.textFillProperty().bind(Themes.titleFillProperty());
            getChildren().setAll(titleIcon, titleLabel);
        }
    }

    @Override
    public ReadOnlyObjectProperty<State> stateProperty() {
        return state.getReadOnlyProperty();
    }

    private static final class DownloadNavigator implements Navigation {
        private final SettingsMap settings = new SettingsMap();

        @Override
        public void onStart() {
        }

        @Override
        public void onNext() {
        }

        @Override
        public void onPrev(boolean cleanUp) {
        }

        @Override
        public boolean canPrev() {
            return false;
        }

        @Override
        public void onFinish() {
        }

        @Override
        public void onEnd() {
        }

        @Override
        public void onCancel() {
        }

        @Override
        public SettingsMap getSettings() {
            return settings;
        }

        public void onGameSelected() {
            HMCLGameRepository repository = org.jackhuang.hmcl.setting.GameDirectoryManager.getSelectedRepository();
            if (repository.isLoaded()) {
                Controllers.getDecorator().startWizard(new VanillaInstallWizardProvider(repository, (GameRemoteVersion) settings.get("game")), i18n("install.new_game"));
            }
        }
    }

    private static class VanillaInstallWizardProvider implements WizardProvider {
        private final HMCLGameRepository repository;
        private final HMCLDependencyManager dependencyManager;
        private final DownloadProvider downloadProvider;
        private final GameRemoteVersion gameVersion;

        public VanillaInstallWizardProvider(HMCLGameRepository repository, GameRemoteVersion gameVersion) {
            this.repository = repository;
            this.gameVersion = gameVersion;
            this.downloadProvider = DownloadProviders.getDownloadProvider();
            this.dependencyManager = repository.getDependency(downloadProvider);
        }

        @Override
        public void start(SettingsMap settings) {
            settings.put(ModpackPage.GAME_DIRECTORY, repository.getGameDirectory());
            settings.put(ModpackPage.REPOSITORY, repository);
            settings.put(GameComponentType.GAME.getPatchId(), gameVersion);
        }

        /// Builds the selected instance and selects it after successful completion.
        ///
        /// @param settings the installer selections and target instance id
        /// @return the builder task with its stage hints preserved as the outermost task wrapper
        private Task<?> finishVersionDownloadingAsync(SettingsMap settings) {
            @Nullable GameInstanceID instanceId = settings.get(AbstractInstallersPage.INSTANCE_ID);
            if (instanceId == null) {
                throw new IllegalStateException("Instance ID is not set");
            }

            try (HMCLGameBuilder builder = dependencyManager.newGameBuilder(instanceId)) {
                builder.component(GameComponentType.GAME, ((ComponentRemoteVersion) settings.get(GameComponentType.GAME.getPatchId())).getGameVersion());

                settings.asStringMap().forEach((key, value) -> {
                    if (!GameComponentType.GAME.getPatchId().equals(key)
                            && value instanceof ComponentRemoteVersion remoteVersion)
                        builder.component(remoteVersion);
                });

                boolean modded = GameComponentType.MOD_LOADERS.stream()
                        .anyMatch(componentType ->
                                settings.get(componentType.getPatchId()) instanceof ComponentRemoteVersion);
                if (repository.shouldIsolateNewInstance(modded)) {
                    builder.enableIsolation();
                }

                Task<?> buildTask = builder.buildAsync();
                settings.put(TaskCleanup.KEY, builder::abort);
                buildTask.onDone().register(event -> {
                    if (!event.isFailed()) {
                        runInFX(() -> repository.setSelectedInstance(repository.getInstance(instanceId)));
                    }
                });
                return buildTask;
            }
        }

        @Override
        public Object finish(SettingsMap settings) {
            settings.put("title", i18n("install.new_game.installation"));
            settings.put("success_message", i18n("install.success"));
            settings.put(FailureCallback.KEY, (settings1, exception, next) -> UpdateInstallerWizardProvider.alertFailureMessage(exception, next));

            return finishVersionDownloadingAsync(settings);
        }

        @Override
        public Node createPage(WizardController controller, int step, SettingsMap settings) {
            switch (step) {
                case 0:
                    return new InstallersPage(controller, repository, ((ComponentRemoteVersion) controller.getSettings().get("game")).getGameVersion(), downloadProvider);
                default:
                    throw new IllegalStateException("error step " + step + ", settings: " + settings + ", pages: " + controller.getPages());
            }
        }

        @Override
        public boolean cancel() {
            return true;
        }
    }
}
