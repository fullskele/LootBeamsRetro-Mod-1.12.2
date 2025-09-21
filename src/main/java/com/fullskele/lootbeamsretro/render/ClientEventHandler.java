package com.fullskele.lootbeamsretro.render;

import com.anthonyhilyard.legendarytooltips.LegendaryTooltips;
import com.anthonyhilyard.legendarytooltips.LegendaryTooltipsConfig;
import com.fullskele.lootbeamsretro.LootBeamsRetro;
import com.fullskele.lootbeamsretro.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = LootBeamsRetro.MODID)
public class ClientEventHandler {

    private static final ResourceLocation LOOT_BEAM_TEXTURE =
            new ResourceLocation(LootBeamsRetro.MODID, "textures/entity/loot_beam_new.png");

    public static final Map<ResourceLocation, Integer> COLOR_OVERRIDES = new HashMap<>();
    private static final Frustum frustum = new Frustum();
    private static final Map<EnumRarity, float[]> rarityCache = new HashMap<>();

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        double partialTicks = event.getPartialTicks();

        double px = interpolate(player.lastTickPosX, player.posX, partialTicks);
        double py = interpolate(player.lastTickPosY, player.posY, partialTicks);
        double pz = interpolate(player.lastTickPosZ, player.posZ, partialTicks);

        frustum.setPosition(px, py, pz);

        List<EntityItem> items = mc.world.getEntitiesWithinAABB(EntityItem.class,
                player.getEntityBoundingBox().grow(Config.beamBlockMaxDistance));

