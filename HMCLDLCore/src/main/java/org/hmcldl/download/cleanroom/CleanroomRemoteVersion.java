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
package org.hmcldl.download.cleanroom;

import org.hmcldl.download.DefaultDependencyManager;
import org.hmcldl.download.ComponentRemoteVersion;
import org.hmcldl.game.GameComponentType;
import org.hmcldl.game.GameInstanceManifest;
import org.hmcldl.game.GameInstancePatch;
import org.hmcldl.task.Task;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

public class CleanroomRemoteVersion extends ComponentRemoteVersion {
    public CleanroomRemoteVersion(String gameVersion, String selfVersion, Instant releaseDate, List<String> url) {
        super(GameComponentType.CLEANROOM, gameVersion, selfVersion, releaseDate, url);
    }

    @Override
    public Task<GameInstancePatch> getInstallTask(DefaultDependencyManager dependencyManager, GameInstanceManifest baseManifest, Path modsDirectory) {
        return new CleanroomInstallTask(dependencyManager, baseManifest, this);
    }
}
