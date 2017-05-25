package mrriegel.storagenetwork.tile;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import mrriegel.storagenetwork.api.IConnectable;
import mrriegel.storagenetwork.config.ConfigHandler;
import mrriegel.storagenetwork.helper.CraftingTask;
import mrriegel.storagenetwork.helper.FilterItem;
import mrriegel.storagenetwork.helper.InvHelper;
import mrriegel.storagenetwork.helper.NBTHelper;
import mrriegel.storagenetwork.helper.StackWrapper;
import mrriegel.storagenetwork.helper.Util;
import mrriegel.storagenetwork.items.ItemTemplate;
import mrriegel.storagenetwork.items.ItemUpgrade;
import mrriegel.storagenetwork.tile.AbstractFilterTile.Direction;
import mrriegel.storagenetwork.tile.TileKabel.Kind;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import cofh.api.energy.EnergyStorage;
import cofh.api.energy.IEnergyReceiver;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class TileMaster extends TileEntity implements ITickable, IEnergyReceiver {
  public Set<BlockPos> connectables;
  public List<BlockPos> storageInventorys, fstorageInventorys;
  public EnergyStorage en = new EnergyStorage(ConfigHandler.energyCapacity, Integer.MAX_VALUE, 0);
  public List<CraftingTask> tasks = Lists.newArrayList();
  public List<FluidStack> getFluids() {
    List<FluidStack> stacks = Lists.newArrayList();
    List<AbstractFilterTile> invs = Lists.newArrayList();
    if (connectables == null)
      refreshNetwork();
    for (BlockPos p : connectables) {
      if (world.getTileEntity(p) instanceof AbstractFilterTile) {
        AbstractFilterTile tile = (AbstractFilterTile) world.getTileEntity(p);
        if (tile.isStorage() && tile.getFluidTank() != null) {
          invs.add(tile);
        }
      }
    }
    for (AbstractFilterTile t : invs) {
      IFluidHandler inv = t.getFluidTank();
      if (inv == null)
        continue;
      if (inv.getTankProperties() == null)
        continue;
      for (IFluidTankProperties i : inv.getTankProperties()) {
        if (i != null && i.getContents() != null && t.canTransfer(i.getContents().getFluid(), Direction.BOTH))
          addToList(stacks, i.getContents().getFluid(), i.getContents().amount);
      }
    }
    return stacks;
  }
  public List<StackWrapper> getStacks() {
    List<StackWrapper> stacks = Lists.newArrayList();
    List<AbstractFilterTile> invs = Lists.newArrayList();
    if (connectables == null)
      refreshNetwork();
    for (BlockPos p : connectables) {
      if (world.getTileEntity(p) instanceof AbstractFilterTile) {
        AbstractFilterTile tile = (AbstractFilterTile) world.getTileEntity(p);
        if (tile.isStorage() && tile.getInventory() != null) {
          invs.add(tile);
        }
      }
    }
    for (AbstractFilterTile t : invs) {
      IItemHandler inv = t.getInventory();
      for (int i = 0; i < inv.getSlots(); i++) {
        if (inv.getStackInSlot(i) != null && t.canTransfer(inv.getStackInSlot(i), Direction.BOTH))
          addToList(stacks, inv.getStackInSlot(i).copy(), inv.getStackInSlot(i).getCount());
      }
    }
    return stacks;
  }
  public int emptySlots() {
    int res = 0;
    List<StackWrapper> stacks = Lists.newArrayList();
    List<AbstractFilterTile> invs = Lists.newArrayList();
    for (BlockPos p : connectables) {
      if (world.getTileEntity(p) instanceof AbstractFilterTile) {
        AbstractFilterTile tile = (AbstractFilterTile) world.getTileEntity(p);
        if (tile.isStorage() && tile.getInventory() != null) {
          invs.add(tile);
        }
      }
    }
    for (AbstractFilterTile t : invs) {
      IItemHandler inv = t.getInventory();
      for (int i = 0; i < inv.getSlots(); i++) {
        if (inv.getStackInSlot(i) == null)
          res++;
      }
    }
    return res;
  }
  public List<StackWrapper> getCraftableStacks(List<StackWrapper> stacks) {
    List<StackWrapper> craftableStacks = Lists.newArrayList();
    List<TileContainer> invs = Lists.newArrayList();
    for (BlockPos p : connectables) {
      if (!(world.getTileEntity(p) instanceof TileContainer))
        continue;
      TileContainer tile = (TileContainer) world.getTileEntity(p);
      invs.add(tile);
    }
    for (TileContainer t : invs) {
      for (int i = 0; i < t.getSizeInventory(); i++) {
        if (t.getStackInSlot(i) != null && !t.getStackInSlot(i).isEmpty()) {
          NBTTagCompound res = (NBTTagCompound) t.getStackInSlot(i).getTagCompound().getTag("res");
          if (!Util.contains(stacks, new StackWrapper(new ItemStack(res), 0), new Comparator<StackWrapper>() {
            @Override
            public int compare(StackWrapper o1, StackWrapper o2) {
              if (ItemHandlerHelper.canItemStacksStack(o1.getStack(), o2.getStack())) { return 0; }
              return 1;
            }
          }))
            addToList(craftableStacks, new ItemStack(res), 0);
        }
      }
    }
    return craftableStacks;
  }
  private void addToList(List<StackWrapper> lis, ItemStack s, int num) {
    boolean added = false;
    for (int i = 0; i < lis.size(); i++) {
      ItemStack stack = lis.get(i).getStack();
      if (ItemHandlerHelper.canItemStacksStack(s, stack)) {
        lis.get(i).setSize(lis.get(i).getSize() + num);
        added = true;
      }
    }
    if (!added)
      lis.add(new StackWrapper(s, num));
  }
  private void addToList(List<FluidStack> lis, Fluid s, int num) {
    boolean added = false;
    for (int i = 0; i < lis.size(); i++) {
      FluidStack stack = lis.get(i);
      if (stack.getFluid() == s) {
        lis.get(i).amount += num;
        added = true;
      }
    }
    if (!added)
      lis.add(new FluidStack(s, num));
  }
  public int getAmount(FilterItem fil) {
    if (fil == null)
      return 0;
    int size = 0;
    ItemStack s = fil.getStack();
    for (StackWrapper w : getStacks()) {
      if (fil.match(w.getStack()))
        size += w.getSize();
    }
    return size;
  }
  public int getAmount(Fluid fluid) {
    if (fluid == null)
      return 0;
    int size = 0;
    for (FluidStack w : getFluids()) {
      if (w.getFluid() == fluid)
        size += w.amount;
    }
    return size;
  }
  public List<TileContainer> getContainers() {
    List<TileContainer> lis = Lists.newArrayList();
    for (BlockPos p : connectables) {
      if (!(world.getTileEntity(p) instanceof TileContainer))
        continue;
      lis.add((TileContainer) world.getTileEntity(p));
    }
    return lis;
  }
  public List<ItemStack> getTemplates(FilterItem fil) {
    List<ItemStack> templates = Lists.newArrayList();
    for (TileContainer tile : getContainers()) {
      for (ItemStack s : tile.getTemplates()) {
        ItemStack result = ItemTemplate.getOutput(s);
        if (fil.match(result)) {
          ItemStack a = s;
          a.setCount(result.getCount());
          templates.add(s);
        }
      }
    }
    return templates;
  }
  public List<FilterItem> getIngredients(ItemStack template) {
    Map<Integer, ItemStack> stacks = Maps.<Integer, ItemStack> newHashMap();
    Map<Integer, Boolean> metas = Maps.<Integer, Boolean> newHashMap();
    Map<Integer, Boolean> ores = Maps.<Integer, Boolean> newHashMap();
    NBTTagList invList = template.getTagCompound().getTagList("crunchItem", Constants.NBT.TAG_COMPOUND);
    for (int i = 0; i < invList.tagCount(); i++) {
      NBTTagCompound stackTag = invList.getCompoundTagAt(i);
      int slot = stackTag.getByte("Slot");
      stacks.put(slot, new ItemStack(stackTag));
    }
    List<FilterItem> list = Lists.newArrayList();
    for (int i = 1; i < 10; i++) {
      metas.put(i - 1, NBTHelper.getBoolean(template, "meta" + i));
      ores.put(i - 1, NBTHelper.getBoolean(template, "ore" + i));
    }
    for (Entry<Integer, ItemStack> e : stacks.entrySet()) {
      if (e.getValue() != null) {
        boolean meta = metas.get(e.getKey()), ore = ores.get(e.getKey());
        list.add(new FilterItem(e.getValue(), meta, ore, false));
      }
    }
    return list;
  }
  @Override
  public NBTTagCompound getUpdateTag() {
    return writeToNBT(new NBTTagCompound());
  }
  @Override
  public void readFromNBT(NBTTagCompound compound) {
    super.readFromNBT(compound);
    en.readFromNBT(compound);
    NBTTagList tasksList = compound.getTagList("tasks", Constants.NBT.TAG_COMPOUND);
    tasks = Lists.newArrayList();
    for (int i = 0; i < tasksList.tagCount(); i++) {
      NBTTagCompound stackTag = tasksList.getCompoundTagAt(i);
      tasks.add(CraftingTask.loadCraftingTaskFromNBT(stackTag));
    }
  }
  @Override
  public NBTTagCompound writeToNBT(NBTTagCompound compound) {
    super.writeToNBT(compound);
    en.writeToNBT(compound);
    NBTTagList tasksList = new NBTTagList();
    for (CraftingTask t : tasks) {
      NBTTagCompound stackTag = new NBTTagCompound();
      t.writeToNBT(stackTag);
      tasksList.appendTag(stackTag);
    }
    compound.setTag("tasks", tasksList);
    return compound;
  }
  private void addConnectables(final BlockPos pos) {
    if (pos == null || world == null)
      return;
    for (BlockPos bl : Util.getSides(pos)) {
      Chunk chunk = world.getChunkFromBlockCoords(bl);
      if (chunk == null || !chunk.isLoaded())
        continue;
      if (world.getTileEntity(bl) != null && world.getTileEntity(bl) instanceof TileMaster && !bl.equals(this.pos)) {
        world.getBlockState(bl).getBlock().dropBlockAsItem(world, bl, world.getBlockState(bl), 0);
        world.setBlockToAir(bl);
        world.removeTileEntity(bl);
        continue;
      }
      if (world.getTileEntity(bl) != null && world.getTileEntity(bl) instanceof IConnectable && !connectables.contains(bl)) {
        if (world.getTileEntity(bl) instanceof TileToggler && ((TileToggler) world.getTileEntity(bl)).isDisabled())
          continue;
        connectables.add(bl);
        ((IConnectable) world.getTileEntity(bl)).setMaster(this.pos);
        chunk.setChunkModified();
        addConnectables(bl);
      }
    }
  }
  private void addInventorys() {
    storageInventorys = Lists.newArrayList();
    fstorageInventorys = Lists.newArrayList();
    for (BlockPos cable : connectables) {
      if (world.getTileEntity(cable) instanceof AbstractFilterTile) {
        AbstractFilterTile s = (AbstractFilterTile) world.getTileEntity(cable);
        if (s.getInventory() != null && s.isStorage()) {
          BlockPos pos = s.getSource();
          if (world.getChunkFromBlockCoords(pos).isLoaded())
            storageInventorys.add(pos);
        }
        else if (s.getFluidTank() != null && s.isStorage()) {
          BlockPos pos = s.getSource();
          if (world.getChunkFromBlockCoords(pos).isLoaded())
            fstorageInventorys.add(pos);
        }
      }
    }
  }
  public void refreshNetwork() {
    if (world.isRemote)
      return;
    connectables = Sets.newHashSet();
    try {
      addConnectables(pos);
    }
    catch (Error e) {
      e.printStackTrace();
    }
    addInventorys();
    world.getChunkFromBlockCoords(pos).setChunkModified();
  }
  public void vacuum() {
    if ((world.getTotalWorldTime() + 0) % 30 != 0)
      return;
    for (BlockPos p : connectables) {
      if (world.getTileEntity(p) != null && world.getTileEntity(p) instanceof TileKabel && ((TileKabel) world.getTileEntity(p)).getKind() == Kind.vacuumKabel) {
        double range = 2.5;
        double x = p.getX() + .5;
        double y = p.getY();
        double z = p.getZ() + .5;
        List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(x - range, y - range, z - range, x + range + 1, y + range + 1, z + range + 1));
        for (EntityItem item : items) {
          if (item.ticksExisted < 40 || item.isDead || !consumeRF(item.getEntityItem().getCount(), true))
            continue;
          ItemStack stack = item.getEntityItem().copy();
          int rest = insertStack(stack, null, false);
          ItemStack r = stack.copy();
          r.setCount(rest);
          stack.shrink(rest);
          consumeRF(stack.getCount(), false);
          if (rest <= 0)
            item.setDead();
          else
            item.setEntityItemStack(r);
          break;
        }
      }
    }
  }
  public int insertStack(ItemStack stack, BlockPos source, boolean simulate) {
    if (stack == null||stack.isEmpty())
      return 0;
    List<AbstractFilterTile> invs = Lists.newArrayList();
    if (connectables == null)
      refreshNetwork();
    for (BlockPos p : connectables) {
      if (world.getTileEntity(p) instanceof AbstractFilterTile) {
        AbstractFilterTile tile = (AbstractFilterTile) world.getTileEntity(p);
        if (tile.isStorage() && tile.getInventory() != null) {
          invs.add(tile);
        }
      }
    }
    Collections.sort(invs, new Comparator<AbstractFilterTile>() {
      @Override
      public int compare(AbstractFilterTile o1, AbstractFilterTile o2) {
        return Integer.compare(o2.getPriority(), o1.getPriority());
      }
    });
    ItemStack in = stack.copy();
    for (AbstractFilterTile t : invs) {
      IItemHandler inv = t.getInventory();
      if (!InvHelper.contains(inv, in))
        continue;
      if (!t.canTransfer(in, Direction.IN))
        continue;
      if (t.getSource().equals(source))
        continue;
      ItemStack remain = ItemHandlerHelper.insertItemStacked(inv, in, simulate);
      if (remain == null || remain.isEmpty())
        return 0;
      in = ItemHandlerHelper.copyStackWithSize(in, remain.getCount());
      world.markChunkDirty(t.getSource(), world.getTileEntity(t.getSource()));
    }
    for (AbstractFilterTile t : invs) {
      IItemHandler inv = t.getInventory();
      if (InvHelper.contains(inv, in))
        continue;
      if (!t.canTransfer(in, Direction.IN))
        continue;
      if (t.getSource().equals(source))
        continue;
      ItemStack remain = ItemHandlerHelper.insertItem(inv, in, simulate);
      if (remain == null  ||remain.isEmpty() )
        return 0;
      in = ItemHandlerHelper.copyStackWithSize(in, remain.getCount());
      world.markChunkDirty(t.getSource(), world.getTileEntity(t.getSource()));
    }
    return in.getCount();
  }
