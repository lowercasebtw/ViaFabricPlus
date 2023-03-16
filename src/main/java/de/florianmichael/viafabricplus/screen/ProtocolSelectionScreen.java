/*
 * This file is part of ViaFabricPlus - https://github.com/FlorianMichael/ViaFabricPlus
 * Copyright (C) 2021-2023 FlorianMichael/MrLookAtMe (EnZaXD) and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.florianmichael.viafabricplus.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.definition.bedrock.BedrockAccountManager;
import de.florianmichael.viafabricplus.screen.settings.SettingsScreen;
import de.florianmichael.viafabricplus.settings.groups.GeneralSettings;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import de.florianmichael.vialoadingbase.platform.InternalProtocolList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.NoticeScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.raphimc.mcauth.MinecraftAuth;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings({"DataFlowIssue", "DuplicatedCode"})
public class ProtocolSelectionScreen extends Screen {
    private final static ProtocolSelectionScreen INSTANCE = new ProtocolSelectionScreen();
    public Screen prevScreen;

    protected ProtocolSelectionScreen() {
        super(Text.literal("Protocol selection"));
    }

    public static void open(final Screen current) {
        INSTANCE.prevScreen = current;

        RenderSystem.recordRenderCall(() -> MinecraftClient.getInstance().setScreen(INSTANCE));
    }

    private ButtonWidget bedrockAuthentication;

    @Override
    protected void init() {
        super.init();

        this.addDrawableChild(new SlotList(this.client, width, height, 3 + 3 /* start offset */ + (textRenderer.fontHeight + 2) * 3 /* title is 2 */, height + 5, textRenderer.fontHeight + 4));
        this.addDrawableChild(ButtonWidget.builder(Text.literal("<-"), button -> this.close()).position(0, height - 20).size(20, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Settings"), button -> client.setScreen(SettingsScreen.get(this))).position(0, 0).size(98, 20).build());
        this.addDrawableChild(bedrockAuthentication = ButtonWidget.builder(getBedrockAuthenticationText(), button -> {
            CompletableFuture.runAsync(() -> {
                try {
                    BedrockAccountManager.INSTANCE.setAccount(MinecraftAuth.requestBedrockLogin(msaDeviceCode -> {
                        client.execute(() -> this.client.setScreen(new NoticeScreen(() -> {
                            ProtocolSelectionScreen.open(new MultiplayerScreen(new TitleScreen()));
                            Thread.currentThread().interrupt();
                        }, Text.literal("Microsoft Bedrock login"), Text.literal("Your webbrowser should've opened.\nPlease enter the following Code: " + msaDeviceCode.userCode() + "\nClosing this screen will cancel the process!"), Text.literal("Cancel"), false)));
                        try {
                            Util.getOperatingSystem().open(new URI(msaDeviceCode.verificationUri()));
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }
                    }));
                    ProtocolSelectionScreen.open(new MultiplayerScreen(new TitleScreen()));
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            });
        }).position(width - 98, 0).size(98, 20).build());
    }

    public Text getBedrockAuthenticationText() {
        if (BedrockAccountManager.INSTANCE.getAccount() != null) {
            return Text.literal(BedrockAccountManager.INSTANCE.getAccount().displayName());
        }
        return Text.literal("Set Bedrock Account");
    }

    @Override
    public void tick() {
        super.tick();
        if (bedrockAuthentication != null) {
            bedrockAuthentication.setMessage(getBedrockAuthenticationText());
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);

        matrices.push();
        matrices.scale(2F, 2F, 2F);
        drawCenteredTextWithShadow(matrices, textRenderer, "ViaFabricPlus", width / 4, 3, Color.ORANGE.getRGB());
        matrices.pop();
        drawCenteredTextWithShadow(matrices, textRenderer, "https://github.com/FlorianMichael/ViaFabricPlus", width / 2, (textRenderer.fontHeight + 2) * 2 + 3, -1);
    }

    @Override
    public void close() {
        client.setScreen(prevScreen);
    }

    public static class SlotList extends AlwaysSelectedEntryListWidget<ProtocolSlot> {

        public SlotList(MinecraftClient minecraftClient, int width, int height, int top, int bottom, int entryHeight) {
            super(minecraftClient, width, height, top, bottom, entryHeight);

            InternalProtocolList.getProtocols().stream().map(ProtocolSlot::new).forEach(this::addEntry);
        }
    }

    public static class ProtocolSlot extends AlwaysSelectedEntryListWidget.Entry<ProtocolSlot> {
        private final ProtocolVersion protocolVersion;

        public ProtocolSlot(final ProtocolVersion protocolVersion) {
            this.protocolVersion = protocolVersion;
        }

        @Override
        public Text getNarration() {
            return Text.literal(this.protocolVersion.getName());
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            ViaLoadingBase.getClassWrapper().reload(this.protocolVersion);
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            final boolean isSelected = ViaLoadingBase.getClassWrapper().getTargetVersion().getVersion() == protocolVersion.getVersion();

            matrices.push();
            matrices.translate(x, y - 1, 0);

            final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            drawCenteredTextWithShadow(matrices, textRenderer, this.protocolVersion.getName(), entryWidth / 2, entryHeight / 2 - textRenderer.fontHeight / 2, isSelected ? Color.GREEN.getRGB() : Color.RED.getRGB());
            matrices.pop();
        }
    }
}
