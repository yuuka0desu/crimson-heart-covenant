package com.narrafork.playermaid.inventory.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.narrafork.playermaid.MaidNetwork;
import com.narrafork.playermaid.WorkMode;
import com.narrafork.playermaid.inventory.MaidInventoryMenu;
import com.narrafork.playermaid.inventory.ViewType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** 复用 touhou_little_maid 1.5.2 运行时纹理的固定 256x256 玩家女仆界面。 */
public final class MaidInventoryScreen extends AbstractContainerScreen<MaidInventoryMenu> {
    private static final ResourceLocation MAIN = texture("maid_gui_main.png");
    private static final ResourceLocation TASK = texture("maid_gui_task.png");
    private static final ResourceLocation CONFIG = texture("maid_gui_config.png");
    private static final ResourceLocation BACKPACK = texture("maid_gui_backpack.png");
    private static final ResourceLocation BAUBLE = texture("maid_gui_bauble.png");
    private static final ResourceLocation CURIOS = texture("maid_gui_curios.png");
    private static final ResourceLocation ENDER = new ResourceLocation("touhou_little_maid", "textures/gui/maid_ender_chest.png");
    private MaidNetwork.Snapshot snapshot;

    public MaidInventoryScreen(MaidInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = MaidInventoryMenu.IMAGE_SIZE;
        imageHeight = MaidInventoryMenu.IMAGE_SIZE;
        titleLabelY = -1000;
        inventoryLabelY = -1000;
    }

    @Override protected void init() {
        super.init();
        snapshot = MaidNetwork.clientSnapshot(menu.getMaidId());
        addActionButton(51, 6, 48, Component.translatable("screen.playermaid.tab.main"), MaidNetwork.Action.OPEN_MAIN);
        addActionButton(103, 6, 48, Component.translatable("screen.playermaid.tab.task"), MaidNetwork.Action.OPEN_TASK);
        addActionButton(155, 6, 48, Component.translatable("screen.playermaid.tab.config"), MaidNetwork.Action.OPEN_CONFIG);
        addActionButton(7, 33, 38, Component.translatable("screen.playermaid.page.pack"), MaidNetwork.Action.OPEN_BACKPACK);
        addActionButton(7, 55, 38, Component.translatable("screen.playermaid.page.bauble"), MaidNetwork.Action.OPEN_BAUBLES);
        addActionButton(7, 77, 38, Component.translatable("screen.playermaid.page.curios"), MaidNetwork.Action.OPEN_CURIOS);
        addActionButton(7, 99, 38, Component.translatable("screen.playermaid.page.ender"), MaidNetwork.Action.OPEN_ENDER);

        switch (menu.getViewType()) {
            case MAIN -> {
                addActionButton(50, 184, 47, Component.translatable("screen.playermaid.home"), MaidNetwork.Action.TOGGLE_HOME);
                addActionButton(101, 184, 47, Component.translatable("screen.playermaid.pickup"), MaidNetwork.Action.TOGGLE_PICKUP);
                addActionButton(152, 184, 47, Component.translatable("screen.playermaid.ride"), MaidNetwork.Action.TOGGLE_RIDE);
                addActionButton(76, 208, 98, Component.translatable("screen.playermaid.schedule"), MaidNetwork.Action.CYCLE_SCHEDULE);
            }
            case TASK -> {
                WorkMode[] modes = WorkMode.values();
                for (int i = 0; i < modes.length; i++) {
                    int column = i % 3;
                    int row = i / 3;
                    WorkMode mode = modes[i];
                    addRenderableWidget(Button.builder(Component.translatable("playermaid.work." + mode.id()),
                                    button -> MaidNetwork.sendAction(menu.getMaidId(), MaidNetwork.Action.SELECT_TASK, mode.ordinal()))
                            .bounds(leftPos + 51 + column * 65, topPos + 43 + row * 15, 61, 14).build());
                }
                addActionButton(85, 169, 86, Component.translatable("screen.playermaid.auto"), MaidNetwork.Action.TOGGLE_AUTO);
            }
            case CONFIG -> {
                addActionButton(55, 43, 92, Component.translatable("screen.playermaid.config.backpack"), MaidNetwork.Action.TOGGLE_SHOW_BACKPACK);
                addActionButton(151, 43, 92, Component.translatable("screen.playermaid.config.backitem"), MaidNetwork.Action.TOGGLE_SHOW_BACK_ITEM);
                addActionButton(55, 67, 92, Component.translatable("screen.playermaid.config.bubble"), MaidNetwork.Action.TOGGLE_CHAT_BUBBLE);
                addActionButton(151, 67, 44, Component.literal("-"), MaidNetwork.Action.SOUND_DOWN);
                addActionButton(199, 67, 44, Component.literal("+"), MaidNetwork.Action.SOUND_UP);
                addActionButton(55, 91, 188, Component.translatable("screen.playermaid.config.picktype"), MaidNetwork.Action.CYCLE_PICK_TYPE);
                addActionButton(55, 115, 92, Component.translatable("screen.playermaid.config.door"), MaidNetwork.Action.TOGGLE_DOOR);
                addActionButton(151, 115, 92, Component.translatable("screen.playermaid.config.gate"), MaidNetwork.Action.TOGGLE_GATE);
                addActionButton(55, 139, 188, Component.translatable("screen.playermaid.config.climb"), MaidNetwork.Action.TOGGLE_CLIMBING);
            }
            default -> { }
        }
        MaidNetwork.sendAction(menu.getMaidId(), MaidNetwork.Action.REFRESH);
    }

