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

package si.ladja.npcbaritone.forge;

import net.minecraft.entity.EntityLiving;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import si.ladja.npcbaritone.core.Baritone;
import si.ladja.npcbaritone.core.api.pathing.calc.IPath;
import si.ladja.npcbaritone.core.api.pathing.goals.Goal;
import si.ladja.npcbaritone.core.api.pathing.movement.IMovement;
import si.ladja.npcbaritone.core.behavior.PathingBehavior;
import si.ladja.npcbaritone.core.pathing.path.PathExecutor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * M3.4: sled navigacije kot CSV (vzorec {@code FleetTrace} iz {@code ladja_mod}). Ena
 * vrstica na tick na sledeno entiteto; zapisuje se v pomnilnik in izpiše z
 * {@code /npcb trace dump} ali ob koncu tečaja (en tek T1 = ena datoteka, merilo A3).
 *
 * <p>Teče samo na strežniški niti ({@link TickEvent.ServerTickEvent}, faza END, po
 * posodobitvi entitet), zato stanje ni sinhronizirano.
 */
public final class PathTrace {

    public static final PathTrace INSTANCE = new PathTrace();

    /** Vrstni red stolpcev je pogodba za orodja, ki CSV berejo (dodajaj samo na konec). */
    public static final String HEADER = "world_tick,entity_id,entity,tag,state,x,y,z,goal,path_len,path_pos,movement,"
            + "search_us,searches,replans,fail_reason,events,"
            // M7.9 (nedeterminizem v grlu): razlog preklica/premora, zamik uporabe rezultata iskanja, gneča
            + "cancel_reason,pause,submit_tick,exec_no,exec_lat,crowd,collided";
    public static final int COLUMNS = HEADER.split(",").length;
    /** Zgornja meja vrstic v pomnilniku (~20 min pri 20 entitetah); presežek se šteje v {@link #dropped()}. */
    static final int MAX_ROWS = 500_000;

    private boolean recording;
    /** null = vse pripete entitete. */
    private Set<EntityLiving> only;
    private String tag = "";
    private final List<String> rows = new ArrayList<>();
    private long dropped;
    /** M7.9: zadnji viden izvajalec poti po entiteti (za {@code exec_no}/{@code exec_lat}). */
    private final Map<EntityLiving, ExecTrack> execTracks = new WeakHashMap<>();
    /** M7.9: rob okoli hitboxa v blokih, v katerem se druga živa entiteta šteje v {@code crowd}. */
    static final double CROWD_MARGIN = 0.3;

    private PathTrace() {
    }

    public boolean isRecording() {
        return recording;
    }

    public int rowCount() {
        return rows.size();
    }

    public long dropped() {
        return dropped;
    }

    /** Začne novo sled (prejšnje vrstice se zavržejo). {@code entities == null} → vse pripete. */
    public void start(List<EntityLiving> entities) {
        rows.clear();
        execTracks.clear();
        dropped = 0;
        tag = "";
        if (entities == null) {
            only = null;
        } else {
            only = Collections.newSetFromMap(new WeakHashMap<>());
            only.addAll(entities);
        }
        recording = true;
    }

    public void stop() {
        recording = false;
    }

    /** Oznaka v stolpcu {@code tag} (npr. odsek tečaja {@code T1/3}); prazno = brez. */
    public void setTag(String tag) {
        this.tag = tag == null ? "" : tag;
    }

    /**
     * Izpiše sled v {@code file} (mape se ustvarijo). Vrstice ostanejo v pomnilniku.
     *
     * @return število zapisanih vrstic brez glave
     */
    public int dump(File file) throws IOException {
        List<String> out = new ArrayList<>(rows.size() + 1);
        out.add(HEADER);
        out.addAll(rows);
        File dir = file.getAbsoluteFile().getParentFile();
        if (dir != null) {
            Files.createDirectories(dir.toPath());
        }
        Files.write(file.toPath(), out, StandardCharsets.UTF_8);
        return rows.size();
    }

    /** Privzeta datoteka za {@code /npcb trace dump}: {@code npcbaritone/traces/trace-<čas>.csv}. */
    public static File defaultFile() {
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
        return new File("npcbaritone/traces", "trace-" + stamp + ".csv");
    }

