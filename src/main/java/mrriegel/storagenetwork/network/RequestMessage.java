package mrriegel.storagenetwork.network;

import io.netty.buffer.ByteBuf;

import java.util.List;

import mrriegel.storagenetwork.gui.remote.ContainerRemote;
import mrriegel.storagenetwork.gui.request.ContainerRequest;
import mrriegel.storagenetwork.helper.FilterItem;
import mrriegel.storagenetwork.helper.StackWrapper;
import mrriegel.storagenetwork.items.ItemRemote;
import mrriegel.storagenetwork.tile.TileMaster;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IThreadListener;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.items.ItemHandlerHelper;

public class RequestMessage implements IMessage, IMessageHandler<RequestMessage, IMessage> {
	int id;
	ItemStack stack;
	boolean shift, ctrl;

	public RequestMessage() {
	}

	public RequestMessage(int id, ItemStack stack, boolean shift, boolean ctrl) {
		this.id = id;
		this.stack = stack;
		this.shift = shift;
		this.ctrl = ctrl;

	}

	@Override
	public IMessage onMessage(final RequestMessage message, final MessageContext ctx) {
		IThreadListener mainThread = (WorldServer) ctx.getServerHandler().playerEntity.world;
		mainThread.addScheduledTask(new Runnable() {
			@Override
			public void run() {
				if (ctx.getServerHandler().playerEntity.openContainer instanceof ContainerRequest) {
					TileMaster tile = (TileMaster) ctx.getServerHandler().playerEntity.world.getTileEntity(((ContainerRequest) ctx.getServerHandler().playerEntity.openContainer).tile.getMaster());
					if (tile == null)
						return;
					int in = message.stack.isEmpty() ? 0 : tile.getAmount(new FilterItem(message.stack, true, false, true));
					ItemStack stack = message.stack.isEmpty() ? ItemStack.EMPTY : tile.request(new FilterItem(message.stack, true, false, true), message.id == 0 ? message.stack.getMaxStackSize() : message.ctrl ? 1 : Math.max(Math.min(message.stack.getMaxStackSize() / 2, in / 2), 1), false);
					if (!stack.isEmpty()) {
						if (message.shift) {
							ItemHandlerHelper.giveItemToPlayer(ctx.getServerHandler().playerEntity, stack);
						} else {
							ctx.getServerHandler().playerEntity.inventory.setItemStack(stack);
							PacketHandler.INSTANCE.sendTo(new StackMessage(stack), ctx.getServerHandler().playerEntity);
						}
					}
					List<StackWrapper> list = tile.getStacks();
					PacketHandler.INSTANCE.sendTo(new StacksMessage(list, tile.getCraftableStacks(list)), ctx.getServerHandler().playerEntity);
					ctx.getServerHandler().playerEntity.openContainer.detectAndSendChanges();
				} else if (ctx.getServerHandler().playerEntity.openContainer instanceof ContainerRemote) {
					TileMaster tile = ItemRemote.getTile(ctx.getServerHandler().playerEntity.inventory.getCurrentItem());
					if (tile == null)
						return;
					int in = message.stack.isEmpty() ? 0 : tile.getAmount(new FilterItem(message.stack, true, false, true));
					ItemStack stack = message.stack.isEmpty() ? ItemStack.EMPTY : tile.request(new FilterItem(message.stack, true, false, true), message.id == 0 ? message.stack.getMaxStackSize() : message.ctrl ? 1 : Math.max(Math.min(message.stack.getMaxStackSize() / 2, in / 2), 1), false);
				   if (!stack.isEmpty()) {
						if (message.shift) {
							ItemHandlerHelper.giveItemToPlayer(ctx.getServerHandler().playerEntity, stack);
						} else {
							ctx.getServerHandler().playerEntity.inventory.setItemStack(stack);
							PacketHandler.INSTANCE.sendTo(new StackMessage(stack), ctx.getServerHandler().playerEntity);
						}
					}
					List<StackWrapper> list = tile.getStacks();
					PacketHandler.INSTANCE.sendTo(new StacksMessage(list, tile.getCraftableStacks(list)), ctx.getServerHandler().playerEntity);
					ctx.getServerHandler().playerEntity.openContainer.detectAndSendChanges();
				}

			}
		});
		return null;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.id = buf.readInt();
		this.stack = ByteBufUtils.readItemStack(buf);
		this.shift = buf.readBoolean();
		this.ctrl = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(this.id);
		ByteBufUtils.writeItemStack(buf, this.stack);
		buf.writeBoolean(this.shift);
		buf.writeBoolean(this.ctrl);
	}
}
