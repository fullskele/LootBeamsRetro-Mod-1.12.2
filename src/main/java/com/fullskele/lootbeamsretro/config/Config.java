package com.fullskele.lootbeamsretro.config;

import com.fullskele.lootbeamsretro.render.RenderEventHandler;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.Objects;
import java.util.stream.Stream;

public final class Config {

    public static Configuration instance;

    public static boolean beamForCommonRarity;
    public static boolean beamForAnyRarity;
    public static boolean enableNametagRender;
    public static boolean nametagShowsStackSize;
    public static String[] colorOverrides;

    @net.minecraftforge.common.config.Config.RequiresMcRestart
    public static boolean legendaryTooltipsCompat;
    @net.minecraftforge.common.config.Config.RequiresMcRestart
    public static boolean legendaryTooltipsAffectRarity;
    public static boolean itemBordersCompat;

    public static float beamRotateSpeed;
    public static double beamBlockMaxDistance;

    public static double nametagBlockMaxDistance;
    public static double nametagCenterCutoff;
    public static double nametagXOffset;
    public static double nametagYOffset;
    public static double nametagZOffset;
    public static float nameTagScale;

    public static double innerBeamXOffset;
    public static double innerBeamYOffset;
    public static double innerBeamZOffset;
    public static float innerBeamHeight;
    public static float innerBeamRadius;
    public static float innerBeamAlpha;

    public static double outerBeamXOffset;
    public static double outerBeamYOffset;
    public static double outerBeamZOffset;
    public static float outerBeamHeight;
    public static float outerBeamRadius;
    public static float outerBeamAlpha;

    public static void preInit(File file) {
        instance = new Configuration(file);
        instance.load();
        load();
        if (instance.hasChanged()) {
            instance.save();
        }
    }

    public static void load() {
        String category = "general";

        beamForAnyRarity = instance.get(
                category,
                "beamForAnyRarity",
                true,
                "Should items get beams based on their rarities?"
        ).getBoolean();

        beamForCommonRarity = instance.get(
                category,
                "beamForCommonRarity",
                true,
                "Should beams be enabled for common-rarity item drops?"
        ).getBoolean();

        enableNametagRender = instance.get(
                category,
                "enableNametagRender",
                true,
                "Should floating item names be displayed along with loot beams?"
        ).getBoolean();

        nametagShowsStackSize = instance.get(
                category,
                "nametagShowsStackSize",
                false,
                "Should floating nametags also display how many items are present in the dropped stack (3x Cobblestone)?"
        ).getBoolean();

        colorOverrides = instance.get(
                category,
                "customColorOverrides",
                new String[] {
                        "minecraft:diamond=#00FFFF",
                        "minecraft:emerald=#00FF00"
                },
                "Custom item color overrides in the format 'modid:itemid=#RRGGBB'"
        ).getStringList();



        category = "compat";

        legendaryTooltipsCompat = instance.get(
                category,
                "legendaryTooltipsCompat",
                true,
                "Should Legendary Tooltips compatibility be enabled? Note: This can affect item rarity colors"
        ).getBoolean();

        legendaryTooltipsAffectRarity = instance.get(
                category,
                "legendaryTooltipsAffectRarity",
                false,
                "Should Legendary Tooltips override uncommon/rare/epic item colors?"
        ).getBoolean();

        itemBordersCompat = instance.get(
                category,
                "itemBordersCompat",
                true,
                "Should Item Borders compatibility be enabled?"
        ).getBoolean();



        category = "render.nametag";

        nametagBlockMaxDistance = instance.get(
                category,
                "nametagBlockMaxDistance",
                4.5,
                "Maximum distance that nametags can render from"
        ).getDouble();

        nametagCenterCutoff = instance.get(
                category,
                "nametagCenterCutoff",
                0.008,
                "Max distance that nametags appear from the center of the camera"
        ).getDouble();

        nameTagScale = (float) instance.get(
                category,
                "nameTagScale",
                1.0,
                "Scale factor for nametag render"
        ).getDouble();

        nametagXOffset = instance.get(
                category,
                "nametagXOffset",
                0,
                "X offset for the floating nametag"
        ).getDouble();

        nametagYOffset = instance.get(
                category,
                "nametagYOffset",
                1.0D,
                "Y offset for the floating nametag"
        ).getDouble();

        nametagZOffset = instance.get(
                category,
                "nametagZOffset",
                0,
                "Z offset for the floating nametag"
        ).getDouble();




        category = "render.beam";

        beamRotateSpeed = (float) instance.get(
                category,
                "beamRotateSpeed",
                45.0,
                "Speed at which the beam rotates"
        ).getDouble();

        beamBlockMaxDistance = instance.get(
                category,
                "beamBlockMaxDistance",
                23.574,
                "Maximum distance the beam can render from, in blocks"
        ).getDouble();



        category = "render.beam.inner";

        innerBeamXOffset = instance.get(
                category,
                "innerBeamXOffset",
                0.0,
                "X offset for the inner loot beam"
        ).getDouble();

        innerBeamYOffset = instance.get(
                category,
                "innerBeamYOffset",
                0.25D,
                "Y offset for the inner loot beam"
        ).getDouble();

        innerBeamZOffset = instance.get(
                category,
                "innerBeamZOffset",
                0.0,
                "Z offset for the inner loot beam"
        ).getDouble();

        innerBeamHeight = (float) instance.get(
                category,
                "innerBeamHeight",
                1.5,
                "Height of the inner beam"
        ).getDouble();

        innerBeamRadius = (float) instance.get(
                category,
                "innerBeamRadius",
                0.02,
                "Radius (thickness) of the inner beam"
        ).getDouble();

        innerBeamAlpha = (float) instance.get(
                category,
                "innerBeamAlpha",
                0.45,
                "Transparency (alpha) of the inner beam"
        ).getDouble();



        category = "render.beam.outer";

        outerBeamXOffset = instance.get(
                category,
                "outerBeamXOffset",
                0.0,
                "Horizontal X offset for the outer beam"
        ).getDouble();

        outerBeamYOffset = instance.get(
                category,
                "outerBeamYOffset",
                0.28D,
                "Vertical Y offset for the outer beam"
        ).getDouble();

        outerBeamZOffset = instance.get(
                category,
                "outerBeamZOffset",
                0.0,
                "Depth Z offset for the outer beam"
        ).getDouble();

        outerBeamHeight = (float) instance.get(
                category,
                "outerBeamHeight",
                1.65,
                "Height of the outer beam"
        ).getDouble();

        outerBeamRadius = (float) instance.get(
                category,
                "outerBeamRadius",
                0.03,
                "Radius (thickness) of the outer beam"
        ).getDouble();

        outerBeamAlpha = (float) instance.get(
                category,
                "outerBeamAlpha",
                0.35,
                "Transparency (alpha) of the outer beam"
        ).getDouble();
    }

