package net.tiffit.wynnforge.module;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.server.SPacketWindowItems;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.tiffit.wynnforge.PacketRecieveEvent;
import net.tiffit.wynnforge.data.LocalData;
import net.tiffit.wynnforge.module.ModuleBase.ModuleClass;
import net.tiffit.wynnforge.support.PluginJEI;
import net.tiffit.wynnforge.utils.WFUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mezz.jei.api.ingredients.VanillaTypes;

@ModuleClass(reqMod = "jei")
public class ModuleJEI extends ModuleBase {

    private NonNullList<ItemStack> ITEMS;
    private boolean doInitialAdd = false;

    public ModuleJEI() {
        super("jei");
    }

    @Override
    public void loadModule() {
        NBTTagCompound tag = LocalData.getTag(this);
        if (tag.hasKey("Items")) {
            NonNullList<ItemStack> copy = NonNullList.withSize(tag.getTagList("Items", 10).tagCount(), ItemStack.EMPTY);
            ITEMS = NonNullList.create();
            ItemStackHelper.loadAllItems(tag, copy);
            ITEMS.addAll(copy);
            doInitialAdd = true;
        } else {
            ITEMS = NonNullList.create();
            doInitialAdd = false;
        }
    }

    int tick = 0;

    @SubscribeEvent
    public void onUpdate(TickEvent.PlayerTickEvent e) {
        if (!WFUtils.isInWorld()) return;
        if (doInitialAdd) {
            doInitialAdd = false;
            PluginJEI.ingredientRegistry.addIngredientsAtRuntime(VanillaTypes.ITEM, ITEMS);
            checkPlayerInventories(e.player.inventory.mainInventory);
        }
        if (++tick > 5000) {
            tick = 0;
            checkPlayerInventories(e.player.inventory.mainInventory);
        }
    }

    @SubscribeEvent
    public void onSomeCheckAction(PacketRecieveEvent e) {
        if (!e.pre) {
            if (e.getPacket() instanceof SPacketWindowItems) {
                //Open Pouch and Bank
                Minecraft mc = Minecraft.getMinecraft();
                EntityPlayerSP player = mc.player;
                Container container = player.openContainer;
                if (container != null && mc.currentScreen instanceof GuiChest) {
                    GuiChest chest = (GuiChest) mc.currentScreen;
                    String chestName = chest.lowerChestInventory.getDisplayName().getUnformattedText();
                    if (chestName.endsWith("Pouch") || chestName.endsWith("Bank")) {
                        checkPlayerInventories(player.inventory.mainInventory);
                        checkPlayerInventories(container.inventoryItemStacks);
                    }

                }
            }
        }
    }


    private void checkPlayerInventories(NonNullList<ItemStack> playerMainInventory) {
        NBTTagCompound tag = LocalData.getTag(this);
        NBTTagCompound old = tag.copy();
        for (ItemStack is : playerMainInventory) {
            if (is.hasTagCompound() && is.getTagCompound().hasKey("display") && is.getTagCompound().getCompoundTag("display").hasKey("Lore")) {
                NBTTagList lore = is.getTagCompound().getCompoundTag("display").getTagList("Lore", 8);
                String firstLine = TextFormatting.getTextWithoutFormattingCodes(lore.getStringTagAt(0));
                if (firstLine.equals("Crafting Ingredient")) {
                    updateList(is);
                }
            }
        }
        ItemStackHelper.saveAllItems(tag, ITEMS, false);
        if (!tag.equals(old)) LocalData.save();
    }

    private void updateList(ItemStack stack) {
        stack = stack.copy();
        stack.setCount(1);
        List<ItemStack> old = new ArrayList<>(ITEMS);
        boolean found = false;
        for (int i = 0; i < ITEMS.size(); i++) {
            ItemStack is = ITEMS.get(i);
            if (ItemStack.areItemStacksEqual(is, stack)) return;
            if (is.getDisplayName().equals(stack.getDisplayName())) {
                ITEMS.set(i, stack);
                found = true;
                break;
            }
        }
        if (!found) {
            ITEMS.add(stack);
            PluginJEI.ingredientRegistry.addIngredientsAtRuntime(VanillaTypes.ITEM, Arrays.asList(stack));

            System.out.println("test");
        } else {
            PluginJEI.ingredientRegistry.removeIngredientsAtRuntime(VanillaTypes.ITEM, old);
            PluginJEI.ingredientRegistry.addIngredientsAtRuntime(VanillaTypes.ITEM, ITEMS);
        }
    }


}
