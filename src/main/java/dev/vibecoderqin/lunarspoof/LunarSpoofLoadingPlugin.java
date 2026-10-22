package dev.vibecoderqin.lunarspoof;

import dev.vibecoderqin.lunarspoof.transform.BrandAndChannelTransformer;
import net.minecraftforge.fml.relauncher.FMLCorePlugin;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

/** Coremod entry point for Minecraft Forge 1.8.9. */
@IFMLLoadingPlugin.Name("LunarSpoof-1.8.9")
@IFMLLoadingPlugin.MCVersion("1.8.9")
@IFMLLoadingPlugin.TransformerExclusions({"dev.vibecoderqin.lunarspoof"})
public final class LunarSpoofLoadingPlugin extends FMLCorePlugin {
    @Override
    public String[] getASMTransformerClass() {
        String[] original = super.getASMTransformerClass();
        String[] result = new String[original.length + 1];
        System.arraycopy(original, 0, result, 0, original.length);
        result[result.length - 1] = BrandAndChannelTransformer.class.getName();
        return result;
    }

    @Override
    public String getModContainerClass() {
        return LunarSpoofModContainer.class.getName();
    }
}
