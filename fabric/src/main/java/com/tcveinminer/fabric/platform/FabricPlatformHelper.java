package com.tcveinminer.fabric.platform;

import com.tcveinminer.platform.PlatformHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Fabric implementation of {@link PlatformHelper}.
 * <p>
 * Registered via:
 * {@code fabric/src/main/resources/META-INF/services/com.tcveinminer.platform.PlatformHelper}
 */
public final class FabricPlatformHelper implements PlatformHelper {

    @Override
    public Environment getEnvironment() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT
                ? Environment.CLIENT
                : Environment.SERVER;
    }

    @Override
    public boolean isDedicatedServer() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
    }

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public String getModVersion(String modId) {
        return FabricLoader.getInstance()
                .getModContainer(modId)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }

    @Override
    public boolean isOperator(ServerPlayerEntity player) {
        return player.hasPermissionLevel(2);
    }
}
