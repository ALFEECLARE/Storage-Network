package mrriegel.storagenetwork.remote;
import java.util.List;
import mrriegel.storagenetwork.ModItems;
import mrriegel.storagenetwork.helper.StackWrapper;
import mrriegel.storagenetwork.master.TileMaster;
import mrriegel.storagenetwork.network.PacketHandler;
import mrriegel.storagenetwork.network.StacksMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

public class ContainerRemote extends Container {
  public InventoryPlayer playerInv;
  public TileMaster tile;
  public ContainerRemote(final InventoryPlayer playerInv) {
    this.playerInv = playerInv;
    if (!playerInv.player.world.isRemote)
      tile = ItemRemote.getTile(playerInv.getCurrentItem());
    for (int i = 0; i < 3; ++i) {
      for (int j = 0; j < 9; ++j) {
        this.addSlotToContainer(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 174 + i * 18));
      }
    }
    for (int i = 0; i < 9; ++i) {
      if (i == playerInv.currentItem)
        this.addSlotToContainer(new Slot(playerInv, i, 8 + i * 18, 232) {
          @Override
          public boolean isItemValid(ItemStack stack) {
            return false;
          }
          @Override
          public boolean canTakeStack(EntityPlayer playerIn) {
            return false;
          }
        });
      else
        this.addSlotToContainer(new Slot(playerInv, i, 8 + i * 18, 232));
    }
  }
  @Override
  public ItemStack transferStackInSlot(EntityPlayer playerIn, int slotIndex) {
    if (playerIn.world.isRemote)
      return null;
    ItemStack itemstack = null;
    Slot slot = this.inventorySlots.get(slotIndex);
    if (slot != null && slot.getHasStack()) {
      ItemStack itemstack1 = slot.getStack();
      itemstack = itemstack1.copy();
      if (tile != null) {
        int rest = tile.insertStack(itemstack1, null, false);
        ItemStack stack = rest == 0 ? null : ItemHandlerHelper.copyStackWithSize(itemstack1, rest);
        slot.putStack(stack);
        detectAndSendChanges();
        List<StackWrapper> list = tile.getStacks();
        PacketHandler.INSTANCE.sendTo(new StacksMessage(list, tile.getCraftableStacks(list)), (EntityPlayerMP) playerIn);
        return null;
      }
    }
    return itemstack;
  }
  @Override
  public boolean canInteractWith(EntityPlayer playerIn) {
    if (tile == null)
      return false;
    if (!playerIn.world.isRemote && playerIn.world.getTotalWorldTime() % 40 == 0) {
      List<StackWrapper> list = tile.getStacks();
      PacketHandler.INSTANCE.sendTo(new StacksMessage(list, tile.getCraftableStacks(list)), (EntityPlayerMP) playerIn);
    }
    return playerIn.inventory.getCurrentItem() != null && playerIn.inventory.getCurrentItem().getItem() == ModItems.remote;
  }
}