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
package org.hmcldl.ui;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.validation.base.ValidatorBase;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.stage.*;
import javafx.util.Duration;
import org.hmcldl.Launcher;
import org.hmcldl.Metadata;
import org.hmcldl.java.JavaRuntime;
import org.hmcldl.setting.*;
import org.hmcldl.task.Schedulers;
import org.hmcldl.task.Task;
import org.hmcldl.task.TaskExecutor;
import org.hmcldl.ui.animation.ContainerAnimations;
import org.hmcldl.ui.animation.Motion;
import org.hmcldl.ui.construct.*;
import org.hmcldl.ui.construct.MessageDialogPane.MessageType;
import org.hmcldl.ui.decorator.Decorator;
import org.hmcldl.ui.main.LauncherSettingsPage;
import org.hmcldl.ui.main.RootPage;
import org.hmcldl.util.*;
import org.hmcldl.util.i18n.I18n;
import org.hmcldl.util.io.FileUtils;
import org.hmcldl.util.platform.Architecture;
import org.hmcldl.util.platform.OperatingSystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static org.hmcldl.setting.SettingsManager.settings;
import static org.hmcldl.setting.SettingsManager.state;
import static org.hmcldl.setting.SettingsManager.userState;
import static org.hmcldl.util.i18n.I18n.i18n;
import static org.hmcldl.util.logging.Logger.LOG;

public final class Controllers {
    public static final String JAVA_VERSION_TIP = "javaVersion";
    public static final String JAVA_INTERPRETED_MODE_TIP = "javaInterpretedMode";
    public static final String SOFTWARE_RENDERING = "softwareRendering";

    private static Lazy<RootPage> rootPage = new Lazy<>(RootPage::new);
    /// The coordinator for the main window's scene graph and navigation stack.
    private static @Nullable Decorator decorator;
    private static LauncherSettingsPage settingsPage;

    private Controllers() {
    }

    /// Action used by confirmation dialogs that may fail before the confirmed operation is complete.
    @FunctionalInterface
    public interface ThrowingRunnable {
        /// Runs the confirmed action.
        ///
        /// @throws Exception if the action fails
        void run() throws Exception;
    }

    /// Returns the stage currently attached to the main-window decorator.
    ///
    /// @return the current stage, or `null` before initialization or after detachment
    public static @Nullable Stage getStage() {
        @Nullable Decorator currentDecorator = decorator;
        return currentDecorator == null ? null : currentDecorator.getStage();
    }

    @FXThread
    public static RootPage getRootPage() {
        return rootPage.get();
    }

    @FXThread
    public static LauncherSettingsPage getSettingsPage() {
        if (settingsPage == null) {
            settingsPage = new LauncherSettingsPage();
        }
        return settingsPage;
    }

    @FXThread
    public static void prepareSettingsPage() {
        if (settingsPage == null) {
            LOG.info("Prepare the settings page");
            settingsPage = FXUtils.prepareNode(new LauncherSettingsPage());
        }
    }

    /// Returns the initialized main-window decorator.
    ///
    /// @return the application-wide main-window decorator
    @FXThread
    public static Decorator getDecorator() {
        return Objects.requireNonNull(decorator, "Main window is not initialized");
    }

    /// Releases stage-specific listeners and retained window ownership before application shutdown.
    public static void onApplicationStop() {
        if (decorator != null) {
            decorator.detachStage();
        }
    }

