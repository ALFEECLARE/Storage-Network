package com.lothrazar.storagenetwork.registry;

import java.util.List;
import com.lothrazar.storagenetwork.StorageNetworkMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SSNTab {

  private static final ResourceKey<CreativeModeTab> TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation(StorageNetworkMod.MODID, "tab"));

  @SubscribeEvent
  public static void onCreativeModeTabRegister(RegisterEvent event) {
    event.register(Registries.CREATIVE_MODE_TAB, helper -> {
      helper.register(TAB, CreativeModeTab.builder().icon(() -> new ItemStack(SsnRegistry.Blocks.REQUEST.get()))
          .title(Component.translatable("itemGroup." + StorageNetworkMod.MODID))
          .withLabelColor(0x00FF00)
          .displayItems((enabledFlags, populator) -> {
            List<ItemStack> stacks = SsnRegistry.ITEMS.getEntries().stream().map(reg -> new ItemStack(reg.get())).toList();
            populator.acceptAll(stacks);
          }).build());
    });
  }
}
