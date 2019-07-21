package com.lothrazar.storagenetwork.gui;
import com.lothrazar.storagenetwork.api.util.UtilInventory;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

/**
 * used as the MAIN grid in the network item display
 * <p>
 * also as ghost/filter items in the cable filter slots
 */
public class ItemSlotNetwork {

  private final int x;
  private final int y;

  private  int size;
  private final int guiLeft;
  private final int guiTop;
  private boolean showNumbers;
  private final IGuiPrivate parent;
  private ItemStack stack;
//TODO: Interface for parent expose isInRegion and drawgradient rect and the tooltip one
  public ItemSlotNetwork(IGuiPrivate parent, @Nonnull ItemStack stack, int x, int y, int size, int guiLeft, int guiTop, boolean number) {
    this.x = x;
    this.y = y;
    this.size = size;
    this.guiLeft = guiLeft;
    this.guiTop = guiTop;
    setShowNumbers(number);
    this.parent = parent;
    setStack(stack);
  }

  public boolean isMouseOverSlot(int mouseX, int mouseY) {
    return parent.isInRegion(x - guiLeft, y - guiTop, 16, 16, mouseX, mouseY);
  }

  public void drawSlot(FontRenderer font,int mx, int my) {
    //     TODO: renderItem and keyboard isKeyDown issues
    GlStateManager.pushMatrix();
    if (!getStack().isEmpty()) {
      RenderHelper.enableGUIStandardItemLighting();
      Minecraft.getInstance().getItemRenderer().renderItemAndEffectIntoGUI(getStack(), x, y);
      String amount;
      //cant sneak in gui
      //default to short form, show full amount if sneak
      if (Screen.hasShiftDown()) {
        amount = size + "";
      }
      else {
        amount = UtilInventory.formatLargeNumber(size);
      }
      if (isShowNumbers()) {
        GlStateManager.pushMatrix();
        GlStateManager.scalef(.5f, .5f, .5f);
        Minecraft.getInstance().getItemRenderer().renderItemOverlayIntoGUI(font, stack,
            x * 2 + 16,
            y * 2 + 16, amount);
        GlStateManager.popMatrix();
      }
    }
    if (isMouseOverSlot(mx, my)) {
      GlStateManager.disableLighting();
      GlStateManager.disableDepthTest();
      //GlStateManager.disableDepth();
      int j1 = x;
      int k1 = y;
      GlStateManager.colorMask(true, true, true, false);
      parent.drawGradientRect(j1, k1, j1 + 16, k1 + 16, -2130706433, -2130706433);
      GlStateManager.colorMask(true, true, true, true);
      GlStateManager.enableLighting();
      // GlStateManager.enableDepth();
    }
    GlStateManager.popMatrix();
  }

  void drawTooltip(int mx, int my) {
    if (isMouseOverSlot(mx, my) && !getStack().isEmpty()) {
      parent.renderStackToolTip(getStack(),
          mx - parent.getGuiLeft(),
          my - parent.getGuiTop());
    }
  }

  public ItemStack getStack() {
    return stack;
  }

  public void setStack(ItemStack stack) {
    this.stack = stack;
  }
  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }


  private boolean isShowNumbers() {
    return showNumbers;
  }

  private void setShowNumbers(boolean showNumbers) {
    this.showNumbers = showNumbers;
  }
}
