package me.alex.rsmjaddon;

import com.raoulvdberge.refinedstorage.api.IRSAPI;
import com.raoulvdberge.refinedstorage.api.RSAPIInject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = RSMJAddon.MODID, name = RSMJAddon.NAME, version = RSMJAddon.VERSION)
public class RSMJAddon
{
    public static final String MODID = "rsmjaddon";
    public static final String NAME = "Refined Storage Minecraft Joules Addon";
    public static final String VERSION = "1.0.0";

    @RSAPIInject
    public static IRSAPI RSApi;
    public static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
    }

    @EventHandler
    public void postInit(FMLInitializationEvent event)
    {
        logger.info("RS MJ Addon Loading...");
        RSApi.getReaderWriterHandlerRegistry().add(ReaderWriterHandlerMinecraftJoules.ID, ReaderWriterHandlerMinecraftJoules::new);
    }
}