    public static void loadItems(boolean reload) {
        if (reload) {
            load();
            RenderEventHandler.COLOR_OVERRIDES.clear();
            RenderEventHandler.integrateLegendaryTooltips();
        }

        for (String entry : colorOverrides) {
            try {
                String[] parts = entry.split("=");
                if (parts.length != 2) continue;

                ResourceLocation id = new ResourceLocation(parts[0].trim());
                String hex = parts[1].trim();
                int metaIndex = id.getPath().indexOf(':');

                ResourceLocation itemId = metaIndex == -1 ? id : new ResourceLocation(id.getNamespace(), id.getPath().substring(0, metaIndex));
                Item item = Objects.requireNonNull(Item.REGISTRY.getObject(itemId), () -> "No item with id " + itemId);

                if (hex.startsWith("#")) hex = hex.substring(1);
                if (hex.startsWith("0x")) hex = hex.substring(2);

                float[] color = hexToRgb(Integer.parseInt(hex, 16));

                if (metaIndex == -1) getSubItems(item).forEach(stack -> RenderEventHandler.COLOR_OVERRIDES.put(Pair.of(item, stack.getMetadata()), color));
                else RenderEventHandler.COLOR_OVERRIDES.put(Pair.of(item, Integer.parseInt(id.getPath().substring(metaIndex + 1))), color);
            }
            catch (Exception e) {
                System.err.println("[LootBeams] Invalid color override: " + entry);
                System.err.println(e.getLocalizedMessage());
            }
        }
    }

    public static Stream<ItemStack> getSubItems(Item item) {
        NonNullList<ItemStack> items = NonNullList.create();
        item.getSubItems(CreativeTabs.SEARCH, items);
        return !items.isEmpty() ? items.stream() : Stream.of(new ItemStack(item));
    }

    public static float[] hexToRgb(int hex) {
        float r = ((hex >> 16) & 0xFF) / 255.0F;
        float g = ((hex >> 8) & 0xFF) / 255.0F;
        float b = (hex & 0xFF) / 255.0F;
        return new float[]{r, g, b};
    }

    private Config() {}
}