package com.fullskele.lootbeamsretro.config;

import com.fullskele.lootbeamsretro.LootBeamsRetro;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.ArrayList;
import java.util.List;

public class ModConfigGui extends GuiConfig {
    public ModConfigGui(GuiScreen parentScreen) {
        super(parentScreen,
                getConfigElements(),
                LootBeamsRetro.MODID,
                false,
                false,
                GuiConfig.getAbridgedConfigPath(Config.instance.toString()));
    }

    private static List<IConfigElement> getConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        list.add(new ConfigElement(Config.instance.getCategory("general")));
        list.add(new ConfigElement(Config.instance.getCategory("compat")));
        list.add(new ConfigElement(Config.instance.getCategory("render")));
        return list;
    }
}