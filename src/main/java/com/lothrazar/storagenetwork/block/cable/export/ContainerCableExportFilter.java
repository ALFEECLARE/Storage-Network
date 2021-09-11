package com.lothrazar.storagenetwork.block.cable.export;

import com.lothrazar.storagenetwork.block.cable.ContainerCable;
import com.lothrazar.storagenetwork.capability.CapabilityConnectableAutoIO;
import com.lothrazar.storagenetwork.item.ItemUpgrade;
import com.lothrazar.storagenetwork.registry.SsnRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerCableExportFilter extends ContainerCable {

  public final TileCableExport tile;
  public CapabilityConnectableAutoIO cap;

  public ContainerCableExportFilter(int windowId, Level world, BlockPos pos, Inventory playerInv, Player player) {
    super(SsnRegistry.FILTEREXPORTCONTAINER, windowId);
    tile = (TileCableExport) world.getBlockEntity(pos);
    this.cap = tile.ioStorage;
    for (int i = 0; i < cap.upgrades.getSlots(); i++) {
      this.addSlot(new SlotItemHandler(cap.upgrades, i, 98 + i * SQ, 6) {

        @Override
        public int getMaxStackSize() {
          return 1;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
          return stack.getItem() instanceof ItemUpgrade;
        }
      });
    }
    this.bindPlayerInvo(playerInv);
  }

  @Override
  public ItemStack quickMoveStack(Player player, int slotIndex) {
    Slot slot = this.slots.get(slotIndex);
    //in range [4,39] means its coming FROM inventory
    // [0,3] is the filter list
    if (slot != null && slot.hasItem()) {
      ItemStack stackInSlot = slot.getItem();
      if (stackInSlot.getItem() instanceof ItemUpgrade) {
        if (4 <= slotIndex && slotIndex <= 39) {
          //FROM inventory to upgrade slots 
          if (!this.moveItemStackTo(stackInSlot, 0, 4, true)) { // SsnRegistry.UPGRADE_COUNT
            return ItemStack.EMPTY;
          }
        }
        else if (0 <= slotIndex && slotIndex <= 3) {
          //FROM upgrade slots TO inventory 
          if (!this.moveItemStackTo(stackInSlot, 0, 35, true)) {
            return ItemStack.EMPTY;
          }
        }
      }
    }
    return ItemStack.EMPTY;
  }

  @Override
  public boolean stillValid(Player playerIn) {
    return true;
  }
}