//  public int insertFluid(FluidStack stack, BlockPos source, boolean simulate) {
//    if (stack == null)
//      return 0;
//    List<AbstractFilterTile> invs = Lists.newArrayList();
//    if (connectables == null)
//      refreshNetwork();
//    for (BlockPos p : connectables) {
//      if (world.getTileEntity(p) instanceof AbstractFilterTile) {
//        AbstractFilterTile tile = (AbstractFilterTile) world.getTileEntity(p);
//        if (tile.isStorage() && tile.getFluidTank() != null) {
//          invs.add(tile);
//        }
//      }
//    }
//    Collections.sort(invs, new Comparator<AbstractFilterTile>() {
//      @Override
//      public int compare(AbstractFilterTile o1, AbstractFilterTile o2) {
//        return Integer.compare(o2.getPriority(), o1.getPriority());
//      }
//    });
//    FluidStack in = stack.copy();
//    for (AbstractFilterTile t : invs) {
//      IFluidHandler inv = t.getFluidTank();
//      if (inv != null) {
//        if (!InvHelper.contains(inv, in))
//          continue;
//        if (!t.canTransfer(stack.getFluid(), Direction.IN))
//          continue;
//        if (t.getSource().equals(source))
//          continue;
//        int remain = in.amount - inv.fill(in, !simulate);
//        if (remain <= 0)
//          return 0;
//        in = new FluidStack(in.getFluid(), remain);
//        world.markChunkDirty(pos, this);
//      }
//    }
//    for (AbstractFilterTile t : invs) {
//      IFluidHandler inv = t.getFluidTank();
//      if (inv != null) {
//        if (InvHelper.contains(inv, in))
//          continue;
//        if (!t.canTransfer(stack.getFluid(), Direction.IN))
//          continue;
//        if (t.getSource().equals(source))
//          continue;
//        int remain = in.amount - inv.fill(in, !simulate);
//        if (remain <= 0)
//          return 0;
//        in = new FluidStack(in.getFluid(), remain);
//        world.markChunkDirty(pos, this);
//      }
//    }
//    return in.amount;
//  }
  public void impor() {
    List<TileKabel> invs = Lists.newArrayList();
    for (BlockPos p : connectables) {
      if (!(world.getTileEntity(p) instanceof TileKabel))
        continue;
      TileKabel tile = (TileKabel) world.getTileEntity(p);
      if (tile.getKind() == Kind.imKabel && tile.getInventory() != null) {
        invs.add(tile);
      }
    }
    Collections.sort(invs, new Comparator<TileKabel>() {
      @Override
      public int compare(TileKabel o1, TileKabel o2) {
        return Integer.compare(o2.getPriority(), o1.getPriority());
      }
    });
    for (TileKabel t : invs) {
      IItemHandler inv = t.getInventory();
      if ((world.getTotalWorldTime() + 10) % (30 / (t.elements(ItemUpgrade.SPEED) + 1)) != 0)
        continue;
      for (int i = 0; i < inv.getSlots(); i++) {
        ItemStack s = inv.getStackInSlot(i);
        if (s == null)
          continue;
        if (!t.canTransfer(s, Direction.OUT))
          continue;
        if (!t.status())
          continue;
        int num = s.getCount();
        int insert = Math.min(s.getCount(), (int) Math.pow(2, t.elements(ItemUpgrade.STACK) + 2));
        ItemStack extracted = inv.extractItem(i, insert, true);
        if (extracted == null || extracted.getCount() < insert)
          continue;
        if (!consumeRF(insert + t.elements(ItemUpgrade.SPEED), false))
          continue;
        int rest = insertStack(ItemHandlerHelper.copyStackWithSize(s, insert), t.getConnectedInventory(), false);
        inv.extractItem(i, insert - rest, false);
        world.markChunkDirty(pos, this);
        break;
      }
    }
  }
