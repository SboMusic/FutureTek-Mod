package com.futuretek.energy;

import java.text.NumberFormat;
import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** Funzioni di aiuto per spostare energia tra blocchi. */
public final class EnergyUtil {
    private EnergyUtil() {
    }

    /**
     * Spinge energia dal serbatoio verso i blocchi vicini (cavi, macchine, mod esterne).
     * @param sides lati da cui uscire (null = tutti e 6)
     * @return energia inviata
     */
    public static int pushToNeighbors(Level level, BlockPos pos, FTEnergyStorage storage, int maxPerTick,
            Direction... sides) {
        Direction[] dirs = (sides == null || sides.length == 0) ? Direction.values() : sides;
        int remaining = maxPerTick;
        int sent = 0;
        for (Direction dir : dirs) {
            if (remaining <= 0 || storage.getEnergyStored() <= 0) {
                break;
            }
            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(dir),
                    dir.getOpposite());
            if (target == null || !target.canReceive()) {
                continue;
            }
            int offer = Math.min(remaining, storage.getEnergyStored());
            int accepted = target.receiveEnergy(offer, false);
            storage.consume(accepted);
            remaining -= accepted;
            sent += accepted;
        }
        return sent;
    }

    /** Scrive un numero con i separatori delle migliaia, es. 40.000 */
    public static String format(long value) {
        return NumberFormat.getIntegerInstance(Locale.ITALY).format(value);
    }
}
