/*
 * This file is part of ViaFabricPlus - https://github.com/FlorianMichael/ViaFabricPlus
 * Copyright (C) 2021-2023 FlorianMichael/EnZaXD and contributors
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
package de.florianmichael.viafabricplus.injection.mixin.base;

import de.florianmichael.viafabricplus.injection.access.ITextFieldWidget;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TextFieldWidget.class)
public class MixinTextFieldWidget implements ITextFieldWidget {

    @Unique
    private boolean viaFabricPlus$forbiddenCharactersUnlocked = false;

    @Redirect(method = "charTyped", at = @At(value = "INVOKE", target = "Lnet/minecraft/SharedConstants;isValidChar(C)Z"))
    public boolean allowForbiddenCharacters(char c) {
        if (this.viaFabricPlus$forbiddenCharactersUnlocked) {
            return true;
        }
        return SharedConstants.isValidChar(c);
    }

    @Redirect(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/SharedConstants;stripInvalidChars(Ljava/lang/String;)Ljava/lang/String;"))
    public String allowForbiddenCharacters(String string) {
        if (this.viaFabricPlus$forbiddenCharactersUnlocked) {
            return string;
        }
        return SharedConstants.stripInvalidChars(string);
    }

    @Override
    public void viaFabricPlus$unlockForbiddenCharacters() {
        this.viaFabricPlus$forbiddenCharactersUnlocked = true;
    }
}