    /// Initializes the main application stage, scene graph, and background services.
    ///
    /// @param stage the primary application stage, which must not have been shown
    public static void initialize(Stage stage) {
        LOG.info("Start initializing application");


        if (System.getProperty("prism.lcdtext") == null) {
            @Nullable String fontAntiAliasing = SettingsManager.userSettings().fontAntiAliasingProperty().get();
            if ("lcd".equalsIgnoreCase(fontAntiAliasing)) {
                LOG.info("Enable sub-pixel antialiasing");
                System.getProperties().put("prism.lcdtext", "true");
            } else if ("gray".equalsIgnoreCase(fontAntiAliasing)
                    || OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS && Screen.getPrimary().getOutputScaleX() > 1) {
                LOG.info("Disable sub-pixel antialiasing");
                System.getProperties().put("prism.lcdtext", "false");
            }
        }

        stage.setOnCloseRequest(e -> Launcher.stopApplication());

        decorator = new Decorator(getRootPage());
        Scene mainScene = decorator.attachStage(stage);

        if (settings().commonDirectoryTypeProperty().get() == EnumCommonDirectory.CUSTOM &&
                !FileUtils.canCreateDirectory(settings().getResolvedCommonDirectory())) {
            settings().commonDirectoryTypeProperty().set(EnumCommonDirectory.DEFAULT);
            dialog(i18n("launcher.cache_directory.invalid"));
        }

        StyleSheets.init(mainScene);

        FXUtils.setIcon(stage);
        stage.setTitle(Metadata.FULL_TITLE);

        if (!Architecture.SYSTEM_ARCH.isX86() && SettingsManager.userState().platformPromptVersionProperty().get() < 1) {
            Runnable continueAction = () -> {
                UserState userState = userState();
                userState.platformPromptVersionProperty().set(1);
            };

            if (OperatingSystem.CURRENT_OS == OperatingSystem.MACOS && Architecture.SYSTEM_ARCH == Architecture.ARM64) {
                continueAction.run();
            } else if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS && Architecture.SYSTEM_ARCH == Architecture.ARM64) {
                Controllers.dialog(i18n("fatal.unsupported_platform.windows_arm64"), null, MessageType.INFO, continueAction);
            } else if (OperatingSystem.CURRENT_OS == OperatingSystem.LINUX &&
                    (Architecture.SYSTEM_ARCH == Architecture.LOONGARCH64
                            || Architecture.SYSTEM_ARCH == Architecture.LOONGARCH64_OW
                            || Architecture.SYSTEM_ARCH == Architecture.MIPS64EL)) {
                Controllers.dialog(i18n("fatal.unsupported_platform.loongarch"), null, MessageType.INFO, continueAction);
            } else {
                Controllers.dialog(i18n("fatal.unsupported_platform"), null, MessageType.WARNING, continueAction);
            }
        }

        if (JavaRuntime.CURRENT_VERSION < Metadata.MINIMUM_SUPPORTED_JAVA_VERSION) {
            Number shownTipVersion = null;
            try {
                shownTipVersion = (Number) state().getShownTips().get(JAVA_VERSION_TIP);
            } catch (ClassCastException e) {
                LOG.warning("Invalid type for shown tips key: " + JAVA_VERSION_TIP, e);
            }
            if (shownTipVersion == null || shownTipVersion.intValue() < Metadata.MINIMUM_SUPPORTED_JAVA_VERSION) {
                MessageDialogPane.Builder builder = new MessageDialogPane.Builder(i18n("fatal.deprecated_java_version"), null, MessageType.WARNING);
                String downloadLink = Metadata.getSuggestedJavaDownloadLink();
                if (downloadLink != null)
                    builder.addHyperLink(
                            i18n("fatal.deprecated_java_version.download_link", Metadata.RECOMMENDED_JAVA_VERSION),
                            downloadLink
                    );
                Controllers.dialog(builder
                        .ok(() -> state().getShownTips().put(JAVA_VERSION_TIP, Metadata.MINIMUM_SUPPORTED_JAVA_VERSION))
                        .build());
            }
        }

        // Check whether JIT is enabled in the current environment
        if (!JavaRuntime.CURRENT_JIT_ENABLED && !Boolean.TRUE.equals(state().getShownTips().get(JAVA_INTERPRETED_MODE_TIP))) {
            Controllers.dialog(new MessageDialogPane.Builder(i18n("warning.java_interpreted_mode"), i18n("message.warning"), MessageType.WARNING)
                    .ok(null)
                    .addCancel(i18n("button.do_not_show_again"), () ->
                            state().getShownTips().put(JAVA_INTERPRETED_MODE_TIP, true))
                    .build());
        }

        // Check whether hardware acceleration is enabled
        if (!FXUtils.GPU_ACCELERATION_ENABLED && !Boolean.TRUE.equals(state().getShownTips().get(SOFTWARE_RENDERING))) {
            Controllers.dialog(new MessageDialogPane.Builder(i18n("warning.software_rendering"), i18n("message.warning"), MessageType.WARNING)
                    .ok(null)
                    .addCancel(i18n("button.do_not_show_again"), () ->
                            state().getShownTips().put(SOFTWARE_RENDERING, true))
                    .build());
        }

