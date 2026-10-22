package dev.vibecoderqin.lunarspoof;

import com.google.common.eventbus.EventBus;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModMetadata;

import java.util.Collections;

/** Exposes this coremod in Forge's mod list. */
public final class LunarSpoofModContainer extends DummyModContainer {
    public LunarSpoofModContainer() {
        super(new ModMetadata());
        ModMetadata metadata = getMetadata();
        metadata.modId = "lunarspoof";
        metadata.name = "LunarSpoof";
        metadata.version = "1.0.0";
        metadata.authorList = Collections.singletonList("vibecoderqin");
        metadata.url = "https://github.com/vibecoderqin/lunarspoof";
        metadata.description = "Registers the Lunar-Client plugin channel for compatible public server APIs.";
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller) {
        bus.register(this);
        return true;
    }
}
