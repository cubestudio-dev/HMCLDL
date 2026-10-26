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

import com.jfoenix.controls.*;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.*;
import org.hmcldl.setting.DefaultIsolationType;
import org.hmcldl.setting.DownloadSource;
import org.hmcldl.setting.EnumCommonDirectory;
import org.hmcldl.setting.GameDirectory;
import org.hmcldl.setting.GameDirectoryID;
import org.hmcldl.setting.GameDirectoryManager;
import org.hmcldl.setting.ProxyType;
import org.hmcldl.setting.SettingsManager;
import org.hmcldl.task.FetchTask;
import org.hmcldl.task.Schedulers;
import org.hmcldl.task.Task;
import org.hmcldl.setting.ThemeColorType;
import org.hmcldl.theme.ThemeColor;
import org.hmcldl.ui.FXUtils;
import org.hmcldl.ui.WeakListenerHolder;
import org.hmcldl.ui.construct.*;
import org.hmcldl.util.FXThread;
import org.hmcldl.util.Holder;
import org.hmcldl.util.PortablePath;
import org.hmcldl.util.i18n.I18n;
import org.hmcldl.util.io.FileUtils;
import org.hmcldl.util.javafx.SafeStringConverter;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static org.hmcldl.setting.SettingsManager.settings;
import static org.hmcldl.util.i18n.I18n.i18n;

/// Downloader settings of HMCLDL: basic options (instance isolation, download
/// path, appearance), download sources, cache directory, download threads and proxy.
public class DownloadSettingsPage extends StackPane {

    private final WeakListenerHolder holder = new WeakListenerHolder();

    public DownloadSettingsPage() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setFillWidth(true);
        ScrollPane scrollPane = new ScrollPane(content);
        FXUtils.smoothScrolling(scrollPane);
        scrollPane.setFitToWidth(true);
        getChildren().setAll(scrollPane);