//  public void fimpor() {
//    List<TileKabel> invs = Lists.newArrayList();
//    for (BlockPos p : connectables) {
//      if (!(world.getTileEntity(p) instanceof TileKabel))
//        continue;
//      TileKabel tile = (TileKabel) world.getTileEntity(p);
//      if (tile.getKind() == Kind.fimKabel && tile.getFluidTank() != null) {
//        invs.add(tile);
//      }
//    }
//    Collections.sort(invs, new Comparator<TileKabel>() {
//      @Override
//      public int compare(TileKabel o1, TileKabel o2) {
//        return Integer.compare(o2.getPriority(), o1.getPriority());
//      }
//    });
//    for (TileKabel t : invs) {
//      IFluidHandler inv = t.getFluidTank();
//      if (inv != null) {
//        if ((world.getTotalWorldTime() + 10) % (30 / (t.elements(ItemUpgrade.SPEED) + 1)) != 0)
//          continue;
//        if (inv.getTankProperties() == null)
//          continue;
//        for (IFluidTankProperties i : inv.getTankProperties()) {
//          FluidStack s = i.getContents();
//          if (s == null)
//            continue;
//          if (!t.canTransfer(s.getFluid(), Direction.OUT))
//            continue;
//          if (!t.status())
//            continue;
//          FluidStack canDrain = inv.drain(s, false);
//          if (canDrain == null || canDrain.amount <= 0)
//            continue;
//          int num = s.amount;
//          int insert = Math.min(s.amount, 200 + t.elements(ItemUpgrade.STACK) * 200);
//          if (!consumeRF(insert + t.elements(ItemUpgrade.SPEED), false))
//            continue;
//          int rest = insertFluid(new FluidStack(s, insert), t.getSource(), false);
//          if (insert == rest)
//            continue;
//          inv.drain(new FluidStack(s.getFluid(), insert - rest), true);
//          world.markChunkDirty(pos, this);
//          break;
//        }
//      }
//    }
//  }
  public void export() {
    List<TileKabel> invs = Lists.newArrayList();
    for (BlockPos p : connectables) {
      if (!(world.getTileEntity(p) instanceof TileKabel))
        continue;
      TileKabel tile = (TileKabel) world.getTileEntity(p);
      if (tile.getKind() == Kind.exKabel && tile.getInventory() != null) {
        invs.add(tile);
      }
    }
    Collections.sort(invs, new Comparator<TileKabel>() {
      @Override
      public int compare(TileKabel o1, TileKabel o2) {
        return Integer.compare(o1.getPriority(), o2.getPriority());
      }
    });
    for (TileKabel t : invs) {
      IItemHandler inv = t.getInventory();
      if ((world.getTotalWorldTime() + 20) % (30 / (t.elements(ItemUpgrade.SPEED) + 1)) != 0)
        continue;
      for (int i = 0; i < 18; i++) {
        if (t.getFilter().get(i) == null)
          continue;
        boolean ore = t.getOre(i);
        boolean meta = t.getMeta(i);
        ItemStack fil = t.getFilter().get(i).getStack();
        if (fil == null)
          continue;
        if (storageInventorys.contains(t.getPos()))
          continue;
        ItemStack g = request(new FilterItem(fil, meta, ore, false), 1, true);
        if (g == null)
          continue;
        int m = g.getMaxStackSize();
        if ((t.elements(ItemUpgrade.STOCK) > 0))
          m = Math.min(m, t.getFilter().get(i).getSize() - InvHelper.getAmount(inv, new FilterItem(g, meta, ore, false)));
        if (m <= 0)
          continue;
        ItemStack max = ItemHandlerHelper.copyStackWithSize(g, m);
        ItemStack remain = ItemHandlerHelper.insertItemStacked(inv, max, true);
        int insert = remain == null ? max.getCount() : max.getCount() - remain.getCount();
        insert = Math.min(insert, (int) Math.pow(2, t.elements(ItemUpgrade.STACK) + 2));
        if (!t.status())
          continue;
        if (!consumeRF(insert + t.elements(ItemUpgrade.SPEED), true))
          continue;
        ItemStack rec = request(new FilterItem(g, meta, ore, false), insert, false);
        if (rec == null)
          continue;
        rec.shrink(t.elements(ItemUpgrade.SPEED));
        consumeRF(rec.getCount(), false);
        //				consumeRF(rec.stackSize + t.elements(ItemUpgrade.SPEED), false);
        ItemHandlerHelper.insertItemStacked(inv, rec, false);
        world.markChunkDirty(pos, this);
        break;
      }
    }
  }
