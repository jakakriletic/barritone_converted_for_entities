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

package si.ladja.npcbaritone.core.pathing.movement;

import net.minecraft.entity.Entity;

/**
 * M8.1: velikost entitete v blokih, kot jo vidijo premiki (Automatone {@code 324bd259}:
 * {@code width}, {@code height}, {@code requiredSideSpace} v {@code CalculationContext}).
 *
 * <p>Model: entiteta stoji na sredini bloka nog. Zaseda {@link #heightBlocks} blokov v višino
 * in {@link #sideSpace} dodatnih stolpcev na vsako stran (0 za širino ≤ 1, 1 za širino ≤ 3).
 * Standardna velikost (igralec, zombi, CNPC size 3–5) je {@code sideSpace = 0},
 * {@code heightBlocks = 2}; zanjo premiki računajo natanko tako kot upstream Baritone.
 *
 * <p>Pri spustu (M8, D-028) široka entiteta ne more stati na sredini ciljnega bloka, ker bi
 * zadela blok, s katerega je stopila; kolizija jo porine naprej, zato spust preveri
 * {@link #forwardSpan} stolpcev pred ciljem namesto simetričnega okvira. To velja samo, dokler
 * sredina ostane v ciljnem stolpcu ({@link #canDescend()}: širina &lt; 2).
 */
public final class EntitySize {

    /** Toleranca za float velikosti (CNPC: {@code 0.6F / 5F * size}). */
    private static final float EPS = 1.0E-3F;

    /** Igralec / privzeti CNPC (size 5): 0,6 × 1,8. */
    public static final EntitySize STANDARD = new EntitySize(0.6F, 1.8F);

    public final float width;
    public final float height;
    /** Dodatni stolpci na vsako stran (Automatone {@code requiredSideSpace}). */
    public final int sideSpace;
    /** Višina v blokih, ≥ 1. */
    public final int heightBlocks;
    /** Stolpci pred ciljem, ki jih entiteta zasede po spustu (0 za širino ≤ 1). */
    public final int forwardSpan;

    public EntitySize(float width, float height) {
        if (!(width > 0) || !(height > 0) || Float.isInfinite(width) || Float.isInfinite(height)) {
            throw new IllegalArgumentException("bad entity size " + width + " x " + height);
        }
        this.width = width;
        this.height = height;
        this.sideSpace = Math.max(0, (int) Math.ceil((width - 1.0F) * 0.5F - EPS));
        this.heightBlocks = Math.max(1, (int) Math.ceil(height - EPS));
        this.forwardSpan = Math.max(0, (int) Math.ceil(width - EPS) - 1);
    }

    public static EntitySize of(Entity entity) {
        if (entity == null || isStandard(entity.width, entity.height)) {
            return STANDARD;
        }
        return new EntitySize(entity.width, entity.height);
    }

    /** Brez alokacije (izvajalec poti ga kliče vsak tick): širina ≤ 1, višina v (1, 2]. */
    public static boolean isStandard(float width, float height) {
        return width > 0 && width - 1.0F <= 2 * EPS && height > 1.0F + EPS && height - 2.0F <= EPS;
    }

    /** En stolpec, dva bloka: premiki računajo kot upstream. */
    public boolean isStandard() {
        return sideSpace == 0 && heightBlocks == 2;
    }

    /** Spust in padec sta mogoča (sredina ostane v ciljnem stolpcu). */
    public boolean canDescend() {
        return sideSpace == 0 || width < 2.0F - EPS;
    }

    /** Širina ≤ 1: gre skozi režo širine 1. */
    public boolean singleColumn() {
        return sideSpace == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EntitySize)) {
            return false;
        }
        EntitySize s = (EntitySize) o;
        return Float.compare(width, s.width) == 0 && Float.compare(height, s.height) == 0;
    }

    @Override
    public int hashCode() {
        return 31 * Float.floatToIntBits(width) + Float.floatToIntBits(height);
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "%.2fx%.2f(s%d,h%d)", width, height, sideSpace, heightBlocks);
    }
}