    private void addActionButton(int x, int y, int width, Component label, MaidNetwork.Action action) {
        addRenderableWidget(Button.builder(label, button -> MaidNetwork.sendAction(menu.getMaidId(), action))
                .bounds(leftPos + x, topPos + y, width, 18).build());
    }

    public void onSnapshot(MaidNetwork.Snapshot value) { if (value.maidId().equals(menu.getMaidId())) snapshot = value; }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        ResourceLocation texture = switch (menu.getViewType()) {
            case MAIN -> MAIN;
            case TASK -> TASK;
            case CONFIG -> CONFIG;
            case COMBINED -> BACKPACK;
            case MAID_BAUBLES -> BAUBLE;
            case CURIOS -> CURIOS;
            case ENDER_CHEST -> ENDER;
        };
        graphics.blit(texture, leftPos, topPos, 0, 0, 256, 256, 256, 256);
        if (menu.getViewType() == ViewType.COMBINED || menu.getViewType() == ViewType.MAID_BAUBLES
                || menu.getViewType() == ViewType.CURIOS || menu.getViewType() == ViewType.ENDER_CHEST) {
            for (Slot slot : menu.slots) {
                int x = leftPos + slot.x - 1, y = topPos + slot.y - 1;
                graphics.fill(x, y, x + 18, y + 18, 0x88735C68);
                graphics.fill(x + 1, y + 1, x + 17, y + 17, 0x88F4E9EE);
            }
        }
        RenderSystem.disableBlend();
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (snapshot == null) return;
        int color = 0x5B3547;
        graphics.drawString(font, Component.translatable("screen.playermaid.name", snapshot.maidName()), 55, 29, color, false);
        switch (menu.getViewType()) {
            case MAIN -> {
                graphics.drawString(font, Component.translatable("screen.playermaid.health", (int) snapshot.health(), (int) snapshot.maxHealth()), 133, 52, color, false);
                graphics.drawString(font, Component.translatable("screen.playermaid.armor_level", snapshot.armor(), snapshot.experienceLevel()), 133, 64, color, false);
                graphics.drawString(font, Component.translatable("screen.playermaid.work", snapshot.taskName()), 133, 76, color, false);
                graphics.drawString(font, Component.translatable("screen.playermaid.favor", snapshot.favorability()), 133, 88, color, false);
                graphics.drawString(font, Component.translatable("screen.playermaid.relation", Component.translatable("playermaid.relation." + snapshot.relation().name().toLowerCase())), 133, 100, color, false);
                graphics.drawString(font, Component.translatable("screen.playermaid.mood", Component.translatable("playermaid.mood." + snapshot.mood().name().toLowerCase())), 133, 112, color, false);
                graphics.drawString(font, Component.translatable("screen.playermaid.bond", snapshot.bondLevel(), snapshot.bondPoints()), 133, 124, color, false);
                graphics.drawString(font, Component.translatable("screen.playermaid.home.state", onOff(snapshot.home())), 55, 164, color, false);
                graphics.drawString(font, Component.translatable("screen.playermaid.pickup.state", onOff(snapshot.pickup())), 122, 164, color, false);
                graphics.drawString(font, Component.translatable("screen.playermaid.ride.state", onOff(snapshot.rideable())), 187, 164, color, false);
                graphics.drawString(font, Component.translatable("screen.playermaid.schedule.state", Component.translatable("playermaid.schedule." + snapshot.schedule().name().toLowerCase())), 76, 231, color, false);
            }
            case TASK -> {
                graphics.drawString(font, Component.translatable("screen.playermaid.task.current", snapshot.taskName() + " [" + snapshot.taskUid() + "]"), 58, 49, color, false);
                graphics.drawString(font, Component.translatable("screen.playermaid.auto.state", onOff(snapshot.autoWork())), 58, 65, color, false);
                graphics.drawWordWrap(font, Component.translatable("screen.playermaid.task.hint"), 58, 88, 180, color);
            }
            case CONFIG -> {
                graphics.drawString(font, Component.translatable("screen.playermaid.config.sound", snapshot.soundFreq()), 151, 58, color, false);
                graphics.drawString(font, Component.translatable("screen.playermaid.config.picktype.state", snapshot.pickType()), 58, 179, color, false);
                graphics.drawString(font, Component.translatable("screen.playermaid.config.state", onOff(snapshot.showBackpack()), onOff(snapshot.showBackItem()), onOff(snapshot.showChatBubble()), onOff(snapshot.openDoor()), onOff(snapshot.openFenceGate()), onOff(snapshot.activeClimbing())), 55, 198, color, false);
            }
            default -> graphics.drawString(font, Component.translatable("screen.playermaid.owner_inventory"), 47, 143, color, false);
        }
    }

    private Component onOff(boolean value) { return Component.translatable(value ? "screen.playermaid.on" : "screen.playermaid.off"); }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (menu.getViewType() == ViewType.MAIN && minecraft != null && minecraft.level != null) {
            LivingEntity maid = (LivingEntity) minecraft.level.getPlayerByUUID(menu.getMaidId());
            if (maid != null) InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, leftPos + 91, topPos + 157, 48,
                    leftPos + 91 - mouseX, topPos + 85 - mouseY, maid);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    private static ResourceLocation texture(String file) {
        return new ResourceLocation("touhou_little_maid", "textures/gui/" + file);
    }
}
