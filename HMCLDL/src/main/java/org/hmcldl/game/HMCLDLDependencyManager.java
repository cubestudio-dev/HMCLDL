/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2026 huangyuhui <huanghongxun2008@126.com> and contributors
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
package org.hmcldl.game;

import org.hmcldl.download.DefaultCacheRepository;
import org.hmcldl.download.DefaultDependencyManager;
import org.hmcldl.download.DownloadProvider;
import org.jetbrains.annotations.NotNullByDefault;

/// Provides HMCLDL-specific game builders for an HMCLDL game repository.
@NotNullByDefault
public class HMCLDLDependencyManager extends DefaultDependencyManager {
    /// Creates a dependency manager for a repository and download context.
    ///
    /// @param repository       the associated game repository
    /// @param downloadProvider the remote download provider
    /// @param cacheRepository  the artifact cache
    public HMCLDLDependencyManager(
            HMCLDLGameRepository repository,
            DownloadProvider downloadProvider,
            DefaultCacheRepository cacheRepository) {
        super(repository, downloadProvider, cacheRepository);
    }

    /// {@inheritDoc}
    @Override
    public HMCLDLGameRepository getGameRepository() {
        return (HMCLDLGameRepository) super.getGameRepository();
    }

    /// {@inheritDoc}
    @Override
    public HMCLDLGameBuilder newGameBuilder(GameInstanceID instanceId) {
        GameInstanceManifest initialManifest = new GameInstanceManifest(instanceId);
        DefaultGameRepositoryDraft draft = openGameBuilderDraft(initialManifest, null);
        return new HMCLDLGameBuilder(this, instanceId, null, draft, initialManifest);
    }

    /// {@inheritDoc}
    @Override
    public HMCLDLGameBuilder newGameBuilder(GameInstance instance) {
        validateGameInstance(instance);
        HMCLDLGameInstance updateTarget = (HMCLDLGameInstance) instance;

        GameInstanceManifest initialManifest = new GameInstanceManifest(updateTarget.getId());
        DefaultGameRepositoryDraft draft = openGameBuilderDraft(initialManifest, updateTarget);
        return new HMCLDLGameBuilder(
                this, updateTarget.getId(), updateTarget, draft, initialManifest);
    }
}
