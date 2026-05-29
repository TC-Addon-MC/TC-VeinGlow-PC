package com.tcveinminer.api.addon;

/**
 * SPI interface for external TC VeinGlow addons.
 * <p>
 * To create an addon:
 * <ol>
 *   <li>Implement this interface in your mod.</li>
 *   <li>Create the file
 *       {@code META-INF/services/com.tcveinminer.api.addon.VeinMinerAddon}
 *       containing the fully-qualified class name of your implementation.</li>
 *   <li>Use {@link AddonContext} to register strategies, capabilities, etc.</li>
 * </ol>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * public class MyAddon implements VeinMinerAddon {
 *     @Override public String getAddonId() { return "myaddon:vein-extension"; }
 *
 *     @Override public void onAddonInit(AddonContext ctx) {
 *         ctx.registerMiningStrategy(new MyCustomStrategy());
 *         ctx.events().BLOCK_BREAK_PRE.register(e -> EventResult.PASS);
 *     }
 * }
 * }</pre>
 *
 * This interface is 100% loader-agnostic. Addon JARs can target Fabric, NeoForge,
 * or Forge without changing any addon code.
 */
public interface VeinMinerAddon {

    /** Unique ID for this addon. Use "modid:addon-name" format. */
    String getAddonId();

    /**
     * Minimum API version required.
     * TC VeinGlow will skip addons that require a newer API version.
     * Current API version: {@code 1}.
     */
    default int getRequiredApiVersion() { return 1; }

    /**
     * Called once after TC VeinGlow has fully initialized.
     * Register all strategies, capabilities, event listeners here.
     *
     * @param context the injection context — all registration goes through here.
     */
    void onAddonInit(AddonContext context);

    /** Called when the server shuts down. Override to release resources. */
    default void onAddonShutdown() {}
}
