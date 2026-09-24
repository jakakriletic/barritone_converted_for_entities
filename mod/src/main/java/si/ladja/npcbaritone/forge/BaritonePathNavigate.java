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

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import si.ladja.npcbaritone.core.Baritone;
import si.ladja.npcbaritone.core.api.pathing.goals.Goal;
import si.ladja.npcbaritone.core.api.pathing.goals.GoalBlock;

/**
 * M2.5 (minimalno): navigator, ki vsak tick poganja Baritona (D-009) in cilje vanilla taskov
 * preslika v Baritonove. Polna pogodba (debounce, noPath med iskanjem, hibrid za sinhrone
 * {@code getPathTo*}, vanilla {@code Path} iz Baritonove poti) je M6 (D-018).
 *
 * <p>Vanilla sledenje poti je izklopljeno: {@link #onUpdateNavigation()} ne kliče {@code super}.
 * Sinhroni {@code getPathToPos}/{@code getPathToEntityLiving} ostanejo vanilla (podedovano).
 */
public class BaritonePathNavigate extends PathNavigateGround {

    private final Baritone baritone;
    private BlockPos lastGoal;

    public BaritonePathNavigate(EntityLiving entity, World world, Baritone baritone) {
        super(entity, world);
        this.baritone = baritone;
    }

    public Baritone baritone() {
        return baritone;
    }

    @Override
    public void onUpdateNavigation() {
        ++this.totalTicks;
        baritone.tick();
    }

    /** Postavi cilj Baritonu; isti blok ob ponovnem klicu ne sproži novega iskanja. */
    public boolean goTo(Goal goal) {
        baritone.getCustomGoalProcess().setGoalAndPath(goal);
        return true;
    }

    @Override
    public boolean tryMoveToXYZ(double x, double y, double z, double speedIn) {
        BlockPos pos = new BlockPos(x, y, z);
        this.speed = speedIn;
        if (pos.equals(lastGoal) && !noPath()) {
            return true;
        }
        lastGoal = pos;
        return goTo(new GoalBlock(pos));
    }

    @Override
    public boolean tryMoveToEntityLiving(Entity entity, double speedIn) {
        return tryMoveToXYZ(entity.posX, entity.posY, entity.posZ, speedIn);
    }

    @Override
    public void clearPath() {
        super.clearPath();
        lastGoal = null;
        if (baritone != null) { // super konstruktor lahko pokliče clearPath pred nastavitvijo polj
            baritone.getPathingBehavior().cancelEverything();
        }
    }

    @Override
    public boolean noPath() {
        return baritone == null
                || (!baritone.getCustomGoalProcess().isActive()
                && !baritone.getPathingBehavior().isPathing()
                && !baritone.getPathingBehavior().getInProgress().isPresent());
    }
}
