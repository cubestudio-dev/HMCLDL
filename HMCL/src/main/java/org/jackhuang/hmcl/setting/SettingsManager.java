/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2020  huangyuhui <huanghongxun2008@126.com> and contributors
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
package org.jackhuang.hmcl.setting;

import com.google.gson.JsonObject;
import javafx.collections.ObservableList;
import org.jackhuang.hmcl.Metadata;
import org.jackhuang.hmcl.util.FileSaver;
import org.jackhuang.hmcl.util.gson.JsonSchema;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jackhuang.hmcl.util.i18n.I18n;
import org.jackhuang.hmcl.util.io.FileUtils;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Locale;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Owns the process-wide configuration and detached workspace settings instances.
@NotNullByDefault
public final class SettingsManager {
    /// Prevents instantiation.
    private SettingsManager() {
    }
    /// Saves deferred launcher state changes and shutdown file saver.
    public static void shutdown() {
        savePendingChanges();
        FileSaver.shutdown();
    }
    /// Saves deferred launcher state changes.
    public static void savePendingChanges() {
        if (launcherState != null && launcherState.isSavable() && launcherState.isSavePending()) {
            launcherState.setSavePending(false);
            STATE_FILE.save(launcherState);
        }
    }
    /// The local directory storing per-workspace configuration files.
    private static final Path LOCAL_CONFIG_FILES_DIRECTORY = Metadata.HMCL_LOCAL_HOME.resolve("config");
    /// The local directory storing per-workspace state files.
    private static final Path LOCAL_STATE_DIRECTORY = Metadata.HMCL_LOCAL_HOME.resolve("state");
    /// The local directory storing per-workspace cache files.
    private static final Path LOCAL_CACHE_DIRECTORY = Metadata.HMCL_LOCAL_HOME.resolve("cache");
    /// The local directory storing per-workspace private data files.
    private static final Path LOCAL_PRIVATE_DIRECTORY = Metadata.HMCL_LOCAL_HOME.resolve("private");
    /// The user directory storing shared configuration files.
    private static final Path USER_CONFIG_FILES_DIRECTORY = Metadata.HMCL_USER_HOME.resolve("config");
    /// The user directory storing shared state files.
    private static final Path USER_STATE_DIRECTORY = Metadata.HMCL_USER_HOME.resolve("state");
    /// The user directory storing shared private data files.
    private static final Path USER_PRIVATE_DIRECTORY = Metadata.HMCL_USER_HOME.resolve("private");
    /// The user settings path shared by all workspaces.
    public static final Path USER_SETTINGS_LOCATION =
            USER_CONFIG_FILES_DIRECTORY.resolve("user-settings.json");
    /// The user state path shared by all workspaces.
    public static final Path USER_STATE_LOCATION =
            USER_STATE_DIRECTORY.resolve("user-state.json");
    /// The current per-workspace launcher settings path.
    private static final Path SETTINGS_LOCATION =
            LOCAL_CONFIG_FILES_DIRECTORY.resolve("launcher-settings.json");
    /// The current per-workspace launcher state path.
    private static final Path STATE_LOCATION =
            LOCAL_STATE_DIRECTORY.resolve("launcher-state.json");
    /// The current per-workspace game directories path.
    private static final Path LOCAL_GAME_DIRECTORIES_LOCATION =
            LOCAL_CONFIG_FILES_DIRECTORY.resolve("game-directories.json");
    /// The current user game directories path.
    private static final Path USER_GAME_DIRECTORIES_LOCATION =
            USER_CONFIG_FILES_DIRECTORY.resolve("user-game-directories.json");
    /// The current per-workspace game settings path.
    private static final Path GAME_SETTINGS_LOCATION =
            LOCAL_CONFIG_FILES_DIRECTORY.resolve("game-settings.json");
    /// The per-workspace game directory file helper.
    private static final JsonSettingFile<GameDirectories> LOCAL_GAME_DIRECTORIES_FILE = new JsonSettingFile<>(
            LOCAL_GAME_DIRECTORIES_LOCATION,
            "game directories",
            GameDirectories.class,
            GameDirectories.CURRENT_SCHEMA,
            GameDirectories::new);
    /// The user game directory file helper.
    private static final JsonSettingFile<GameDirectories> USER_GAME_DIRECTORIES_FILE = new JsonSettingFile<>(
            USER_GAME_DIRECTORIES_LOCATION,
            "user game directories",
            GameDirectories.class,
            GameDirectories.CURRENT_SCHEMA,
            GameDirectories::new);
    /// The detached game settings file helper.
    private static final JsonSettingFile<GameSettingsPresets> GAME_SETTINGS_FILE = new JsonSettingFile<>(
            GAME_SETTINGS_LOCATION,
            "game settings",
            GameSettingsPresets.class,
            GameSettingsPresets.CURRENT_SCHEMA,
            GameSettingsPresets::new);
    /// The detached launcher state file helper.
    private static final JsonSettingFile<LauncherState> STATE_FILE = new JsonSettingFile<>(
            STATE_LOCATION,
            "launcher state",
            LauncherState.class,
            LauncherState.CURRENT_SCHEMA,
            LauncherState::new);
    /// The user settings file helper.
    private static final JsonSettingFile<UserSettings> USER_SETTINGS_FILE = new JsonSettingFile<>(
            USER_SETTINGS_LOCATION,
            "user settings",
            UserSettings.class,
            UserSettings.CURRENT_SCHEMA,
            UserSettings::new);
    /// The user state file helper.
    private static final JsonSettingFile<UserState> USER_STATE_FILE = new JsonSettingFile<>(
            USER_STATE_LOCATION,
            "user state",
            UserState.class,
            UserState.CURRENT_SCHEMA,
            UserState::new);
    /// The loaded per-workspace launcher settings.
    private static @UnknownNullability LauncherSettings launcherSettings;
    /// The loaded user settings instance.
    private static @UnknownNullability UserSettings userSettingsInstance;
    /// The loaded user state instance.
    private static @UnknownNullability UserState userStateInstance;
    /// The loaded per-workspace game directory file.
    private static @UnknownNullability GameDirectories localGameDirectories;
    /// The loaded user game directory file.
    private static @UnknownNullability GameDirectories userGameDirectories;
    /// The loaded detached preset store.
    private static @UnknownNullability GameSettingsPresets gameSettingsPresets;
    /// The loaded detached launcher state store.
    private static @UnknownNullability LauncherState launcherState;
    /// Whether this run appears to be using a new workspace.
    private static boolean newlyCreated;
    /// Whether root is reading a per-workspace config owned by another user.
    private static boolean ownerChanged = false;
    /// Access status for `config/launcher-settings.json`.
    private static SettingFileAccess launcherSettingsAccess = SettingFileAccess.READ_WRITE;
    /// Access status for `state/launcher-state.json`.
    private static SettingFileAccess launcherStateAccess = SettingFileAccess.READ_WRITE;
    /// Access status for local `config/game-directories.json`.
    private static SettingFileAccess localGameDirectoriesAccess = SettingFileAccess.READ_WRITE;
    /// Access status for user `config/user-game-directories.json`.
    private static SettingFileAccess userGameDirectoriesAccess = SettingFileAccess.READ_WRITE;
    /// Access status for `config/game-settings.json`.
    private static SettingFileAccess gameSettingsAccess = SettingFileAccess.READ_WRITE;
    /// Access status for `config/user-settings.json`.
    private static SettingFileAccess userSettingsAccess = SettingFileAccess.READ_WRITE;
    /// Access status for `state/user-state.json`.
    private static SettingFileAccess userStateAccess = SettingFileAccess.READ_WRITE;
    /// Returns the loaded per-workspace launcher settings.
    public static LauncherSettings settings() {
        if (launcherSettings == null) {
            throw new IllegalStateException("Configuration hasn't been loaded");
        }
        return launcherSettings;
    }
    /// Returns the loaded user settings.
    public static UserSettings userSettings() {
        if (userSettingsInstance == null) {
            throw new IllegalStateException("Configuration hasn't been loaded");
        }
        return userSettingsInstance;
    }
    /// Returns the loaded user state.
    public static UserState userState() {
        if (userStateInstance == null) {
            throw new IllegalStateException("User state hasn't been loaded");
        }
        return userStateInstance;
    }
    /// Returns the loaded per-workspace launcher state.
    public static LauncherState state() {
        if (launcherState == null) {
            throw new IllegalStateException("Launcher state hasn't been loaded");
        }
        return launcherState;
    }
    /// Returns the current per-workspace config directory path.
    public static Path localConfigDirectory() {
        return Metadata.HMCL_LOCAL_HOME;
    }
    /// Returns the current per-workspace settings path.
    public static Path settingsLocation() {
        return SETTINGS_LOCATION;
    }
    /// Returns the current per-workspace launcher state path.
    public static Path stateLocation() {
        return STATE_LOCATION;
    }
    /// Returns the current per-workspace game directories path.
    public static Path gameDirectoriesLocation() {
        return LOCAL_GAME_DIRECTORIES_LOCATION;
    }
    /// Returns the user game directories path.
    public static Path userGameDirectoriesLocation() {
        return USER_GAME_DIRECTORIES_LOCATION;
    }
    /// Returns the current per-workspace game settings path.
    public static Path gameSettingsLocation() {
        return GAME_SETTINGS_LOCATION;
    }
    /// Returns the loaded detached preset store.
    public static GameSettingsPresets gameSettingsPresets() {
        if (gameSettingsPresets == null) {
            throw new IllegalStateException("Game settings presets haven't been loaded");
        }
        return gameSettingsPresets;
    }
    /// Returns the loaded per-workspace game directory store.
    static GameDirectories localGameDirectories() {
        if (localGameDirectories == null) {
            throw new IllegalStateException("Game directories haven't been loaded");
        }
        return localGameDirectories;
    }
    /// Returns the loaded user game directory store.
    static GameDirectories userGameDirectories() {
        if (userGameDirectories == null) {
            throw new IllegalStateException("Game directories haven't been loaded");
        }
        return userGameDirectories;
    }
    /// Returns the reusable game setting presets.
    public static ObservableList<GameSettings.Preset> getGameSettings() {
        return gameSettingsPresets().getPresets();
    }
    /// Returns the game setting preset with the given ID.
    public static GameSettings.@Nullable Preset getGameSettings(@Nullable GameSettingsPresetID id) {
        return gameSettingsPresets().getPreset(id);
    }
    /// Returns the default game setting preset, creating one when needed.
    public static GameSettings.Preset getDefaultGameSettingsPresetOrCreate() {
        GameSettings.Preset setting = getGameSettings(settings().defaultGameSettingsPresetProperty().get());
        if (setting != null) {
            return setting;
        }

        if (!getGameSettings().isEmpty()) {
            setting = getGameSettings().get(0);
            settings().defaultGameSettingsPresetProperty().set(setting.idProperty().getValue());
            return setting;
        }

        setting = new GameSettings.Preset(gameSettingsPresets().newPresetId());
        setting.autoNameNumberProperty().setValue(1);
        getGameSettings().add(setting);
        settings().defaultGameSettingsPresetProperty().set(setting.idProperty().getValue());
        return setting;
    }
    /// Returns whether this run created a new per-workspace config.
    public static boolean isNewlyCreated() {
        return newlyCreated;
    }
    /// Returns whether root is reading a config owned by another user.
    public static boolean isOwnerChanged() {
        return ownerChanged;
    }
    /// Returns whether the core launcher settings cannot be safely overwritten.
    public static boolean hasReadOnlyCoreSettings() {
        return launcherSettingsAccess.blocksEditing() || launcherStateAccess.blocksEditing();
    }
    /// Returns whether game settings presets cannot be safely overwritten.
    public static boolean isGameSettingsReadOnly() {
        return gameSettingsAccess.blocksEditing();
    }
    /// Returns whether the local game directory store cannot be safely overwritten.
    public static boolean isLocalGameDirectoriesReadOnly() {
        return localGameDirectoriesAccess.blocksEditing();
    }
    /// Returns whether the user game directory store cannot be safely overwritten.
    public static boolean isUserGameDirectoriesReadOnly() {
        return userGameDirectoriesAccess.blocksEditing();
    }
    /// Returns whether the user settings store cannot be safely overwritten.
    public static boolean isUserSettingsReadOnly() {
        return userSettingsAccess.blocksEditing();
    }
    /// Backs up and overwrites `config/game-settings.json` with the currently loaded presets.
    public static void forceOverwriteGameSettings() {
        boolean installAutoSave = !gameSettingsPresets().isSavable();
        GAME_SETTINGS_FILE.backupAndOverwrite(gameSettingsPresets());
        if (installAutoSave) {
            GAME_SETTINGS_FILE.installAutoSave(gameSettingsPresets());
        }
        gameSettingsAccess = SettingFileAccess.READ_WRITE;
    }
    /// Backs up and overwrites local `config/game-directories.json` with the currently loaded game directories.
    public static void forceOverwriteLocalGameDirectories() {
        boolean installAutoSave = !localGameDirectories().isSavable();
        LOCAL_GAME_DIRECTORIES_FILE.backupAndOverwrite(localGameDirectories());
        if (installAutoSave) {
            LOCAL_GAME_DIRECTORIES_FILE.installAutoSave(localGameDirectories());
        }
        localGameDirectoriesAccess = SettingFileAccess.READ_WRITE;
    }
    /// Backs up and overwrites user `config/user-game-directories.json` with the currently loaded game directories.
    public static void forceOverwriteUserGameDirectories() {
        boolean installAutoSave = !userGameDirectories().isSavable();
        USER_GAME_DIRECTORIES_FILE.backupAndOverwrite(userGameDirectories());
        if (installAutoSave) {
            USER_GAME_DIRECTORIES_FILE.installAutoSave(userGameDirectories());
        }
        userGameDirectoriesAccess = SettingFileAccess.READ_WRITE;
    }
    /// Loads game directories and installs the save listener.
    ///
    /// @param fallbackGameDirectories the fallback store used when the local game directory file does not exist
    private static void loadGameDirectories(
            @Nullable GameDirectories fallbackGameDirectories) throws IOException {
        if (localGameDirectories != null || userGameDirectories != null) {
            throw new IllegalStateException("Game directories are already loaded");
        }

        boolean newlyCreatedLocal = !Files.exists(LOCAL_GAME_DIRECTORIES_LOCATION);
        boolean newlyCreatedUser = !Files.exists(USER_GAME_DIRECTORIES_LOCATION);
        JsonSettingFile.LoadResult<GameDirectories> userResult = USER_GAME_DIRECTORIES_FILE.load(null);
        JsonSettingFile.LoadResult<GameDirectories> localResult = LOCAL_GAME_DIRECTORIES_FILE.load(fallbackGameDirectories);

        localGameDirectories = localResult.value();
        localGameDirectories.setUserFile(false);
        localGameDirectories.setNewlyCreated(newlyCreatedLocal);

        userGameDirectories = userResult.value();
        userGameDirectories.setUserFile(true);
        userGameDirectories.setNewlyCreated(newlyCreatedUser);

        if (localGameDirectories.isSavable()) {
            LOCAL_GAME_DIRECTORIES_FILE.installAutoSave(localGameDirectories);
        }
        if (userGameDirectories.isSavable()) {
            USER_GAME_DIRECTORIES_FILE.installAutoSave(userGameDirectories);
        }

        if (newlyCreatedLocal && localGameDirectories.isSavable()) {
            LOCAL_GAME_DIRECTORIES_FILE.saveSync(localGameDirectories);
        }
        if (newlyCreatedUser && userGameDirectories.isSavable()) {
            USER_GAME_DIRECTORIES_FILE.saveSync(userGameDirectories);
        }

        localGameDirectoriesAccess = localResult.access();
        userGameDirectoriesAccess = userResult.access();
    }
    /// Loads game settings presets and installs the save listener.
    ///
    /// @param fallbackGameSettingsPresets the fallback store used when the preset file does not exist
    /// @return the preset file access status
    private static SettingFileAccess loadGameSettingsPresets(
            @Nullable GameSettingsPresets fallbackGameSettingsPresets) throws IOException {
        if (gameSettingsPresets != null) {
            throw new IllegalStateException("Game settings presets are already loaded");
        }

        boolean newlyCreated = !Files.exists(GAME_SETTINGS_LOCATION);
        JsonSettingFile.LoadResult<GameSettingsPresets> result =
                GAME_SETTINGS_FILE.load(fallbackGameSettingsPresets);
        gameSettingsPresets = result.value();
        if (gameSettingsPresets.isSavable()) {
            GAME_SETTINGS_FILE.installAutoSave(gameSettingsPresets);
        }
        normalizeGameSettingsPresets();

        if (newlyCreated && gameSettingsPresets.isSavable()) {
            GAME_SETTINGS_FILE.saveSync(gameSettingsPresets);
        }

        return result.access();
    }
    /// Ensures there is a valid default game settings preset.
    private static void normalizeGameSettingsPresets() {
        if (getGameSettings().isEmpty()) {
            getDefaultGameSettingsPresetOrCreate();
        } else if (getGameSettings(settings().defaultGameSettingsPresetProperty().get()) == null) {
            settings().defaultGameSettingsPresetProperty().set(getGameSettings().get(0).idProperty().getValue());
        }
    }
    /// Loads launcher state and installs the save listener.
    ///
    /// @param fallbackLauncherState the fallback state used when the launcher state file does not exist
    /// @return the launcher state file access status
    private static SettingFileAccess loadLauncherState(
            @Nullable LauncherState fallbackLauncherState) throws IOException {
        if (launcherState != null) {
            throw new IllegalStateException("Launcher state is already loaded");
        }

        boolean newlyCreated = !Files.exists(STATE_LOCATION);
        JsonSettingFile.LoadResult<LauncherState> result =
                STATE_FILE.load(fallbackLauncherState);
        launcherState = result.value();
        if (launcherState.isSavable()) {
            STATE_FILE.installAutoSave(launcherState);
        }

        if (newlyCreated && launcherState.isSavable()) {
            STATE_FILE.saveSync(launcherState);
        }

        return result.access();
    }
    /// Checks whether root is reading a config path owned by another user.
    private static void checkOwner(Path location) {
        if (!Files.exists(location)) {
            return;
        }

        try {
            if (OperatingSystem.CURRENT_OS != OperatingSystem.WINDOWS
                    && "root".equals(System.getProperty("user.name"))
                    && !"root".equals(Files.getOwner(location).getName())) {
                ownerChanged = true;
            }
        } catch (IOException e) {
            LOG.warning("Failed to get owner");
        }
    }
    /// Checks that the given config path is writable.
    private static void checkWritable(Path location) throws IOException {
        if (!Files.isWritable(location)) {
            if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS
                    && location.getFileSystem() == FileSystems.getDefault()
                    && location.toFile().canWrite()) {
                LOG.warning("Launcher config path " + location + " is not writable, but it seems to be a Samba share or OpenJDK bug");
                // There are some serious problems with the implementation of Samba or OpenJDK
                throw new SambaException();
            } else {
                // the config cannot be saved
                // throw up the error now to prevent further data loss
                throw new IOException("Launcher config path " + location + " is not writable");
            }
        }
    }

    /// Loads configs, installs save listeners, and applies process-wide settings.
    public static void init() throws IOException {
        if (launcherSettings != null) {
            throw new IllegalStateException("Configuration is already loaded");
        }

        checkLocalConfigOwner();
        newlyCreated = isNewWorkspace();

        LoadedLauncherSettings loadedLauncherSettings = loadLauncherSettings();
        launcherSettings = loadedLauncherSettings.settings();
        launcherSettingsAccess = loadedLauncherSettings.access();

        userSettingsAccess = loadUserSettings();
        userStateAccess = loadUserState();

        Locale.setDefault(settings().languageProperty().get().getLocale());
        I18n.setLocale(launcherSettings.languageProperty().get());
        LOG.setLogRetention(userSettings().logRetentionProperty().get());
        loadGameDirectories(null);
        gameSettingsAccess = loadGameSettingsPresets(null);
        launcherStateAccess = loadLauncherState(null);

        if (Files.exists(Metadata.HMCL_LOCAL_HOME)) {
            checkWritable(Metadata.HMCL_LOCAL_HOME);
        }

        if (launcherSettings.isSavable()) {
            launcherSettings.addListener(source -> {
                if (launcherSettings.isBackupOnNextSave()) {
                    launcherSettings.setBackupOnNextSave(false);
                    SettingFileUtils.backupInvalidConfig(SETTINGS_LOCATION);
                }
                FileSaver.save(SETTINGS_LOCATION, launcherSettings.toJson());
            });
        }
    }

    /// Loads the current per-workspace settings file.
    private static LoadedLauncherSettings loadLauncherSettings() throws IOException {
        if (Files.exists(SETTINGS_LOCATION)) {
            JsonObject jsonObject;
            try {
                jsonObject = JsonUtils.fromJsonFile(SETTINGS_LOCATION, JsonObject.class);
            } catch (Exception e) {
                LOG.warning("Failed to read launcher settings file: " + SETTINGS_LOCATION, e);

                LauncherSettings settings = new LauncherSettings();
                settings.setBackupOnNextSave(true);
                return new LoadedLauncherSettings(settings, SettingFileAccess.READ_WRITE);
            }

            if (jsonObject == null) {
                LOG.warning("Launcher settings file is empty: " + SETTINGS_LOCATION);

                return new LoadedLauncherSettings(new LauncherSettings(), SettingFileAccess.READ_WRITE);
            }

            JsonSchema.CompatibilityResult schemaResult =
                    JsonSchema.check(jsonObject, LauncherSettings.CURRENT_SCHEMA);
            switch (schemaResult.status()) {
                case MISSING -> LOG.warning("Missing schema in launcher settings file: " + SETTINGS_LOCATION);
                case INVALID -> LOG.warning("Invalid schema in launcher settings file: "
                        + SETTINGS_LOCATION + ", Actual: " + schemaResult.invalidValue());
                case UNPARSEABLE -> LOG.warning("Unparseable schema in launcher settings file: "
                        + SETTINGS_LOCATION + ", Actual: " + schemaResult.actual());
                case UNEXPECTED_ID -> LOG.warning("Unexpected launcher settings file schema. Expected: "
                        + LauncherSettings.CURRENT_SCHEMA + ", Actual: " + schemaResult.actual());
                case UNSUPPORTED_MAJOR, READ_ONLY_PRESERVE_SCHEMA -> LOG.warning("Unsupported launcher settings file schema. Expected: "
                        + LauncherSettings.CURRENT_SCHEMA + ", Actual: " + schemaResult.actual());
                case READ_WRITE, READ_WRITE_PRESERVE_SCHEMA -> {
                }
            }
            if (!schemaResult.readable()) {
                LauncherSettings settings = new LauncherSettings();
                settings.setSavable(false);
                return new LoadedLauncherSettings(settings, SettingFileAccess.UNREADABLE);
            }

            try {
                LauncherSettings settings = LauncherSettings.fromJson(jsonObject);
                if (settings == null) {
                    settings = new LauncherSettings();
                    settings.setSavable(false);
                    return new LoadedLauncherSettings(settings, SettingFileAccess.UNREADABLE);
                }

                if (!schemaResult.preserveSchema() && !LauncherSettings.CURRENT_SCHEMA.equals(settings.schemaProperty().get())) {
                    settings.schemaProperty().set(LauncherSettings.CURRENT_SCHEMA);
                }

                settings.setSavable(schemaResult.allowSave());
                return new LoadedLauncherSettings(settings, schemaResult.allowSave()
                        ? SettingFileAccess.READ_WRITE
                        : SettingFileAccess.READ_ONLY);
            } catch (Exception e) {
                LOG.warning("Failed to parse launcher settings file: " + SETTINGS_LOCATION, e);
                LauncherSettings settings = new LauncherSettings();
                settings.setBackupOnNextSave(true);
                return new LoadedLauncherSettings(settings, SettingFileAccess.READ_WRITE);
            }
        }

        return new LoadedLauncherSettings(new LauncherSettings(), SettingFileAccess.READ_WRITE);
    }

    /// Returns whether the current workspace already has any local configuration footprint.
    private static boolean isNewWorkspace() {
        return !(Files.exists(SETTINGS_LOCATION)
                || Files.exists(STATE_LOCATION)
                || Files.exists(LOCAL_GAME_DIRECTORIES_LOCATION)
                || Files.exists(GAME_SETTINGS_LOCATION));
    }

    /// Checks whether root is reading per-workspace config data owned by another user.
    private static void checkLocalConfigOwner() {
        checkOwner(Metadata.HMCL_LOCAL_HOME);
        checkOwner(SETTINGS_LOCATION);
        checkOwner(STATE_LOCATION);
        checkOwner(LOCAL_GAME_DIRECTORIES_LOCATION);
        checkOwner(GAME_SETTINGS_LOCATION);
    }

    /// Loads user settings and installs the save listener.
    ///
    /// @return the user settings file access status
    private static SettingFileAccess loadUserSettings() throws IOException {
        if (userSettingsInstance != null) {
            throw new IllegalStateException("User settings are already loaded");
        }

        JsonSettingFile.LoadResult<UserSettings> result = USER_SETTINGS_FILE.load(null);
        userSettingsInstance = result.value();
        if (userSettingsInstance.isSavable()) {
            USER_SETTINGS_FILE.installAutoSave(userSettingsInstance);
        }

        return result.access();
    }

    /// Loads user state and installs the save listener.
    ///
    /// @return the user state file access status
    private static SettingFileAccess loadUserState() throws IOException {
        if (userStateInstance != null) {
            throw new IllegalStateException("User state is already loaded");
        }

        JsonSettingFile.LoadResult<UserState> result = USER_STATE_FILE.load(null);
        userStateInstance = result.value();
        if (userStateInstance.isSavable()) {
            USER_STATE_FILE.installAutoSave(userStateInstance);
        }

        if (!Files.exists(USER_STATE_LOCATION) && userStateInstance.isSavable()) {
            USER_STATE_FILE.saveSync(userStateInstance);
        }

        return result.access();
    }

    /// Result of loading per-workspace launcher settings.
    ///
    /// @param settings the loaded launcher settings
    /// @param access the settings file access status
    private record LoadedLauncherSettings(LauncherSettings settings, SettingFileAccess access) {
    }
}