        if (SettingsManager.userState().agreementVersionProperty().get() < 1) {
            JFXDialogLayout agreementPane = new JFXDialogLayout();
            agreementPane.setHeading(new Label(i18n("launcher.agreement")));
            agreementPane.setBody(new Label(i18n("launcher.agreement.hint")));
            JFXHyperlink agreementLink = new JFXHyperlink(i18n("launcher.agreement"));
            agreementLink.setExternalLink(Metadata.EULA_URL);
            JFXButton yesButton = new JFXButton(i18n("launcher.agreement.accept"));
            yesButton.getStyleClass().add("dialog-accept");
            yesButton.setOnAction(e -> {
                UserState userState = userState();
                userState.agreementVersionProperty().set(1);
                agreementPane.fireEvent(new DialogCloseEvent());
            });
            JFXButton noButton = new JFXButton(i18n("launcher.agreement.decline"));
            noButton.getStyleClass().add("dialog-cancel");
            noButton.setOnAction(e -> javafx.application.Platform.exit());
            agreementPane.setActions(agreementLink, yesButton, noButton);
            Controllers.dialog(agreementPane);
        }

    }

    public static void dialog(Region content) {
        if (decorator != null)
            decorator.showDialog(content);
    }

    public static void dialog(String text) {
        dialog(text, null);
    }

    public static void dialog(String text, String title) {
        dialog(text, title, MessageType.INFO);
    }

    public static void dialog(String text, String title, MessageType type) {
        dialog(text, title, type, null);
    }

    public static void dialog(String text, String title, MessageType type, Runnable ok) {
        dialog(new MessageDialogPane.Builder(text, title, type).ok(ok).build());
    }

    public static void confirm(String text, String title, Runnable yes, Runnable no) {
        confirm(text, title, MessageType.QUESTION, yes, no);
    }

    public static void confirm(String text, String title, MessageType type, Runnable yes, Runnable no) {
        dialog(new MessageDialogPane.Builder(text, title, type).yesOrNo(yes, no).build());
    }

    /// Shows a warning that confirms backing up a read-only settings file before overwriting it.
    ///
    /// @param text      the file-specific read-only warning
    /// @param overwrite the action that backs up and overwrites the file
    public static void confirmBackupAndOverwrite(String text, ThrowingRunnable overwrite) {
        dialog(new MessageDialogPane.Builder(
                text + "\n\n" + i18n("settings.file.force_write.confirm"),
                i18n("message.warning"),
                MessageType.WARNING)
                .addAction(i18n("settings.file.force_write"), () -> {
                    try {
                        overwrite.run();
                    } catch (Exception e) {
                        LOG.warning("Failed to force overwrite settings file", e);
                        dialog(i18n("message.failed") + "\n\n" + StringUtils.getStackTrace(e),
                                i18n("message.error"),
                                MessageType.ERROR);
                    }
                })
                .addCancel(null)
                .build());
    }

    public static void confirmAction(String text, String title, MessageType type, ButtonBase actionButton) {
        dialog(new MessageDialogPane.Builder(text, title, type).actionOrCancel(actionButton, null).build());
    }

    public static void confirmAction(String text, String title, MessageType type, ButtonBase actionButton, Runnable cancel) {
        dialog(new MessageDialogPane.Builder(text, title, type).actionOrCancel(actionButton, cancel).build());
    }

    public static void confirmWithCountdown(String text, String title, int seconds, MessageType messageType,
                                            @Nullable Runnable ok, @Nullable Runnable cancel) {
        if (seconds <= 0)
            throw new IllegalArgumentException("Seconds must be greater than 0");

        JFXButton btnOk = new JFXButton(i18n("button.ok"));
        btnOk.getStyleClass().add(messageType == MessageType.WARNING || messageType == MessageType.ERROR
                ? "dialog-error"
                : "dialog-accept");

        if (ok != null)
            btnOk.setOnAction(e -> ok.run());
        btnOk.setDisable(true);

        KeyFrame[] keyFrames = new KeyFrame[seconds + 1];
        for (int i = 0; i < seconds; i++) {
            keyFrames[i] = new KeyFrame(Duration.seconds(i),
                    new KeyValue(btnOk.textProperty(), i18n("button.ok.countdown", seconds - i)));
        }
        keyFrames[seconds] = new KeyFrame(Duration.seconds(seconds),
                new KeyValue(btnOk.textProperty(), i18n("button.ok")),
                new KeyValue(btnOk.disableProperty(), false));

        Timeline timeline = new Timeline(keyFrames);
        confirmAction(text, title, messageType, btnOk, () -> {
            timeline.stop();
            if (cancel != null)
                cancel.run();
        });
        timeline.play();
    }

    public static void dialogLater(Region content) {
        if (decorator != null)
            decorator.showDialogLater(content);
    }

    public static CompletableFuture<String> prompt(String title, FutureCallback<String> onResult) {
        return prompt(title, onResult, "");
    }

    public static CompletableFuture<String> prompt(String title, FutureCallback<String> onResult, String initialValue, ValidatorBase... validators) {
        InputDialogPane pane = new InputDialogPane(title, initialValue, onResult, validators);
        dialog(pane);
        return pane.getCompletableFuture();
    }

    public static CompletableFuture<List<PromptDialogPane.Builder.Question<?>>> prompt(PromptDialogPane.Builder builder) {
        PromptDialogPane pane = new PromptDialogPane(builder);
        dialog(pane);
        return pane.getCompletableFuture();
    }

    public static TaskExecutorDialogPane taskDialog(TaskExecutor executor, String title, @NotNull TaskCancellationAction onCancel) {
        TaskExecutorDialogPane pane = new TaskExecutorDialogPane(onCancel);
        pane.setTitle(title);
        pane.setExecutor(executor);
        dialog(pane);
        return pane;
    }

    public static TaskExecutorDialogPane taskDialog(Task<?> task, String title, @NotNull TaskCancellationAction onCancel) {
        TaskExecutor executor = task.executor();
        TaskExecutorDialogPane pane = taskDialog(executor, title, onCancel);
        executor.start();
        return pane;
    }

    public static void navigate(Node node) {
        decorator.navigate(node, ContainerAnimations.NAVIGATION, Motion.SHORT4, Motion.EASE);
    }

    public static void navigateForward(Node node) {
        decorator.navigate(node, ContainerAnimations.FORWARD, Motion.SHORT4, Motion.EASE);
    }

    public static void showToast(String content) {
        decorator.showToast(content);
    }

    /// Shows `directoryChooser` with the current main window as its owner.
    ///
    /// @param directoryChooser the chooser to show
    /// @return the selected directory, or `null` if the chooser is cancelled
    public static @Nullable Path showDialog(DirectoryChooser directoryChooser) {
        return FileUtils.toPath(directoryChooser.showDialog(getStage()));
    }

    /// Shows `fileChooser` for opening one file with the current main window as its owner.
    ///
    /// @param fileChooser the chooser to show
    /// @return the selected file, or `null` if the chooser is cancelled
    public static @Nullable Path showOpenDialog(FileChooser fileChooser) {
        return FileUtils.toPath(fileChooser.showOpenDialog(getStage()));
    }

    /// Shows `fileChooser` for saving one file with the current main window as its owner.
    ///
    /// @param fileChooser the chooser to show
    /// @return the selected file, or `null` if the chooser is cancelled
    public static @Nullable Path showSaveDialog(FileChooser fileChooser) {
        return FileUtils.toPath(fileChooser.showSaveDialog(getStage()));
    }

    /// Shows `fileChooser` for opening multiple files with the current main window as its owner.
    ///
    /// @param fileChooser the chooser to show
    /// @return the selected files, or `null` if the chooser is cancelled
    public static @Nullable List<Path> showOpenMultipleDialog(FileChooser fileChooser) {
        return FileUtils.toPaths(fileChooser.showOpenMultipleDialog(getStage()));
    }

    public static void onHyperlinkAction(String href) {
        FXUtils.openLink(href);
    }

    public static void openUriOrCopy(@Nullable URI uri) {
        if (uri == null) return;
        openUriOrCopy(uri.toString());
    }

    public static void openUriOrCopy(@Nullable String uri) {
        if (uri == null) return;
        var dialog = new MessageDialogPane.Builder(
                i18n("web.open_in_browser", uri),
                i18n("message.confirm"),
                MessageDialogPane.MessageType.QUESTION
        )
                .addAction(i18n("button.copy"), () -> FXUtils.copyText(uri))
                .yesOrNo(() -> FXUtils.openLink(uri), null)
                .build();
        dialog(dialog);
    }

    public static boolean isStopped() {
        return decorator == null;
    }

    /// Releases controller-owned pages, window state, and JavaFX helper resources.
    public static void shutdown() {
        onApplicationStop();
        rootPage = null;
        settingsPage = null;
        decorator = null;

        FXUtils.shutdown();
    }

    public static final boolean AUTO_TRIM_HEAP = !"false".equalsIgnoreCase(System.getenv("HMCLDL_AUTO_TRIM_HEAP"));

    public static void trimHeap() {
        if (AUTO_TRIM_HEAP) {
            System.gc();
        }
    }
}
