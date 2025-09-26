package com.fullskele.lootbeamsretro;

import com.fullskele.lootbeamsretro.config.Config;
import com.fullskele.lootbeamsretro.render.RenderEventHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(
        modid = LootBeamsRetro.MODID,
        name = LootBeamsRetro.NAME,
        version = LootBeamsRetro.VERSION,
        guiFactory = "com.fullskele.lootbeamsretro.config.ModGuiFactory",
        acceptableRemoteVersions = "*",
        clientSideOnly = true
)
public class LootBeamsRetro
{
    public static final String MODID = "lootbeamsretro";
    public static final String NAME = "Loot Beams Retro";
    public static final String VERSION = "1.2.0";

    public static final boolean hasItemBoarders = Loader.isModLoaded("itemborders");
    public static final boolean hasLegendaryTooltips = Loader.isModLoaded("legendarytooltips");

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Config.preInit(event.getSuggestedConfigurationFile());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        Config.loadItems(false);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        RenderEventHandler.integrateLegendaryTooltips();
    }
}