    /** HIGH: vzorči pred {@link CourseRunner}, da zadnji tick odseka (npr. FAILED) ostane v sledi. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !recording) {
            return;
        }
        for (EntityLiving e : Attach.attached()) {
            if (only != null && !only.contains(e)) {
                continue;
            }
            Baritone b = Attach.get(e);
            NavStatus s = Attach.status(e);
            if (b == null || s == null || e.isDead) {
                continue;
            }
            if (rows.size() >= MAX_ROWS) {
                dropped++;
                continue;
            }
            rows.add(row(e, b, s, tag, execTracks.computeIfAbsent(e, k -> new ExecTrack())));
        }
    }

    static String row(EntityLiving e, Baritone b, NavStatus s, String tag, ExecTrack track) {
        long tick = e.world.getTotalWorldTime();
        PathingBehavior pb = b.getPathingBehavior();
        PathExecutor current = pb.getCurrent();
        int pathLen = -1;
        int pathPos = -1;
        String movement = "";
        if (current != null) {
            IPath path = current.getPath();
            pathLen = path.length();
            pathPos = current.getPosition();
            List<IMovement> movements = path.movements();
            if (pathPos >= 0 && pathPos < movements.size()) {
                movement = movements.get(pathPos).getClass().getSimpleName();
            }
        }
        Goal goal = s.goal();
        long submitTick = pb.lastSubmitWorldTick();
        long execLat = track.observe(current, submitTick, tick);
        return format(tick, e.getEntityId(), e.getName(), tag, s.state().name(), e.posX, e.posY, e.posZ,
                goal == null ? "" : goal.toString(), pathLen, pathPos, movement, s.lastSearchMicros(),
                pb.searchesStarted(), s.replans(), s.failReason(), s.eventsAt(tick))
                + "," + formatM79(pb.cancelReasonAt(tick), pb.pauseReasonAt(tick), submitTick, track.no, execLat,
                crowd(e), e.collidedHorizontally);
    }

    /** Prvih 17 stolpcev (M3.4); {@link #row} doda še stolpce M7.9 ({@link #formatM79}). */
    static String format(long tick, int id, String name, String tag, String state, double x, double y, double z,
                         String goal, int pathLen, int pathPos, String movement, long searchUs, long searches,
                         int replans, String failReason, String events) {
        return String.format(Locale.ROOT, "%d,%d,%s,%s,%s,%.3f,%.3f,%.3f,%s,%d,%d,%s,%d,%d,%d,%s,%s",
                tick, id, csv(name), csv(tag), state, x, y, z, csv(goal), pathLen, pathPos, csv(movement),
                searchUs, searches, replans, csv(failReason), csv(events));
    }

    /**
     * Stolpci M7.9. {@code exec_lat} je prazen, razen v ticku, ko sled prvič vidi izvajalca iz novega iskanja
     * (takrat ticki od oddaje); {@code submit_tick} je -1, dokler entiteta ni iskala.
     */
    static String formatM79(String cancelReason, String pause, long submitTick, int execNo, long execLat,
                            int crowd, boolean collided) {
        return csv(cancelReason) + "," + csv(pause) + "," + submitTick + "," + execNo + ","
                + (execLat < 0 ? "" : Long.toString(execLat)) + "," + crowd + "," + (collided ? 1 : 0);
    }

    /**
     * M7.9: število drugih živih entitet, katerih hitbox seka hitbox {@code e}, razširjen za
     * {@link #CROWD_MARGIN} vodoravno. Jahač in nosilec se ne štejeta.
     */
    static int crowd(EntityLiving e) {
        int n = 0;
        for (EntityLiving o : e.world.getEntitiesWithinAABB(EntityLiving.class,
                e.getEntityBoundingBox().grow(CROWD_MARGIN, 0.0, CROWD_MARGIN))) {
            if (o == e || o.isDead || o == e.getRidingEntity() || e.isPassenger(o)) {
                continue;
            }
            n++;
        }
        return n;
    }

    /**
     * M7.9: kdaj entiteta prvič izvaja pot iz novega iskanja. Nov izvajalec brez novega iskanja
     * (spoj z naslednjim segmentom, rez zgodovine) ne šteje. Čista logika (headless test).
     */
    static final class ExecTrack {
        private Object lastExec;
        private long reportedSubmit = -1;
        int no;

        /** @return ticki od oddaje iskanja do prve uporabe njegove poti, ali -1, če v tem ticku ni nove poti */
        long observe(Object exec, long submitTick, long now) {
            Object prev = lastExec;
            lastExec = exec;
            if (exec == null || exec == prev || submitTick < 0 || submitTick == reportedSubmit) {
                return -1;
            }
            reportedSubmit = submitTick;
            no++;
            return now - submitTick;
        }
    }

    /** RFC 4180: polje z vejico, narekovajem ali novo vrstico gre v narekovaje. */
    static String csv(String v) {
        if (v == null || v.isEmpty()) {
            return "";
        }
        if (v.indexOf(',') < 0 && v.indexOf('"') < 0 && v.indexOf('\n') < 0 && v.indexOf('\r') < 0) {
            return v;
        }
        return '"' + v.replace("\"", "\"\"") + '"';
    }
}
