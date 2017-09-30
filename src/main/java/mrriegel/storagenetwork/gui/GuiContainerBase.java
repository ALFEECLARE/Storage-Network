package mrriegel.storagenetwork.gui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

public abstract class GuiContainerBase extends GuiContainer {
  public GuiContainerBase(Container inventorySlotsIn) {
    super(inventorySlotsIn);
  }
  private abstract class AbstractSlot {
    public int x, y, size, guiLeft, guiTop;
    public boolean number, square, smallFont, toolTip;
    protected Minecraft mc;
    public AbstractSlot(int x, int y, int size, int guiLeft, int guiTop, boolean number, boolean square, boolean smallFont, boolean toolTip) {
      super();
      this.x = x;
      this.y = y;
      this.size = size;
      this.guiLeft = guiLeft;
      this.guiTop = guiTop;
      this.number = number;
      this.square = square;
      this.smallFont = smallFont;
      this.toolTip = toolTip;
      mc = Minecraft.getMinecraft();
    }
    public boolean isMouseOverSlot(int mouseX, int mouseY) {
      return isPointInRegion(x - guiLeft, y - guiTop, 16, 16, mouseX, mouseY);
    }
    public abstract void drawSlot(int mx, int my);
    public abstract void drawTooltip(int mx, int my);
  }
  public class ItemSlot extends AbstractSlot {
    public ItemStack stack;
    public ItemSlot(ItemStack stack, int x, int y, int size, int guiLeft, int guiTop, boolean number, boolean square, boolean smallFont, boolean toolTip) {
      super(x, y, size, guiLeft, guiTop, number, square, smallFont, toolTip);
      this.stack = stack;
    }
    @Override
    public void drawSlot(int mx, int my) {
      GlStateManager.pushMatrix();
      if (stack != null && !stack.isEmpty()) {
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
        String amount = size < 1000 ? String.valueOf(size) : size < 1000000 ? size / 1000 + "K" : size / 1000000 + "M";
        if (number)
          if (smallFont) {
          GlStateManager.pushMatrix();
          GlStateManager.scale(.5f, .5f, .5f);
          mc.getRenderItem().renderItemOverlayIntoGUI(fontRenderer, stack, x * 2 + 16, y * 2 + 16, amount);
          GlStateManager.popMatrix();
          }
          else
            mc.getRenderItem().renderItemOverlayIntoGUI(fontRenderer, stack, x, y, amount);
      }
      if (square && this.isMouseOverSlot(mx, my)) {
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        int j1 = x;
        int k1 = y;
        GlStateManager.colorMask(true, true, true, false);
        drawGradientRect(j1, k1, j1 + 16, k1 + 16, -2130706433, -2130706433);
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
      }
      GlStateManager.popMatrix();
    }
    @Override
    public void drawTooltip(int mx, int my) {
      if (toolTip && this.isMouseOverSlot(mx, my) && stack != null && !stack.isEmpty()) {
        try {
          GlStateManager.pushMatrix();
          GlStateManager.disableLighting();
          renderToolTip(stack, mx - this.guiLeft, my - this.guiTop);
          GlStateManager.popMatrix();
          GlStateManager.enableLighting();
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }
}
