package com.fullskele.lootbeamsretro.render;

import com.anthonyhilyard.itemborders.config.ItemBordersConfig;
import com.anthonyhilyard.legendarytooltips.LegendaryTooltips;
import com.anthonyhilyard.legendarytooltips.LegendaryTooltipsConfig;
import com.fullskele.lootbeamsretro.LootBeamsRetro;
import com.fullskele.lootbeamsretro.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.IRarity;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.fullskele.lootbeamsretro.config.Config.hexToRgb;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = LootBeamsRetro.MODID)
public class RenderEventHandler
{
    private static final ResourceLocation LOOT_BEAM_TEXTURE =
            new ResourceLocation(LootBeamsRetro.MODID, "textures/entity/loot_beam.png");

    public static final Map<Pair<Item, Integer>, float[]> COLOR_OVERRIDES = new HashMap<>();
    private static final Frustum frustum = new Frustum();

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        double partialTicks = event.getPartialTicks();

        double px = interpolate(player.lastTickPosX, player.posX, partialTicks);
        double py = interpolate(player.lastTickPosY, player.posY, partialTicks);
        double pz = interpolate(player.lastTickPosZ, player.posZ, partialTicks);

        frustum.setPosition(px, py, pz);

        for (EntityItem item : mc.world.getEntitiesWithinAABB(EntityItem.class, player.getEntityBoundingBox().grow(Config.beamBlockMaxDistance))) {
            if (!item.isInRangeToRender3d(px, py, pz)) continue;

            double x = interpolate(item.lastTickPosX, item.posX, partialTicks) - px;
            double y = interpolate(item.lastTickPosY, item.posY, partialTicks) - py;
            double z = interpolate(item.lastTickPosZ, item.posZ, partialTicks) - pz;

            if (!item.ignoreFrustumCheck)
            {
                double beamHeight = Math.max(Config.innerBeamYOffset + Config.innerBeamHeight, Config.outerBeamYOffset + Config.outerBeamHeight);
                if (!frustum.isBoundingBoxInFrustum(item.getRenderBoundingBox().setMaxY(py + y + beamHeight))) continue;
            }

            ItemStack stack = item.getItem();
            IRarity rarity = stack.getItem().getForgeRarity(stack);
            FontRenderer fontRenderer = stack.getItem().getFontRenderer(stack);
            if (fontRenderer == null) fontRenderer = mc.fontRenderer;

            float[] rgb = getRarityColor(fontRenderer, rarity);
            boolean usedBorderColor = false;

            if (Config.itemBordersCompat && LootBeamsRetro.hasItemBoarders)
            {
                Pair<Supplier<Integer>, Supplier<Integer>> borderColors = ItemBordersConfig.INSTANCE.getBorderColorForItem(stack);

                if (borderColors != null
                        && borderColors.getLeft() != null && borderColors.getLeft().get() != null
                        && borderColors.getRight() != null && borderColors.getRight().get() != null)
                {
                    int leftColor = borderColors.getLeft().get();

                    // Logic to skip common items to favor usual overrides
                    if (rarity != EnumRarity.COMMON || leftColor != 0xFFFFFF && leftColor != 0xFFFFFFFF)
                    {
                        rgb = hexToRgb(leftColor);
                        usedBorderColor = true;
                    }
                }
            }

            //Usual logic if no usable border color
            if (!usedBorderColor)
            {
                //Override color if in map
                Pair<Item, Integer> id = Pair.of(stack.getItem(), stack.getMetadata());
                if (COLOR_OVERRIDES.containsKey(id)) {
                    rgb = COLOR_OVERRIDES.get(id);
                } else {
                    if (!Config.beamForAnyRarity) continue;
                    if (rarity == EnumRarity.COMMON && !Config.beamForCommonRarity) continue;
                }
            }

            renderLootBeam(x, y, z, rgb, item.getAge() + item.hoverStart + (float)partialTicks);

            if (!Config.enableNametagRender || !Minecraft.isGuiEnabled() ||
                    player.getDistance(item) > Config.nametagBlockMaxDistance ||
                    !isLookingAt(player, item, Config.nametagCenterCutoff) ||
                    !canSee(player, item)) continue;

            renderNameTag(item, x + Config.nametagXOffset, y + Config.nametagYOffset, z + Config.nametagZOffset, rgb, fontRenderer);
        }
    }

    private static void renderLootBeam(double x, double y, double z, float[] color, float time) {
        int oldProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        if (oldProgram != 0) GL20.glUseProgram(0);

        Minecraft.getMinecraft().getTextureManager().bindTexture(LOOT_BEAM_TEXTURE);
        float rotation = -(float)Math.toRadians(time * 0.05625 * Config.beamRotateSpeed - 45);

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableCull();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(false);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableAlpha();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        //Inner beam
        renderLootBeamPart(x + Config.innerBeamXOffset, y + Config.innerBeamYOffset, z + Config.innerBeamZOffset,
                Config.innerBeamHeight, Config.innerBeamRadius, rotation,
                color[0], color[1], color[2], color[3] * Config.innerBeamAlpha);

        //Outer beam
        renderLootBeamPart(x + Config.outerBeamXOffset, y + Config.outerBeamYOffset, z + Config.outerBeamZOffset,
                Config.outerBeamHeight, Config.outerBeamRadius, rotation,
                color[0], color[1], color[2], color[3] * Config.outerBeamAlpha);

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableAlpha();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        RenderHelper.enableStandardItemLighting();

        if (oldProgram != 0) GL20.glUseProgram(oldProgram);
    }

    private static void renderLootBeamPart(double x, double y, double z,
                                           float height, float radius, float rotation,
                                           float r, float g, float b, float a) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        double x1 = MathHelper.cos(rotation + (float)Math.PI * 1 / 4) * radius * 4 / 3;
        double z1 = MathHelper.sin(rotation + (float)Math.PI * 1 / 4) * radius * 4 / 3;
        double x3 = MathHelper.cos(rotation + (float)Math.PI * 3 / 4) * radius * 4 / 3;
        double z3 = MathHelper.sin(rotation + (float)Math.PI * 3 / 4) * radius * 4 / 3;
        double x5 = MathHelper.cos(rotation + (float)Math.PI * 5 / 4) * radius * 4 / 3;
        double z5 = MathHelper.sin(rotation + (float)Math.PI * 5 / 4) * radius * 4 / 3;
        double x7 = MathHelper.cos(rotation + (float)Math.PI * 7 / 4) * radius * 4 / 3;
        double z7 = MathHelper.sin(rotation + (float)Math.PI * 7 / 4) * radius * 4 / 3;

        float midY = height / 2f;

        buf.setTranslation(x, y, z);

        buf.pos(x3, height, z3).tex(1, 1).color(r, g, b, 0).endVertex();
        buf.pos(x3, midY,   z3).tex(1, 0.5).color(r, g, b, a).endVertex();
        buf.pos(x1, midY,   z1).tex(0, 0.5).color(r, g, b, a).endVertex();
        buf.pos(x1, height, z1).tex(0, 1).color(r, g, b, 0).endVertex();

        buf.pos(x3, midY,   z3).tex(1, 0.5).color(r, g, b, a).endVertex();
        buf.pos(x3, 0,      z3).tex(1, 0).color(r, g, b, 0).endVertex();
        buf.pos(x1, 0,      z1).tex(0, 0).color(r, g, b, 0).endVertex();
        buf.pos(x1, midY,   z1).tex(0, 0.5).color(r, g, b, a).endVertex();

        buf.pos(x5, height, z5).tex(1, 1).color(r, g, b, 0).endVertex();
        buf.pos(x5, midY,   z5).tex(1, 0.5).color(r, g, b, a).endVertex();
        buf.pos(x3, midY,   z3).tex(0, 0.5).color(r, g, b, a).endVertex();
        buf.pos(x3, height, z3).tex(0, 1).color(r, g, b, 0).endVertex();

        buf.pos(x5, midY,   z5).tex(1, 0.5).color(r, g, b, a).endVertex();
        buf.pos(x5, 0,      z5).tex(1, 0).color(r, g, b, 0).endVertex();
        buf.pos(x3, 0,      z3).tex(0, 0).color(r, g, b, 0).endVertex();
        buf.pos(x3, midY,   z3).tex(0, 0.5).color(r, g, b, a).endVertex();

        buf.pos(x7, height, z7).tex(1, 1).color(r, g, b, 0).endVertex();
        buf.pos(x7, midY,   z7).tex(1, 0.5).color(r, g, b, a).endVertex();
        buf.pos(x5, midY,   z5).tex(0, 0.5).color(r, g, b, a).endVertex();
        buf.pos(x5, height, z5).tex(0, 1).color(r, g, b, 0).endVertex();

        buf.pos(x7, midY,   z7).tex(1, 0.5).color(r, g, b, a).endVertex();
        buf.pos(x7, 0,      z7).tex(1, 0).color(r, g, b, 0).endVertex();
        buf.pos(x5, 0,      z5).tex(0, 0).color(r, g, b, 0).endVertex();
        buf.pos(x5, midY,   z5).tex(0, 0.5).color(r, g, b, a).endVertex();

        buf.pos(x1, height, z1).tex(1, 1).color(r, g, b, 0).endVertex();
        buf.pos(x1, midY,   z1).tex(1, 0.5).color(r, g, b, a).endVertex();
        buf.pos(x7, midY,   z7).tex(0, 0.5).color(r, g, b, a).endVertex();
        buf.pos(x7, height, z7).tex(0, 1).color(r, g, b, 0).endVertex();

        buf.pos(x1, midY,   z1).tex(1, 0.5).color(r, g, b, a).endVertex();
        buf.pos(x1, 0,      z1).tex(1, 0).color(r, g, b, 0).endVertex();
        buf.pos(x7, 0,      z7).tex(0, 0).color(r, g, b, 0).endVertex();
        buf.pos(x7, midY,   z7).tex(0, 0.5).color(r, g, b, a).endVertex();

        buf.setTranslation(0, 0, 0);

        tess.draw();
    }

    private static void renderNameTag(EntityItem item, double x, double y, double z, float[] color, FontRenderer fontRenderer) {
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.pushMatrix();
        {
            GlStateManager.translate(x, y, z);

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

            if (Config.nametagShowsStackSize && item.getItem().getCount() > 1) {
                itemName = item.getItem().getCount() + "x " + itemName;
            }

            int textWidth = fontRenderer.getStringWidth(itemName) / 2;
            float alpha = 64f / 255f * color[3];

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            GlStateManager.disableTexture2D();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(-textWidth - 1, -1, 0.0D).color(0, 0, 0, alpha).endVertex();
            buffer.pos(-textWidth - 1, 8, 0.0D).color(0, 0, 0, alpha).endVertex();
            buffer.pos(textWidth + 1, 8, 0.0D).color(0, 0, 0, alpha).endVertex();
            buffer.pos(textWidth + 1, -1, 0.0D).color(0, 0, 0, alpha).endVertex();
            tessellator.draw();
            GlStateManager.enableTexture2D();

            int rgb = MathHelper.rgb(color[0], color[1], color[2]) | (int)(color[3] * 255) << 24;
            fontRenderer.drawString(itemName, -textWidth, 0, rgb);

            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();

            if (oldProgram != 0) GL20.glUseProgram(oldProgram);
        }
        GlStateManager.popMatrix();
    }

    private static float[] getRarityColor(FontRenderer fontRenderer, IRarity rarity) {
        return hexToRgb(fontRenderer.getColorCode(rarity.getColor().toString().charAt(1)));
    }

    private static double interpolate(double last, double current, double partialTicks) {
        return last + (current - last) * partialTicks;
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

    private static boolean canSee(EntityPlayerSP player, EntityItem item) {
        Vec3d eyePos = player.getPositionEyes(1.0F);
        Vec3d targetPos = new Vec3d(item.posX, item.posY + (item.height * 0.5), item.posZ);

        RayTraceResult result = player.world.rayTraceBlocks(eyePos, targetPos, false, true, false);

        return result == null || result.typeOfHit == RayTraceResult.Type.MISS;
    }

    private static ItemStack[] getAllRegisteredItems() {
        return ForgeRegistries.ITEMS.getValuesCollection().stream().flatMap(Config::getSubItems).toArray(ItemStack[]::new);
    }

    public static void integrateLegendaryTooltips() {
        if (LootBeamsRetro.hasLegendaryTooltips && Config.legendaryTooltipsCompat) try {
            LegendaryTooltipsConfig config = LegendaryTooltipsConfig.INSTANCE;
            ItemStack[] allItems = getAllRegisteredItems();

            for (int level = 0; level < LegendaryTooltips.NUM_FRAMES; level++) {
                Integer colorHex = config.getCustomBackgroundColor(level);
                if (colorHex == null) continue;

                for (ItemStack stack : allItems) {
                    if (config.getFrameLevelForItem(stack) == level) {
                        IRarity rarity = stack.getItem().getForgeRarity(stack);

                        if (!Config.legendaryTooltipsAffectRarity && rarity != EnumRarity.COMMON) {
                            continue;
                        }

                        COLOR_OVERRIDES.put(Pair.of(stack.getItem(), stack.getMetadata()), hexToRgb(colorHex));
                    }
                }
            }

        } catch (Throwable t) {
            System.err.println("[LootBeams] Failed to sync LegendaryTooltips colors: " + t);
        }
    }
}