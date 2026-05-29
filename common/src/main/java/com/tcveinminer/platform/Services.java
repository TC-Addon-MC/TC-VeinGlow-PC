package com.tcveinminer.platform;

import java.util.ServiceLoader;

/**
 * Bootstrap for the {@link PlatformHelper} service.
 * <p>
 * Usage: {@code Services.PLATFORM().getConfigDir()}.
 * <p>
 * The correct implementation is discovered via Java's
 * {@link ServiceLoader} from the active loader module's
 * {@code META-INF/services/com.tcveinminer.platform.PlatformHelper} file.
 * This means the common module has zero loader coupling at compile time.
 */
public final class Services {

    private static final PlatformHelper PLATFORM = load();

    private static PlatformHelper load() {
        return ServiceLoader.load(PlatformHelper.class)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "[TC VeinGlow] No PlatformHelper implementation found! " +
                        "Ensure the correct loader module (fabric/neoforge/forge) is on the classpath."
                ));
    }

    public static PlatformHelper PLATFORM() {
        return PLATFORM;
    }

    private Services() {}
}
