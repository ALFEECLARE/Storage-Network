package com.lothrazar.storagenetwork.block.collection;

import com.lothrazar.storagenetwork.StorageNetworkMod;
import com.lothrazar.storagenetwork.api.IConnectable;
import com.lothrazar.storagenetwork.block.TileConnectable;
import com.lothrazar.storagenetwork.block.main.TileMain;
import com.lothrazar.storagenetwork.capability.handler.FilterItemStackHandler;
import com.lothrazar.storagenetwork.capability.handler.ItemStackHandlerEx;
import com.lothrazar.storagenetwork.registry.StorageNetworkCapabilities;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * @author ajsnarr98 Created MasterItemStackHandler * ItemStackHandler used for interfacing with TileMain like a chest. https://github.com/ajsnarr98/Storage-Network/tree/ajsnarr98-inventory
 * 
 */
public class CollectionItemStackHandler extends ItemStackHandlerEx {

  private TileMain tileMain;
  TileConnectable tile;

  public CollectionItemStackHandler() {
    super(1);
    update();
  }

  public void setMain(TileMain main) {
    this.tileMain = main;
    update();
  }

  /**
   * Updates items in the handler based on outside storage.
   */
  public void update() {
    this.stacks.clear();
  }

  /**
   * @return The remaining ItemStack that was not inserted (if the entire stack is accepted, then return an empty ItemStack). May be the same as the input ItemStack if unchanged, otherwise a new
   *         ItemStack. The returned ItemStack can be safely modified after.
   */
  @Override
  public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
    if (stack.isEmpty() || tileMain == null || !isItemValid(slot, stack)) {
      return stack;
    }
    validateSlotIndex(slot);
    IConnectable cap = tile.getCapability(StorageNetworkCapabilities.CONNECTABLE_CAPABILITY).orElse(null);
    //
    FilterItemStackHandler filter = cap.getFilter();
    if (filter != null
        && !filter.allAreEmpty()
        && filter.isStackFiltered(stack)) {
      // filter is not empty, AND stack does not exist in filter
      // so refuse this
      //      StorageNetwork.log("refuse insertItem " + stack);
      return stack;
    }
    try {
      int remaining = tileMain.insertStack(stack, simulate);
      if (remaining > 0) {
        // if failed, refresh whole list
        update();
        return ItemHandlerHelper.copyStackWithSize(stack, remaining);
      }
    }
    catch (Exception e) {
      StorageNetworkMod.LOGGER.error("insertStack error ", e);
    }
    // if succesful, update internal list
    //      super.insertItem(slot, stack, simulate);
    update();
    return ItemStack.EMPTY;
  }

  @Override
  public ItemStack extractItem(int slot, int amount, boolean simulate) {
    //disabled on this feature
    //    return super.extractItem(slot, 0, true);//disabled
    return ItemStack.EMPTY;
  }
}
