package mrriegel.storagenetwork.blocks;

import java.util.List;

import mrriegel.storagenetwork.StorageNetwork;
import mrriegel.storagenetwork.api.IConnectable;
import mrriegel.storagenetwork.handler.GuiHandler;
import mrriegel.storagenetwork.helper.InvHelper;
import mrriegel.storagenetwork.init.ModBlocks;
import mrriegel.storagenetwork.init.ModItems;
import mrriegel.storagenetwork.tile.TileKabel;
import mrriegel.storagenetwork.tile.TileMaster;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockFKabel extends BlockKabel {
	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    ItemStack heldItem = playerIn.getHeldItem(hand);
    if (!(worldIn.getTileEntity(pos) instanceof TileKabel))
			return false;
		TileKabel tile = (TileKabel) worldIn.getTileEntity(pos);
		if (worldIn.isRemote)
			return true;
		if (/* tile.getMaster() == null || */(heldItem != null && (heldItem.getItem() == ModItems.coverstick || heldItem.getItem() == ModItems.duplicator)))
			return false;
		else if (tile.getKind().isFluid()) {
			playerIn.openGui(StorageNetwork.instance, GuiHandler.FCABLE, worldIn, pos.getX(), pos.getY(), pos.getZ());
			return true;

		}
		playerIn.openContainer.detectAndSendChanges();
		return false;
	}

	@Override
	boolean validInventory(IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
		return InvHelper.hasFluidHandler(worldIn, pos, side);
	}

	@Override
	protected Connect getConnect(IBlockAccess worldIn, BlockPos orig, BlockPos pos) {
		Block block = worldIn.getBlockState(pos).getBlock();
		Block ori = worldIn.getBlockState(orig).getBlock();
		if (worldIn.getTileEntity(pos) instanceof IConnectable || worldIn.getTileEntity(pos) instanceof TileMaster)
			return Connect.CONNECT;
		if (ori == ModBlocks.kabel || ori == ModBlocks.vacuumKabel)
			return Connect.NULL;
		EnumFacing face = get(orig, pos);
		if (!validInventory(worldIn, pos, face))
			return Connect.NULL;
		return Connect.STORAGE;
	}

	public static class Item extends ItemBlock {

		public Item(Block block) {
			super(block);
		}

		@Override
		public void addInformation(ItemStack stack, EntityPlayer playerIn, List<String> tooltip, boolean advanced) {
			super.addInformation(stack, playerIn, tooltip, advanced);
			if (stack.getItem() == net.minecraft.item.Item.getItemFromBlock(ModBlocks.fexKabel))
				tooltip.add(I18n.format("tooltip.storagenetwork.fkabel_E"));
			else if (stack.getItem() == net.minecraft.item.Item.getItemFromBlock(ModBlocks.fimKabel))
				tooltip.add(I18n.format("tooltip.storagenetwork.fkabel_I"));
			else if (stack.getItem() == net.minecraft.item.Item.getItemFromBlock(ModBlocks.fstorageKabel))
				tooltip.add(I18n.format("tooltip.storagenetwork.fkabel_S"));
			tooltip.add(I18n.format("tooltip.storagenetwork.networkNeeded"));
		}

	}
}
