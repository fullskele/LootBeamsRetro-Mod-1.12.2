package com.fullskele.lootbeamsretro.config;

import com.fullskele.lootbeamsretro.LootBeamsRetro;
import com.fullskele.lootbeamsretro.render.RenderEventHandler;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = LootBeamsRetro.MODID)
public class ConfigEventHandler
{
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(LootBeamsRetro.MODID)) {
            if (Config.instance.hasChanged()) {
                Config.loadItems(true);
                Config.instance.save();
            }
        }

        else if (event.getModID().equals("legendarytooltips") && Config.legendaryTooltipsCompat) {
            RenderEventHandler.LEGENDARY_TOOLTIPS_COLOR_OVERRIDES = null;
        }
    }
}