        // ===== Basic settings: isolation / download path / appearance =====
        {
            ComponentList basic = new ComponentList();
            basic.getStyleClass().add("card-non-transparent");

            // --- Version isolation ---
            {
                var isolationPane = new LineSelectButton<DefaultIsolationType>();
                isolationPane.setTitle(i18n("hmcldl.isolation"));
                isolationPane.setSubtitle(i18n("hmcldl.isolation.subtitle"));
                isolationPane.setNullSafeConverter(type -> switch (type) {
                    case ALWAYS -> i18n("settings.game.default_isolation.always");
                    case MODDED -> i18n("settings.game.default_isolation.modded");
                    case NEVER -> i18n("settings.game.default_isolation.never");
                });
                isolationPane.setItems(List.of(DefaultIsolationType.values()));

                Runnable rebindIsolation = () -> {
                    var repository = GameDirectoryManager.getSelectedRepository();
                    isolationPane.valueProperty().bindBidirectional(
                            repository.getParentGameSettings(null).defaultIsolationTypeProperty());
                };
                rebindIsolation.run();
                holder.add(FXUtils.onWeakChange(GameDirectoryManager.selectedRepositoryProperty(), ignored -> {
                    isolationPane.valueProperty().unbindBidirectional(
                            GameDirectoryManager.getSelectedRepository()
                                    .getParentGameSettings(null).defaultIsolationTypeProperty());
                    rebindIsolation.run();
                }));

                basic.getContent().add(isolationPane);
            }

            // --- Game download path ---
            {
                ComponentSublist downloadPathSublist = new ComponentSublist(() -> {
                    LineFileChooserButton pathButton = new LineFileChooserButton();
                    pathButton.setTitle(i18n("hmcldl.download.path"));
                    pathButton.setFileChooserTitle(i18n("game_directory.instance_directory.choose"));
                    pathButton.setType(LineFileChooserButton.Type.OPEN_DIRECTORY);

                    Holder<Boolean> updating = new Holder<>(false);
                    pathButton.locationProperty().addListener((observable, oldValue, newValue) -> {
                        if (updating.value || newValue == null || newValue.isBlank()) return;
                        try {
                            applyGameDownloadPath(newValue);
                        } catch (Exception e) {
                            pathButton.setLocation(oldValue);
                        }
                    });
                    holder.add(FXUtils.onWeakChange(GameDirectoryManager.selectedGameDirectoryProperty(), dir -> {
                        updating.value = true;
                        try {
                            pathButton.setLocation(dir.getPath().toPath().normalize().toString());
                        } finally {
                            updating.value = false;
                        }
                    }));
                    pathButton.setLocation(GameDirectoryManager.getSelectedGameDirectory().getPath().toPath().normalize().toString());

                    return List.of(pathButton);
                });
                downloadPathSublist.setTitle(i18n("hmcldl.download.path"));
                downloadPathSublist.setHasSubtitle(true);
                downloadPathSublist.descriptionProperty().bind(Bindings.createStringBinding(
                        () -> Optional.ofNullable(GameDirectoryManager.getSelectedGameDirectory())
                                .map(dir -> dir.getPath().toString())
                                .orElse(""),
                        GameDirectoryManager.selectedGameDirectoryProperty()));

                basic.getContent().add(downloadPathSublist);
            }

            // --- Appearance: theme mode + theme color ---
            {
                ComponentSublist appearanceSublist = new ComponentSublist(() -> {
                    // theme brightness: auto / light / dark
                    LineSelectButton<String> brightnessPane = new LineSelectButton<>();
                    brightnessPane.setTitle(i18n("settings.launcher.brightness"));
                    brightnessPane.setConverter(name -> i18n("settings.launcher.brightness."
                            + Objects.requireNonNullElse(name, "auto").trim().toLowerCase()));
                    brightnessPane.setItems(Arrays.asList("auto", "light", "dark"));
                    brightnessPane.setValue(isBrightnessOverridden()
                            ? Objects.requireNonNullElse(settings().themeBrightnessModeProperty().get(), "auto")
                            : "auto");
                    brightnessPane.valueProperty().addListener((observable, oldValue, newValue) -> {
                        if (newValue == null) return;
                        settings().themeBrightnessModeProperty().set(newValue);
                        if ("auto".equals(newValue)) {
                            settings().getThemeAppearanceOverrides().remove(org.hmcldl.setting.LauncherSettings.THEME_APPEARANCE_BRIGHTNESS_MODE);
                        } else {
                            settings().getThemeAppearanceOverrides().add(org.hmcldl.setting.LauncherSettings.THEME_APPEARANCE_BRIGHTNESS_MODE);
                        }
                    });

                    // theme color: default / custom
                    ColorPicker picker = new com.jfoenix.controls.JFXColorPicker();
                    picker.getCustomColors().setAll(ThemeColor.STANDARD_COLORS.stream().map(ThemeColor::color).toList());

                    var defaultColorChoice = new RadioChoiceList.Choice<ThemeColorType>(
                            i18n("settings.launcher.theme_color_type.default"), ThemeColorType.DEFAULT);

                    var customColorChoice = new RadioChoiceList.Choice<ThemeColorType>(
                            i18n("settings.launcher.theme_color_type.custom"), ThemeColorType.CUSTOM) {
                        @Override
                        protected Node createRightNode() {
                            return picker;
                        }
                    };
                    customColorChoice.setSubtitle(i18n("settings.launcher.theme_color_type.custom.description"));

                    RadioChoiceList<ThemeColorType> themeColorChoiceList = new RadioChoiceList<>();
                    themeColorChoiceList.setFallbackValue(ThemeColorType.DEFAULT);
                    themeColorChoiceList.setChoices(Arrays.asList(defaultColorChoice, customColorChoice));

                    Holder<Boolean> updatingColor = new Holder<>(false);
                    Runnable refreshColor = () -> {
                        if (updatingColor.value) return;
                        updatingColor.value = true;
                        try {
                            boolean overridden = settings().getThemeAppearanceOverrides()
                                    .contains(org.hmcldl.setting.LauncherSettings.THEME_APPEARANCE_COLOR);
                            ThemeColorType type = overridden
                                    ? Objects.requireNonNullElse(settings().themeColorTypeProperty().get(), ThemeColorType.DEFAULT)
                                    : ThemeColorType.DEFAULT;
                            themeColorChoiceList.selectedValueProperty().set(type);
                            picker.setValue(Objects.requireNonNullElse(
                                    settings().customThemeColorProperty().get(), ThemeColor.DEFAULT).color());
                        } finally {
                            updatingColor.value = false;
                        }
                    };
                    themeColorChoiceList.selectedValueProperty().addListener((observable, oldValue, newValue) -> {
                        if (updatingColor.value || newValue == null) return;
                        updatingColor.value = true;
                        try {
                            settings().themeColorTypeProperty().set(newValue);
                            if (newValue == ThemeColorType.CUSTOM) {
                                javafx.scene.paint.Color color = picker.getValue();
                                settings().customThemeColorProperty().set(
                                        color != null ? ThemeColor.of(color) : ThemeColor.DEFAULT);
                                settings().getThemeAppearanceOverrides().add(org.hmcldl.setting.LauncherSettings.THEME_APPEARANCE_COLOR);
                            } else {
                                settings().getThemeAppearanceOverrides().remove(org.hmcldl.setting.LauncherSettings.THEME_APPEARANCE_COLOR);
                            }
                        } finally {
                            updatingColor.value = false;
                        }
                    });
                    picker.valueProperty().addListener((observable, oldValue, newValue) -> {
                        if (updatingColor.value || newValue == null) return;
                        settings().customThemeColorProperty().set(ThemeColor.of(newValue));
                        settings().themeColorTypeProperty().set(ThemeColorType.CUSTOM);
                        settings().getThemeAppearanceOverrides().add(org.hmcldl.setting.LauncherSettings.THEME_APPEARANCE_COLOR);
                    });
                    refreshColor.run();

                    return List.of(brightnessPane, themeColorChoiceList);
                });
                appearanceSublist.setTitle(i18n("hmcldl.appearance"));
                appearanceSublist.setHasSubtitle(true);
                appearanceSublist.descriptionProperty().bind(Bindings.createStringBinding(
                        () -> i18n("settings.launcher.brightness."
                                + (isBrightnessOverridden()
                                ? Objects.requireNonNullElse(settings().themeBrightnessModeProperty().get(), "auto").trim().toLowerCase()
                                : "auto")),
                        settings().themeBrightnessModeProperty(), settings().getThemeAppearanceOverrides()));

                basic.getContent().add(appearanceSublist);
            }

            content.getChildren().addAll(ComponentList.createComponentListTitle(i18n("hmcldl.basic")), basic);
        }

