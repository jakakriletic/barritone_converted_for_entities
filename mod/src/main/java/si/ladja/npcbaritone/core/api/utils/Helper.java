/*
 * This file is part of Baritone.
 * Modified for NPC Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package si.ladja.npcbaritone.core.api.utils;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import si.ladja.npcbaritone.core.api.BaritoneAPI;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * NPC Baritone (M1.9): namesto klepeta, toastov in namiznih obvestil gre vse v log4j
 * ({@code npcbaritone/core}). Podpisi so ohranjeni, da klicna mesta ostanejo nespremenjena.
 * {@code logDebug} gre na INFO samo, če je {@code chatDebug} vklopljen, sicer na DEBUG.
 *
 * @author Brady
 * @since 8/1/2018
 */
public interface Helper {

    /**
     * Instance of {@link Helper}. Used for static-context reference.
     */
    Helper HELPER = new Helper() {};

    Logger LOG = LogManager.getLogger("npcbaritone/core");

    static String text(ITextComponent... components) {
        return Arrays.stream(components).map(ITextComponent::getUnformattedText).collect(Collectors.joining());
    }

    default void logToast(ITextComponent title, ITextComponent message) {
        LOG.info("{}: {}", title.getUnformattedText(), message.getUnformattedText());
    }

    default void logToast(String title, String message) {
        LOG.info("{}: {}", title, message);
    }

    default void logToast(String message) {
        LOG.info(message);
    }

    default void logNotification(String message) {
        logNotification(message, false);
    }

    default void logNotification(String message, boolean error) {
        logNotificationDirect(message, error);
    }

    default void logNotificationDirect(String message) {
        logNotificationDirect(message, false);
    }

    default void logNotificationDirect(String message, boolean error) {
        if (error) {
            LOG.warn(message);
        } else {
            LOG.info(message);
        }
    }

    default void logDebug(String message) {
        if (BaritoneAPI.getSettings().chatDebug.value) {
            LOG.info(message);
        } else {
            LOG.debug(message);
        }
    }

    default void logDirect(boolean logAsToast, ITextComponent... components) {
        LOG.info(text(components));
    }

    default void logDirect(ITextComponent... components) {
        LOG.info(text(components));
    }

    default void logDirect(String message, TextFormatting color, boolean logAsToast) {
        if (color == TextFormatting.RED) {
            LOG.warn(message);
        } else {
            LOG.info(message);
        }
    }

    default void logDirect(String message, TextFormatting color) {
        logDirect(message, color, false);
    }

    default void logDirect(String message, boolean logAsToast) {
        LOG.info(message);
    }

    /**
     * Send a message to the log regardless of chatDebug.
     *
     * @param message The message to log
     */
    default void logDirect(String message) {
        LOG.info(message);
    }

    default void logUnhandledException(final Throwable exception) {
        LOG.error("An unhandled exception occurred in NPC Baritone", exception);
    }
}