        for (EntityItem item : items) {
            if (!frustum.isBoundingBoxInFrustum(item.getEntityBoundingBox())) continue;

            double x = interpolate(item.lastTickPosX, item.posX, partialTicks) - px;
            double y = interpolate(item.lastTickPosY, item.posY, partialTicks) - py;
            double z = interpolate(item.lastTickPosZ, item.posZ, partialTicks) - pz;

            ItemStack stack = item.getItem();
            EnumRarity rarity = stack.getRarity();

            float[] rgb = rarityCache.computeIfAbsent(rarity, r -> getRarityColor(r));

            //Override color if in map
            ResourceLocation id = stack.getItem().getRegistryName();
            if (id != null && COLOR_OVERRIDES.containsKey(id)) {
                rgb = hexToRgb(COLOR_OVERRIDES.get(id));
            } else {
                if (!Config.beamForAnyRarity) continue;
                if (rarity == EnumRarity.COMMON && !Config.beamForCommonRarity) continue;
            }

            //Inner beam
            renderLootBeam(x + Config.innerBeamXOffset, y + Config.innerBeamYOffset, z + Config.innerBeamZOffset,
                    Config.innerBeamHeight, Config.innerBeamRadius,
                    rgb[0], rgb[1], rgb[2], Config.innerBeamAlpha,
                    (float) (mc.world.getTotalWorldTime() + partialTicks));

            //Outer beam
            renderLootBeam(x + Config.outerBeamXOffset, y + Config.outerBeamYOffset, z + Config.outerBeamZOffset,
                    Config.outerBeamHeight, Config.outerBeamRadius,
                    rgb[0], rgb[1], rgb[2], Config.outerBeamAlpha,
                    (float) (mc.world.getTotalWorldTime() + partialTicks));


            if (!Config.enableNametagRender || player.getDistance(item) > Config.nametagBlockMaxDistance) continue;

            if (!isLookingAt(player, item, Config.nametagCenterCutoff)) continue;

            if (!canSee(player, item)) continue;

            Color tagColor = new Color(rgb[0], rgb[1], rgb[2]);
            renderNameTag(item, x, y + 0.5D, z, tagColor);
        }
    }

    private static boolean isLookingAt(EntityPlayerSP player, Entity target, double tolerance) {
        Vec3d lookVec = player.getLookVec().normalize();
        Vec3d toTarget = new Vec3d(
                target.posX - player.posX,
                target.posY + target.height * 0.5 - (player.posY + player.getEyeHeight()),
                target.posZ - player.posZ
        ).normalize();

        double dot = lookVec.dotProduct(toTarget);
        return dot > (1.0D - tolerance);
    }

    private static double interpolate(double last, double current, double partialTicks) {
        return last + (current - last) * partialTicks;
    }

    private static void renderLootBeam(double x, double y, double z,
                                                     float height, float radius,
                                                     float r, float g, float b, float a,
                                                     float time) {
        Minecraft mc = Minecraft.getMinecraft();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        mc.getTextureManager().bindTexture(LOOT_BEAM_TEXTURE);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        int oldProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        if (oldProgram != 0) GL20.glUseProgram(0);

        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableCull();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(false);
        net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();

        float rotation = (time % 40) / 40.0F * Config.beamRotateSpeed;
        GlStateManager.rotate(rotation * 2.25F - 45F, 0, 1, 0);

        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        //Front
        buf.pos(-radius, 0, -radius).tex(0, 1).color(r, g, b, a).endVertex();
        buf.pos(-radius, height, -radius).tex(0, 0).color(r, g, b, a).endVertex();
        buf.pos(radius, height, -radius).tex(1, 0).color(r, g, b, a).endVertex();
        buf.pos(radius, 0, -radius).tex(1, 1).color(r, g, b, a).endVertex();

        //Back
        buf.pos(radius, 0, radius).tex(0, 1).color(r, g, b, a).endVertex();
        buf.pos(radius, height, radius).tex(0, 0).color(r, g, b, a).endVertex();
        buf.pos(-radius, height, radius).tex(1, 0).color(r, g, b, a).endVertex();
        buf.pos(-radius, 0, radius).tex(1, 1).color(r, g, b, a).endVertex();

        //Left
        buf.pos(-radius, 0, radius).tex(0, 1).color(r, g, b, a).endVertex();
        buf.pos(-radius, height, radius).tex(0, 0).color(r, g, b, a).endVertex();
        buf.pos(-radius, height, -radius).tex(1, 0).color(r, g, b, a).endVertex();
        buf.pos(-radius, 0, -radius).tex(1, 1).color(r, g, b, a).endVertex();

        //Right
        buf.pos(radius, 0, -radius).tex(0, 1).color(r, g, b, a).endVertex();
        buf.pos(radius, height, -radius).tex(0, 0).color(r, g, b, a).endVertex();
        buf.pos(radius, height, radius).tex(1, 0).color(r, g, b, a).endVertex();
        buf.pos(radius, 0, radius).tex(1, 1).color(r, g, b, a).endVertex();

        tess.draw();

        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        net.minecraft.client.renderer.RenderHelper.enableStandardItemLighting();

        if (oldProgram != 0) GL20.glUseProgram(oldProgram);

        GlStateManager.popMatrix();
    }

    /*
    private static void renderLootBeamOld(double x, double y, double z,
                                       float height, float radius,
                                       float r, float g, float b, float a,
                                       float time) {
        Minecraft mc = Minecraft.getMinecraft();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        mc.getTextureManager().bindTexture(LOOT_BEAM_TEXTURE);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableCull();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(false);

        float rotation = (time % 40) / 40.0F * Config.beamRotateSpeed;
        GlStateManager.rotate(rotation * 2.25F - 45F, 0, 1, 0);

        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        //Front
        buf.pos(-radius, 0, -radius).tex(0, 1).color(r, g, b, a).endVertex();
        buf.pos(-radius, height, -radius).tex(0, 0).color(r, g, b, a).endVertex();
        buf.pos(radius, height, -radius).tex(1, 0).color(r, g, b, a).endVertex();
        buf.pos(radius, 0, -radius).tex(1, 1).color(r, g, b, a).endVertex();

        //Back
        buf.pos(radius, 0, radius).tex(0, 1).color(r, g, b, a).endVertex();
        buf.pos(radius, height, radius).tex(0, 0).color(r, g, b, a).endVertex();
        buf.pos(-radius, height, radius).tex(1, 0).color(r, g, b, a).endVertex();
        buf.pos(-radius, 0, radius).tex(1, 1).color(r, g, b, a).endVertex();

        //Left
        buf.pos(-radius, 0, radius).tex(0, 1).color(r, g, b, a).endVertex();
        buf.pos(-radius, height, radius).tex(0, 0).color(r, g, b, a).endVertex();
        buf.pos(-radius, height, -radius).tex(1, 0).color(r, g, b, a).endVertex();
        buf.pos(-radius, 0, -radius).tex(1, 1).color(r, g, b, a).endVertex();

        //Right
        buf.pos(radius, 0, -radius).tex(0, 1).color(r, g, b, a).endVertex();
        buf.pos(radius, height, -radius).tex(0, 0).color(r, g, b, a).endVertex();
        buf.pos(radius, height, radius).tex(1, 0).color(r, g, b, a).endVertex();
        buf.pos(radius, 0, radius).tex(1, 1).color(r, g, b, a).endVertex();

        tess.draw();

        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

     */

    private static float[] getRarityColor(EnumRarity rarity) {
        switch (rarity) {
            case COMMON:
                //white
                return new float[]{1f, 1f, 1f};
            case UNCOMMON:
                //yellow
                return new float[]{1f, 1f, 0f};
            case RARE:
                //cyan
                return new float[]{0f, 1f, 1f};
            case EPIC:
                //hot pink
                return new float[]{1f, 0f, 1f};
            default:
                return new float[]{1f, 1f, 1f};
        }
    }

    private static float[] hexToRgb(int hex) {
        float r = ((hex >> 16) & 0xFF) / 255.0F;
        float g = ((hex >> 8) & 0xFF) / 255.0F;
        float b = (hex & 0xFF) / 255.0F;
        return new float[]{r, g, b};
    }

    private static boolean canSee(EntityPlayerSP player, EntityItem item) {
        Vec3d eyePos = player.getPositionEyes(1.0F);
        Vec3d targetPos = new Vec3d(item.posX, item.posY + (item.height * 0.5), item.posZ);

        RayTraceResult result = player.world.rayTraceBlocks(eyePos, targetPos, false, true, false);

        return result == null || result.typeOfHit == RayTraceResult.Type.MISS;
    }

    private static void renderNameTag(EntityItem item, double x, double y, double z, Color color) {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fontRenderer = mc.fontRenderer;

        GlStateManager.pushMatrix();
        {
            GlStateManager.translate(x, y + 0.5D, z);

            GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);

            //name text scale
            float scale = 0.01667F * 1.6F * Config.nameTagScale;
            GlStateManager.scale(-scale, -scale, scale);

            int oldProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            if (oldProgram != 0) GL20.glUseProgram(0);

            GlStateManager.disableLighting();
            GlStateManager.depthMask(false);
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();

            String itemName = item.getItem().getDisplayName();
            int count = item.getItem().getCount();
            if (count > 1) {
                itemName = count + "x " + itemName;
            }

            int textWidth = fontRenderer.getStringWidth(itemName) / 2;

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            GlStateManager.disableTexture2D();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(-textWidth - 1, -1, 0.0D).color(0, 0, 0, 64).endVertex();
            buffer.pos(-textWidth - 1, 8, 0.0D).color(0, 0, 0, 64).endVertex();
            buffer.pos(textWidth + 1, 8, 0.0D).color(0, 0, 0, 64).endVertex();
            buffer.pos(textWidth + 1, -1, 0.0D).color(0, 0, 0, 64).endVertex();
            tessellator.draw();
            GlStateManager.enableTexture2D();

            int rgb = color.getRGB();
            fontRenderer.drawString(itemName, -textWidth, 0, rgb);

            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();

            if (oldProgram != 0) GL20.glUseProgram(oldProgram);
        }
        GlStateManager.popMatrix();
    }

    /*
    private static void renderNameTagOld(EntityItem item, double x, double y, double z, Color color) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        FontRenderer fontRenderer = mc.fontRenderer;

        GlStateManager.pushMatrix();
        {
            GlStateManager.translate(x, y + 0.5D, z);

            GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);

            float scale = 0.016666668F * 1.6F * Config.nameTagScale;
            GlStateManager.scale(-scale, -scale, scale);

            GlStateManager.disableLighting();
            GlStateManager.depthMask(false);
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();

            String itemName = item.getItem().getDisplayName();
            int count = item.getItem().getCount();
            if (count > 1) {
                itemName = itemName + " x" + count;
            }

            int textWidth = fontRenderer.getStringWidth(itemName) / 2;

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            GlStateManager.disableTexture2D();
            buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(-textWidth - 1, -1, 0.0D).color(0, 0, 0, 64).endVertex();
            buffer.pos(-textWidth - 1, 8, 0.0D).color(0, 0, 0, 64).endVertex();
            buffer.pos(textWidth + 1, 8, 0.0D).color(0, 0, 0, 64).endVertex();
            buffer.pos(textWidth + 1, -1, 0.0D).color(0, 0, 0, 64).endVertex();
            tessellator.draw();
            GlStateManager.enableTexture2D();

            int rgb = color.getRGB();
            fontRenderer.drawString(itemName, -textWidth, 0, rgb);

            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
        }
        GlStateManager.popMatrix();
    }
     */

    private static Iterable<ItemStack> getAllRegisteredItems() {
        return ForgeRegistries.ITEMS.getValuesCollection().stream()
                .filter(i -> i != null)
                .map(ItemStack::new)
                .collect(Collectors.toList());
    }

    public static void integrateLegendaryTooltips() {
        try {
            LegendaryTooltipsConfig config = LegendaryTooltipsConfig.INSTANCE;

            for (int level = 0; level < LegendaryTooltips.NUM_FRAMES; level++) {
                Integer colorHex = config.getCustomBackgroundColor(level);
                if (colorHex == null) continue;

                for (ItemStack stack : getAllRegisteredItems()) {
                    if (config.getFrameLevelForItem(stack) == level) {
                        EnumRarity rarity = stack.getRarity();

                        if (!Config.legendaryTooltipsAffectRarity && rarity != EnumRarity.COMMON) {
                            continue;
                        }

                        ResourceLocation id = stack.getItem().getRegistryName();
                        if (id != null) {
                            COLOR_OVERRIDES.put(id, colorHex);
                        }
                    }
                }
            }

        } catch (Throwable t) {
            System.out.println("[LootBeams] Failed to sync LegendaryTooltips colors: " + t);
        }
    }
}