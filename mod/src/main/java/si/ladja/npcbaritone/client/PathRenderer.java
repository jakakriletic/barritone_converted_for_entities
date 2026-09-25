/*
 * This file is part of NPC Baritone.
 *
 * NPC Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NPC Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with NPC Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package si.ladja.npcbaritone.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import si.ladja.npcbaritone.forge.net.PathSyncMessage;

import java.util.Collection;
import java.util.List;

/**
 * M3.3: izris poti v svetu, po zgledu Baritonovega {@code PathRenderer} (v port mapi CLIENT,
 * izrezan v M1) in Automatonovega {@code 2bce8f85}, a samo iz podatkov paketa — klient ne
 * pozna strežniške instance.
 * <ul>
 *     <li>prehojeni del poti: siva</li>
 *     <li>trenutni premik ({@code pathPos → pathPos+1}): rumena</li>
 *     <li>preostanek poti: rdeča (Baritonov {@code colorCurrentPath})</li>
 *     <li>naslednji segment: magenta ({@code colorNextPath})</li>
 *     <li>cilj: zelena škatla; cian, dokler NPC še išče</li>
 *     <li>črta od NPC-ja do trenutne točke: bela</li>
 * </ul>
 * Riše skozi bloke (kot Baritonov {@code renderPathIgnoreDepth}).
 */
public final class PathRenderer {

    private static final double Y_OFFSET = 0.1;
    /** Ordinala {@code NavStatus.State.SEARCHING} (paket nosi ordinal, klient ne uvaža forge stanja). */
    private static final int STATE_SEARCHING = 1;

    private final ClientPaths paths;

    PathRenderer(ClientPaths paths) {
        this.paths = paths;
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || paths.size() == 0) {
            return;
        }
        Collection<ClientPaths.Entry> live = paths.live(Minecraft.getSystemTime());
        if (live.isEmpty()) {
            return;
        }
        Entity view = mc.getRenderViewEntity();
        if (view == null) {
            return;
        }
        float pt = event.getPartialTicks();
        double vx = view.lastTickPosX + (view.posX - view.lastTickPosX) * pt;
        double vy = view.lastTickPosY + (view.posY - view.lastTickPosY) * pt;
        double vz = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.glLineWidth(3.0F);
        try {
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.getBuffer();
            for (ClientPaths.Entry entry : live) {
                PathSyncMessage m = entry.message;
                buf.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
                drawPath(buf, m.path, m.pathPos, vx, vy, vz);
                drawSegments(buf, m.next, 0, m.next.size(), 1.0F, 0.0F, 1.0F, 0.8F, vx, vy, vz);
                Entity npc = mc.world.getEntityByID(m.entityId);
                if (npc != null && m.pathPos + 1 < m.path.size()) {
                    BlockPos target = m.path.get(m.pathPos + 1);
                    double ex = npc.lastTickPosX + (npc.posX - npc.lastTickPosX) * pt;
                    double ey = npc.lastTickPosY + (npc.posY - npc.lastTickPosY) * pt;
                    double ez = npc.lastTickPosZ + (npc.posZ - npc.lastTickPosZ) * pt;
                    vertex(buf, ex - vx, ey + Y_OFFSET - vy, ez - vz, 1.0F, 1.0F, 1.0F, 0.6F);
                    vertex(buf, target.getX() + 0.5 - vx, target.getY() + Y_OFFSET - vy, target.getZ() + 0.5 - vz, 1.0F, 1.0F, 1.0F, 0.6F);
                }
                tess.draw();
                if (m.goal != null) {
                    boolean searching = m.state == STATE_SEARCHING;
                    RenderGlobal.drawSelectionBoundingBox(new AxisAlignedBB(m.goal).offset(-vx, -vy, -vz),
                            0.0F, 1.0F, searching ? 1.0F : 0.0F, 0.9F);
                }
            }
        } finally {
            GlStateManager.glLineWidth(1.0F);
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.disableBlend();
            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();
        }
    }

    private static void drawPath(BufferBuilder buf, List<BlockPos> path, int pos, double vx, double vy, double vz) {
        int n = path.size();
        int p = Math.max(0, Math.min(pos, n - 1));
        drawSegments(buf, path, 0, p, 0.5F, 0.5F, 0.5F, 0.5F, vx, vy, vz);
        drawSegments(buf, path, p, Math.min(p + 1, n - 1), 1.0F, 1.0F, 0.0F, 1.0F, vx, vy, vz);
        drawSegments(buf, path, Math.min(p + 1, n - 1), n - 1, 1.0F, 0.0F, 0.0F, 0.9F, vx, vy, vz);
    }

    /** Daljice med zaporednimi točkami {@code from..to} (indeksi točk, vključno). */
    private static void drawSegments(BufferBuilder buf, List<BlockPos> path, int from, int to,
                                     float r, float g, float b, float a, double vx, double vy, double vz) {
        for (int i = from; i < to && i + 1 < path.size(); i++) {
            BlockPos s = path.get(i);
            BlockPos e = path.get(i + 1);
            vertex(buf, s.getX() + 0.5 - vx, s.getY() + Y_OFFSET - vy, s.getZ() + 0.5 - vz, r, g, b, a);
            vertex(buf, e.getX() + 0.5 - vx, e.getY() + Y_OFFSET - vy, e.getZ() + 0.5 - vz, r, g, b, a);
        }
    }

    private static void vertex(BufferBuilder buf, double x, double y, double z, float r, float g, float b, float a) {
        buf.pos(x, y, z).color(r, g, b, a).endVertex();
    }
}
