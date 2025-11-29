/*
 * This file is licensed under the MIT License, part of Roughly Enough Items.
 * Copyright (c) 2018, 2019, 2020, 2021, 2022, 2023 shedaniel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.shedaniel.rei.impl.client.gui.craftable;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import me.shedaniel.rei.impl.client.ClientHelperImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class CraftableFilter {
    public static final CraftableFilter INSTANCE = new CraftableFilter();
    private static final int CHECK_INTERVAL = 5;
    private boolean dirty = false;
    private Long2LongMap invStacks = new Long2LongOpenHashMap();
    private Long2LongMap containerStacks = new Long2LongOpenHashMap();
    private long menuId = -2;
    private int tickCounter = 0;
    private long lastInvHash = 0;
    private long lastContainerHash = 0;
    
    public void markDirty() {
        dirty = true;
    }
    
    public boolean wasDirty() {
        if (dirty) {
            dirty = false;
            return true;
        }
        
        return false;
    }
    
    public void tick() {
        if (dirty) return;
        
        AbstractContainerMenu menu = Minecraft.getInstance().player.containerMenu;
        long currentMenuId = menu == null ? -1 : menu.containerId;
        if (currentMenuId != menuId) {
            menuId = currentMenuId;
            markDirty();
            return;
        }
        
        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) return;
        tickCounter = 0;
        
        Long2LongMap currentStacks;
        try {
            currentStacks = ClientHelperImpl.getInstance()._getInventoryItemsTypes();
        } catch (Throwable throwable) {
            currentStacks = Long2LongMaps.EMPTY_MAP;
        }
        long invHash = computeHash(currentStacks);
        if (invHash != lastInvHash) {
            lastInvHash = invHash;
            invStacks = currentStacks;
            markDirty();
            return;
        }
    
        try {
            currentStacks = ClientHelperImpl.getInstance()._getContainerItemsTypes();
        } catch (Throwable throwable) {
            currentStacks = Long2LongMaps.EMPTY_MAP;
        }
        long containerHash = computeHash(currentStacks);
        if (containerHash != lastContainerHash) {
            lastContainerHash = containerHash;
            containerStacks = currentStacks;
            markDirty();
        }
    }
    
    private static long computeHash(Long2LongMap map) {
        long hash = map.size();
        for (Long2LongMap.Entry entry : map.long2LongEntrySet()) {
            hash = hash * 31 + entry.getLongKey();
            hash = hash * 31 + entry.getLongValue();
        }
        return hash;
    }
    
    public Long2LongMap getInvStacks() {
        Long2LongOpenHashMap combined = new Long2LongOpenHashMap(invStacks);
        for (Long2LongMap.Entry entry : containerStacks.long2LongEntrySet()) {
            combined.mergeLong(entry.getLongKey(), entry.getLongValue(), Long::sum);
        }
        return combined;
    }
}
