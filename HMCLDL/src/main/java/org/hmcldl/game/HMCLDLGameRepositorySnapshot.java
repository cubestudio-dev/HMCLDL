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

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Collection;

/// HMCLDL repository snapshot, parallel to [HMCLDLGameInstance] in the instance hierarchy.
@NotNullByDefault
public class HMCLDLGameRepositorySnapshot extends DefaultGameRepositorySnapshot {
    /// Creates an empty unsealed HMCLDL snapshot.
    ///
    /// @param repository the owning repository
    /// @param layout     the HMCLDL layout for this snapshot
    public HMCLDLGameRepositorySnapshot(HMCLDLGameRepository repository, HMCLDLGameRepositoryLayout layout) {
        super(repository, layout);
    }

    @Override
    public HMCLDLGameRepository getRepository() {
        return (HMCLDLGameRepository) super.getRepository();
    }

    @Override
    public HMCLDLGameRepositoryLayout getLayout() {
        return (HMCLDLGameRepositoryLayout) super.getLayout();
    }

    @Override
    protected HMCLDLGameRepositorySnapshot newEmpty() {
        return new HMCLDLGameRepositorySnapshot(getRepository(), getLayout());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<HMCLDLGameInstance> getInstances() {
        return (Collection<HMCLDLGameInstance>) super.getInstances();
    }
}
