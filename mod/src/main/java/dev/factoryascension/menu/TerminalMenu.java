package dev.factoryascension.menu;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.blockentity.TerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;

/** The Universal Storage Terminal's screen: the vault's search, sort and scroll over every linked chest. */
public class TerminalMenu extends VaultMenu {
    // Client
    public TerminalMenu(int id, Inventory inv, BlockPos pos) {
        super(FactoryAscension.TERMINAL_MENU.get(), id, inv, null, true);
    }

    // Server
    public TerminalMenu(int id, Inventory inv, TerminalBlockEntity terminal) {
        super(FactoryAscension.TERMINAL_MENU.get(), id, inv, terminal, false);
    }
}