        // ===== Download sources =====
        {
            var downloadSource = new ComponentList();
            downloadSource.getStyleClass().add("card-non-transparent");
            {
                Function<DownloadSource, String> converter = source -> switch (source) {
                    case DEFAULT -> i18n("settings.launcher.download_source.auto");
                    case OFFICIAL -> i18n("download.provider.official");
                    case MIRROR -> i18n("download.provider.mirror");
                };
                Function<DownloadSource, String> descriptionConverter = source -> {
                    String bundleKey = switch (source) {
                        case DEFAULT -> "download.provider.balanced.desc";
                        case OFFICIAL -> "download.provider.official.desc";
                        case MIRROR -> "download.provider.mirror.desc";
                    };
                    return I18n.hasKey(bundleKey) ? i18n(bundleKey) : null;
                };

                var versionListSourcePane = new LineSelectButton<DownloadSource>();
                versionListSourcePane.setTitle(i18n("settings.launcher.version_list_source"));
                versionListSourcePane.setNullSafeConverter(converter);
                versionListSourcePane.setDescriptionConverter(descriptionConverter);
                versionListSourcePane.setItems(DownloadSource.values());
                versionListSourcePane.valueProperty().bindBidirectional(settings().versionListSourceProperty());

                var downloadSourcePane = new LineSelectButton<DownloadSource>();
                downloadSourcePane.setTitle(i18n("settings.launcher.download_source"));
                downloadSourcePane.setNullSafeConverter(converter);
                downloadSourcePane.setDescriptionConverter(descriptionConverter);
                downloadSourcePane.setItems(DownloadSource.values());
                downloadSourcePane.valueProperty().bindBidirectional(settings().fileDownloadSourceProperty());

                downloadSource.getContent().setAll(versionListSourcePane, downloadSourcePane);
            }

            content.getChildren().addAll(ComponentList.createComponentListTitle(i18n("settings.launcher.download_source")), downloadSource);
        }