//  public void fexport() {
//    List<TileKabel> invs = Lists.newArrayList();
//    for (BlockPos p : connectables) {
//      if (!(world.getTileEntity(p) instanceof TileKabel))
//        continue;
//      TileKabel tile = (TileKabel) world.getTileEntity(p);
//      if (tile.getKind() == Kind.fexKabel && tile.getFluidTank() != null) {
//        invs.add(tile);
//      }
//    }
//    Collections.sort(invs, new Comparator<TileKabel>() {
//      @Override
//      public int compare(TileKabel o1, TileKabel o2) {
//        return Integer.compare(o1.getPriority(), o2.getPriority());
//      }
//    });
//    for (TileKabel t : invs) {
//      IFluidHandler inv = t.getFluidTank();
//      if ((world.getTotalWorldTime() + 20) % (30 / (t.elements(ItemUpgrade.SPEED) + 1)) != 0)
//        continue;
//      for (int i = 0; i < 18; i++) {
//        if (t.getFilter().get(i) == null)
//          continue;
//        ItemStack fil = t.getFilter().get(i).getStack();
//        if (fil == null)
//          continue;
//        FluidStack fs = Util.getFluid(fil);
//        if (fs == null || fs.getFluid() == null)
//          continue;
//        Fluid f = fs.getFluid();
//        if (fstorageInventorys.contains(t.getPos()))
//          continue;
//        if (inv.fill(fs, false) <= 0)
//          continue;
//        if (!t.status())
//          continue;
//        int num = 200 + t.elements(ItemUpgrade.STACK) * 200;
//        num = Math.min(num, inv.fill(new FluidStack(f, num), false));
//        if (num <= 0)
//          continue;
//        FluidStack recs = frequest(f, num, true);
//        if (recs == null)
//          continue;
//        if (!consumeRF(num + t.elements(ItemUpgrade.SPEED), true))
//          continue;
//        FluidStack rec = frequest(f, num, false);
//        if (rec == null)
//          continue;
//        consumeRF(num + t.elements(ItemUpgrade.SPEED), false);
//        inv.fill(rec, true);
//        world.markChunkDirty(pos, this);
//        break;
//      }
//    }
//  }
  public ItemStack request(FilterItem fil, final int size, boolean simulate) {
    if (size == 0 || fil == null)
      return null;
    List<AbstractFilterTile> invs = Lists.newArrayList();
    for (BlockPos p : connectables) {
      if (world.getTileEntity(p) instanceof AbstractFilterTile) {
        AbstractFilterTile tile = (AbstractFilterTile) world.getTileEntity(p);
        if (tile.isStorage() && tile.getInventory() != null) {
          invs.add(tile);
        }
      }
    }
    ItemStack res = ItemStack.EMPTY;
    int result = 0;
    for (AbstractFilterTile t : invs) {
      IItemHandler inv = t.getInventory();
      for (int i = 0; i < inv.getSlots(); i++) {
        ItemStack s = inv.getStackInSlot(i);
        if (s == null || s.isEmpty() )
          continue;
        if (res != null && !res.isEmpty() && !ItemHandlerHelper.canItemStacksStack(s, res))
          continue;
        if (!fil.match(s))
          continue;
        if (!t.canTransfer(s, Direction.OUT))
          continue;
        
        
//        System.out.println("!TileMaster IS NOT EMPTY" + s + "_" + s.getCount());
        
        int miss = size - result;
        ItemStack extracted = inv.extractItem(i, Math.min(inv.getStackInSlot(i).getCount(), miss), simulate);
        world.markChunkDirty(pos, this);
        result += Math.min((extracted == null || extracted.isEmpty()) ? 0 : extracted.getCount(), miss);

//        System.out.println("!TileMaster extracted " + extracted);
//        //so we wan to take out 32 from s
//        System.out.println("!TileMaster SHRINK S BY THIS before" + s.getCount()+"    "+miss);
        res = extracted.copy();
        s.shrink(miss);
        int rest = s.getCount();
//        System.out.println("!TileMaster rest" + rest);
//        System.out.println("!TileMaster result" + result);
//        System.out.println("!TileMaster res+++++" + res);
        //				int rest = s.stackSize - miss;
 
//          System.out.println("!TileMaster COPY");
//          System.out.println("!TileMaster AFTER COPY" + res);
//        }
        //				inv.notifyAll();
        if (result == size)
          return ItemHandlerHelper.copyStackWithSize(res, size);
      }
    }
    if (result == 0)
      return ItemStack.EMPTY;
//    System.out.println("!TileMaster copy result " + result);
    return ItemHandlerHelper.copyStackWithSize(res, result);
  }
  public FluidStack frequest(Fluid fluid, final int size, boolean simulate) {
    if (size == 0 || fluid == null)
      return null;
    List<AbstractFilterTile> invs = Lists.newArrayList();
    for (BlockPos p : connectables) {
      if (world.getTileEntity(p) instanceof AbstractFilterTile) {
        AbstractFilterTile tile = (AbstractFilterTile) world.getTileEntity(p);
        if (tile.isStorage() && tile.getFluidTank() != null) {
          invs.add(tile);
        }
      }
    }
    Fluid res = null;
    int result = 0;
    for (AbstractFilterTile t : invs) {
      IFluidHandler inv = t.getFluidTank();
      if (inv.getTankProperties() == null)
        continue;
      for (IFluidTankProperties i : inv.getTankProperties()) {
        FluidStack s = i.getContents();
        if (s == null)
          continue;
        if (res != null && s.getFluid() != res)
          continue;
        if (s.getFluid() != fluid)
          continue;
        if (!t.canTransfer(fluid, Direction.OUT))
          continue;
        if (inv.drain(s, false) == null || inv.drain(s, false).amount <= 0)
          continue;
        int miss = size - result;
        result += Math.min(s.amount, miss);
        int rest = s.amount - miss;
        if (!simulate)
          inv.drain(new FluidStack(s.getFluid(), miss), true);
        world.markChunkDirty(pos, this);
        if (res == null)
          res = s.getFluid();
        if (result == size)
          return new FluidStack(res, size);
        // break;
      }
    }
    if (result == 0)
      return null;
    return new FluidStack(res, result);
  }
  @Override
  public void update() {
    if (world == null || world.isRemote)
      return;
    if (storageInventorys == null || fstorageInventorys == null || connectables == null) {
      refreshNetwork();
    }
    if (world.getTotalWorldTime() % (200) == 0) {
      refreshNetwork();
    }
    vacuum();
    impor();
    export();
//    fimpor();
//    fexport();
    // craft();
  }
  private void craft() {
    Iterator<CraftingTask> it = tasks.iterator();
    while (it.hasNext()) {
      CraftingTask t = it.next();
      if (t.getDone() >= t.getOutputSize())
        it.remove();
    }
    for (CraftingTask t : tasks) {
      if (t.progress(this))
        break;
    }
  }
  boolean consumeRF(int num, boolean simulate) {
    if (!ConfigHandler.energyNeeded)
      return true;
    int value = num * ConfigHandler.energyMultiplier + (connectables.size() / 15);
    if (en.getEnergyStored() < value)
      return false;
    if (!simulate) {
      en.modifyEnergyStored(-value);
    }
    return true;
  }
  @Override
  public SPacketUpdateTileEntity getUpdatePacket() {
    NBTTagCompound syncData = new NBTTagCompound();
    this.writeToNBT(syncData);
    return new SPacketUpdateTileEntity(this.pos, 1, syncData);
  }
  @Override
  public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
    readFromNBT(pkt.getNbtCompound());
  }
  @Override
  public int getEnergyStored(EnumFacing from) {
    return en.getEnergyStored();
  }
  @Override
  public int getMaxEnergyStored(EnumFacing from) {
    return en.getMaxEnergyStored();
  }
  @Override
  public boolean canConnectEnergy(EnumFacing from) {
    return true;
  }
  @Override
  public int receiveEnergy(EnumFacing from, int maxReceive, boolean simulate) {
    return en.receiveEnergy(maxReceive, simulate);
  }
  @Override
  public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
    return oldState.getBlock() != newSate.getBlock();
  }
}