        // ===== Download: cache directory and threads =====
        {
            var downloadList = new ComponentList();

            ComponentSublist fileCommonLocationSublist = new ComponentSublist(() -> {
                MultiFileItem<EnumCommonDirectory> fileCommonLocation = new MultiFileItem<>();
                fileCommonLocation.loadChildren(Arrays.asList(
                        new MultiFileItem.Option<>(i18n("launcher.cache_directory.default"), EnumCommonDirectory.DEFAULT),
                        new MultiFileItem.FileOption<>(i18n("settings.custom"), EnumCommonDirectory.CUSTOM)
                                .setChooserTitle(i18n("launcher.cache_directory.choose"))
                                .setSelectionMode(FileSelector.SelectionMode.DIRECTORY)
                                .bindBidirectional(settings().commonDirectoryProperty())
                ));
                fileCommonLocation.selectedDataProperty().bindBidirectional(settings().commonDirectoryTypeProperty());
                return List.of(fileCommonLocation);
            });
            fileCommonLocationSublist.setTitle(i18n("launcher.cache_directory"));
            fileCommonLocationSublist.setHasSubtitle(true);
            fileCommonLocationSublist.descriptionProperty().bind(
                    Bindings.createObjectBinding(() -> Optional.ofNullable(settings().getResolvedCommonDirectory())
                                    .orElse(i18n("launcher.cache_directory.disabled")),
                            settings().commonDirectoryProperty(), settings().commonDirectoryTypeProperty()));

            JFXButton cleanButton = FXUtils.newBorderButton(i18n("launcher.cache_directory.clean"));
            cleanButton.setOnAction(e -> clearCacheDirectory(cleanButton));
            fileCommonLocationSublist.setHeaderRight(cleanButton);

            ComponentSublist downloadThreadsSublist = new ComponentSublist(() -> {
                var downloadThreadsList = new RadioChoiceList<Boolean>();
                downloadThreadsList.setChoices(
                        new RadioChoiceList.Choice<>(i18n("settings.launcher.download.threads.auto"), true),
                        new RadioChoiceList.Choice<>(i18n("settings.launcher.download.threads.custom"), false) {
                            @Override
                            protected Node createRightNode() {
                                HBox hbox = new HBox(8);
                                hbox.setViewOrder(-1);
                                hbox.setAlignment(Pos.CENTER);
                                hbox.disableProperty().bind(settings().autoDownloadThreadsProperty());

                                JFXSlider slider = new JFXSlider(1, 256, 64);
                                HBox.setHgrow(slider, Priority.ALWAYS);

                                JFXTextField threadsField = new JFXTextField();
                                FXUtils.setLimitWidth(threadsField, 60);
                                FXUtils.bind(threadsField, settings().downloadThreadsProperty(), SafeStringConverter.fromInteger()
                                        .restrict(it -> it > 0)
                                        .fallbackTo(FetchTask.DEFAULT_CONCURRENCY)
                                        .asPredicate(Validator.addTo(threadsField)));

                                var changedByTextField = new Holder<>(false);
                                FXUtils.onChangeAndOperate(settings().downloadThreadsProperty(), value -> {
                                    changedByTextField.value = true;
                                    slider.setValue(value.intValue());
                                    changedByTextField.value = false;
                                });
                                slider.valueProperty().addListener((value, oldVal, newVal) -> {
                                    if (changedByTextField.value) return;
                                    settings().downloadThreadsProperty().set(value.getValue().intValue());
                                });

                                hbox.getChildren().setAll(slider, threadsField);
                                return hbox;
                            }
                        }
                );

                downloadThreadsList.selectedValueProperty().bindBidirectional(settings().autoDownloadThreadsProperty());

                return List.of(downloadThreadsList);
            });
            downloadThreadsSublist.setTitle(i18n("settings.launcher.download.threads"));
            downloadThreadsSublist.descriptionProperty().bind(Bindings.createStringBinding(() -> {
                if (settings().autoDownloadThreadsProperty().get()) {
                    return i18n("settings.launcher.download.threads.auto");
                } else {
                    return Integer.toString(settings().downloadThreadsProperty().get());
                }
            }, settings().autoDownloadThreadsProperty(), settings().downloadThreadsProperty()));

            downloadList.getContent().addAll(fileCommonLocationSublist, downloadThreadsSublist);
            content.getChildren().addAll(ComponentList.createComponentListTitle(i18n("download")), downloadList);
        }

        // ===== Proxy =====
        {
            VBox proxyList = new VBox(10);
            proxyList.getStyleClass().add("card-non-transparent");

            HBox proxyTypePane = new HBox();
            {
                proxyTypePane.setPadding(new Insets(10, 0, 0, 0));

                ToggleGroup proxyConfigurationGroup = new ToggleGroup();

                JFXRadioButton chkProxySystem = new JFXRadioButton(i18n("settings.launcher.proxy.default"));
                chkProxySystem.setUserData(ProxyType.SYSTEM);
                chkProxySystem.setToggleGroup(proxyConfigurationGroup);

                JFXRadioButton chkProxyNone = new JFXRadioButton(i18n("settings.launcher.proxy.none"));
                chkProxyNone.setUserData(ProxyType.DIRECT);
                chkProxyNone.setToggleGroup(proxyConfigurationGroup);

                JFXRadioButton chkProxyHttp = new JFXRadioButton(i18n("settings.launcher.proxy.http"));
                chkProxyHttp.setUserData(ProxyType.HTTP);
                chkProxyHttp.setToggleGroup(proxyConfigurationGroup);


                JFXRadioButton chkProxySocks = new JFXRadioButton(i18n("settings.launcher.proxy.socks"));
                chkProxySocks.setUserData(ProxyType.SOCKS);
                chkProxySocks.setToggleGroup(proxyConfigurationGroup);

                switch (settings().proxyTypeProperty().get()) {
                    case DIRECT -> chkProxyNone.setSelected(true);
                    case HTTP -> chkProxyHttp.setSelected(true);
                    case SOCKS -> chkProxySocks.setSelected(true);
                    case SYSTEM -> chkProxySystem.setSelected(true);
                }

                holder.add(FXUtils.onWeakChange(proxyConfigurationGroup.selectedToggleProperty(), toggle -> {
                    settings().proxyTypeProperty().set(toggle != null
                            ? (ProxyType) toggle.getUserData()
                            : ProxyType.SYSTEM);
                }));

                proxyTypePane.getChildren().setAll(chkProxySystem, chkProxyNone, chkProxyHttp, chkProxySocks);
                proxyList.getChildren().add(proxyTypePane);
            }

            VBox proxyPane = new VBox();
            {
                proxyPane.disableProperty().bind(
                        Bindings.createBooleanBinding(() ->
                                        !settings().proxyTypeProperty().get().usesCustomAddress(),
                                settings().proxyTypeProperty()));

                ColumnConstraints colHgrow = new ColumnConstraints();
                colHgrow.setHgrow(Priority.ALWAYS);

                {
                    GridPane gridPane = new GridPane();
                    gridPane.setPadding(new Insets(0, 0, 0, 30));
                    gridPane.setHgap(20);
                    gridPane.setVgap(10);
                    gridPane.getColumnConstraints().setAll(new ColumnConstraints(), colHgrow);
                    gridPane.getRowConstraints().setAll(new RowConstraints(), new RowConstraints());

                    {
                        Label host = new Label(i18n("settings.launcher.proxy.host"));
                        GridPane.setRowIndex(host, 1);
                        GridPane.setColumnIndex(host, 0);
                        gridPane.getChildren().add(host);
                    }

                    {
                        JFXTextField txtProxyHost = new JFXTextField();
                        GridPane.setRowIndex(txtProxyHost, 1);
                        GridPane.setColumnIndex(txtProxyHost, 1);
                        gridPane.getChildren().add(txtProxyHost);
                        FXUtils.bindString(txtProxyHost, settings().proxyHostProperty());
                    }

                    {
                        Label port = new Label(i18n("settings.launcher.proxy.port"));
                        GridPane.setRowIndex(port, 2);
                        GridPane.setColumnIndex(port, 0);
                        gridPane.getChildren().add(port);
                    }

                    {
                        JFXTextField txtProxyPort = new JFXTextField();
                        GridPane.setFillWidth(txtProxyPort, false);
                        txtProxyPort.setMaxWidth(200);
                        GridPane.setRowIndex(txtProxyPort, 2);
                        GridPane.setColumnIndex(txtProxyPort, 1);
                        FXUtils.setValidateWhileTextChanged(txtProxyPort, true);
                        gridPane.getChildren().add(txtProxyPort);

                        FXUtils.bind(txtProxyPort, settings().proxyPortProperty(), SafeStringConverter.fromInteger()
                                .restrict(it -> it >= 0 && it <= 0xFFFF)
                                .fallbackTo(0)
                                .asPredicate(Validator.addTo(txtProxyPort)));
                    }
                    proxyPane.getChildren().add(gridPane);
                }

                VBox chkProxyAuthenticationPane = new VBox();
                {
                    chkProxyAuthenticationPane.setPadding(new Insets(20, 0, 20, 5));

                    JFXCheckBox chkProxyAuthentication = new JFXCheckBox(i18n("settings.launcher.proxy.authentication"));
                    chkProxyAuthenticationPane.getChildren().add(chkProxyAuthentication);
                    chkProxyAuthentication.selectedProperty().bindBidirectional(settings().hasProxyAuthProperty());

                    proxyPane.getChildren().add(chkProxyAuthenticationPane);
                }

                GridPane authPane = new GridPane();
                {
                    authPane.setPadding(new Insets(0, 0, 0, 30));
                    authPane.setHgap(20);
                    authPane.setVgap(10);
                    authPane.getColumnConstraints().setAll(new ColumnConstraints(), colHgrow);
                    authPane.getRowConstraints().setAll(new RowConstraints(), new RowConstraints());
                    authPane.disableProperty().bind(settings().hasProxyAuthProperty().not());

                    {
                        Label username = new Label(i18n("settings.launcher.proxy.username"));
                        GridPane.setRowIndex(username, 0);
                        GridPane.setColumnIndex(username, 0);
                        authPane.getChildren().add(username);
                    }

                    {
                        JFXTextField txtProxyUsername = new JFXTextField();
                        GridPane.setRowIndex(txtProxyUsername, 0);
                        GridPane.setColumnIndex(txtProxyUsername, 1);
                        authPane.getChildren().add(txtProxyUsername);
                        FXUtils.bindString(txtProxyUsername, settings().proxyUserProperty());
                    }

                    {
                        Label password = new Label(i18n("settings.launcher.proxy.password"));
                        GridPane.setRowIndex(password, 1);
                        GridPane.setColumnIndex(password, 0);
                        authPane.getChildren().add(password);
                    }

                    {
                        JFXPasswordField txtProxyPassword = new JFXPasswordField();
                        GridPane.setRowIndex(txtProxyPassword, 1);
                        GridPane.setColumnIndex(txtProxyPassword, 1);
                        authPane.getChildren().add(txtProxyPassword);
                        txtProxyPassword.textProperty().bindBidirectional(settings().proxyPasswordProperty());
                    }

                    proxyPane.getChildren().add(authPane);
                    proxyList.getChildren().add(proxyPane);
                }
            }
            content.getChildren().addAll(ComponentList.createComponentListTitle(i18n("settings.launcher.proxy")), proxyList);
        }
    }

    private static boolean isBrightnessOverridden() {
        return settings().getThemeAppearanceOverrides()
                .contains(org.hmcldl.setting.LauncherSettings.THEME_APPEARANCE_BRIGHTNESS_MODE);
    }

    /// Applies the chosen game download path: selects an existing game directory
    /// with that path, or creates and selects a new one.
    @FXThread
    private static void applyGameDownloadPath(String location) {
        Path path;
        try {
            path = Path.of(location).toAbsolutePath().normalize();
        } catch (IllegalArgumentException e) {
            return;
        }

        PortablePath portablePath = PortablePath.fromPath(path);

        // reuse an existing game directory entry when the path matches
        for (GameDirectory existing : GameDirectoryManager.getGameDirectories()) {
            if (existing.getPath().equals(portablePath)) {
                GameDirectoryManager.setSelectedGameDirectory(existing);
                return;
            }
        }

        GameDirectory newGameDirectory = new GameDirectory(
                GameDirectoryManager.newGameDirectoryId(),
                null,
                portablePath);

        org.hmcldl.ui.Controllers.ThrowingRunnable addAndSelect = () -> {
            if (portablePath.isAbsolute()) {
                GameDirectoryManager.addUserGameDirectory(newGameDirectory);
            } else {
                GameDirectoryManager.addLocalGameDirectory(newGameDirectory);
            }
            GameDirectoryManager.setSelectedGameDirectory(newGameDirectory);
        };

        if (portablePath.isAbsolute()) {
            if (SettingsManager.isUserGameDirectoriesReadOnly()) {
                org.hmcldl.ui.Controllers.confirmBackupAndOverwrite(
                        i18n("settings.game_directories.read_only"), addAndSelect);
                return;
            }
        } else {
            if (SettingsManager.isLocalGameDirectoriesReadOnly()) {
                org.hmcldl.ui.Controllers.confirmBackupAndOverwrite(
                        i18n("settings.game_directories.read_only"), addAndSelect);
                return;
            }
        }
        try {
            addAndSelect.run();
        } catch (Exception ignored) {
        }
    }

    @FXThread
    private boolean cleaningCache = false;

    private void clearCacheDirectory(JFXButton cleanButton) {
        if (cleaningCache) return;
        String commonDirectory = settings().getResolvedCommonDirectory();
        if (commonDirectory != null) {
            cleaningCache = true;
            cleanButton.setMinWidth(cleanButton.getWidth());
            var txt = cleanButton.getText();
            cleanButton.setText("");
            var spinner = new JFXSpinner();
            spinner.setRadius(8);
            cleanButton.setGraphic(spinner);
            Task.runAsync("Clear Cache Directory", Schedulers.io(), () -> FileUtils.cleanDirectoryQuietly(Path.of(commonDirectory, "cache")))
                    .whenComplete(Schedulers.javafx(), ignored -> {
                        cleanButton.setGraphic(null);
                        cleanButton.setText(txt);
                        cleaningCache = false;
                    }).setSignificance(Task.TaskSignificance.MINOR)
                    .start();
        }
    }
}
