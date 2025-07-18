package info.rusty.webshoplink;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import info.rusty.webshoplink.DataTypes.*;
import com.google.gson.JsonObject;

import java.util.Map;

/**
 * Handles all inventory operations for the Webshoplink mod.
 */
public class InventoryManager {
    /**
     * Captures a snapshot of a player's inventory
     */
    public static InventorySnapshot captureInventory(Player player) {
        Inventory inventory = player.getInventory();
        int mainSize = inventory.getContainerSize() - inventory.armor.size() - inventory.offhand.size();
        ItemStack[] mainInventory = new ItemStack[mainSize];
        ItemStack[] armorInventory = new ItemStack[inventory.armor.size()];
        ItemStack[] offhandInventory = new ItemStack[inventory.offhand.size()];
        ItemStack[] enderChest = new ItemStack[player.getEnderChestInventory().getContainerSize()];
          // Copy all items to prevent reference issues
        for (int i = 0; i < mainSize; i++) {
            mainInventory[i] = inventory.getItem(i).copy();
            // Debug item NBT
            if (!inventory.getItem(i).isEmpty() && inventory.getItem(i).hasTag()) {
                NbtDebugUtils.logItemStackNbt(inventory.getItem(i), "Main inventory slot " + i);
            }
        }
        
        for (int i = 0; i < inventory.armor.size(); i++) {
            armorInventory[i] = inventory.armor.get(i).copy();
            // Debug item NBT
            if (!inventory.armor.get(i).isEmpty() && inventory.armor.get(i).hasTag()) {
                NbtDebugUtils.logItemStackNbt(inventory.armor.get(i), "Armor slot " + i);
            }
        }
        
        for (int i = 0; i < inventory.offhand.size(); i++) {
            offhandInventory[i] = inventory.offhand.get(i).copy();
            // Debug item NBT
            if (!inventory.offhand.get(i).isEmpty() && inventory.offhand.get(i).hasTag()) {
                NbtDebugUtils.logItemStackNbt(inventory.offhand.get(i), "Offhand slot " + i);
            }
        }
        
        for (int i = 0; i < player.getEnderChestInventory().getContainerSize(); i++) {
            enderChest[i] = player.getEnderChestInventory().getItem(i).copy();
            // Debug item NBT
            if (!player.getEnderChestInventory().getItem(i).isEmpty() && player.getEnderChestInventory().getItem(i).hasTag()) {
                NbtDebugUtils.logItemStackNbt(player.getEnderChestInventory().getItem(i), "Ender chest slot " + i);
            }
        }
        
        return new InventorySnapshot(mainInventory, armorInventory, offhandInventory, enderChest);
    }

    /**
     * Checks if two inventory snapshots match
     */
    public static boolean inventoriesMatch(InventorySnapshot snapshot, InventorySnapshot current) {
        // Check main inventory
        if (snapshot.getMainInventory().length != current.getMainInventory().length) {
            return false;
        }
        
        for (int i = 0; i < snapshot.getMainInventory().length; i++) {
            if (!ItemStack.matches(snapshot.getMainInventory()[i], current.getMainInventory()[i])) {
                return false;
            }
        }
        
        // Check armor
        if (snapshot.getArmorInventory().length != current.getArmorInventory().length) {
            return false;
        }
        
        for (int i = 0; i < snapshot.getArmorInventory().length; i++) {
            if (!ItemStack.matches(snapshot.getArmorInventory()[i], current.getArmorInventory()[i])) {
                return false;
            }
        }
        
        // Check offhand
        if (snapshot.getOffhandInventory().length != current.getOffhandInventory().length) {
            return false;
        }
        
        for (int i = 0; i < snapshot.getOffhandInventory().length; i++) {
            if (!ItemStack.matches(snapshot.getOffhandInventory()[i], current.getOffhandInventory()[i])) {
                return false;
            }
        }
        
        // Check enderChest
        if (snapshot.getEnderChest().length != current.getEnderChest().length) {
            return false;
        }
        
        for (int i = 0; i < snapshot.getEnderChest().length; i++) {
            if (!ItemStack.matches(snapshot.getEnderChest()[i], current.getEnderChest()[i])) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Applies a new inventory to a player
     */
    public static void applyNewInventory(ServerPlayer player, InventoryData newInventory) {
        // Log the operation
        DebugLogger.log("Applying new inventory to player: " + player.getName().getString(), Config.DebugVerbosity.MINIMAL);

        Map<Integer, ItemData> items = newInventory.getItems();        for (int i = 0; i < newInventory.getSize(); i++) {
            // Get the new item for this slot
            ItemData item = items.get(i);
            ItemStack currentStack = player.getInventory().getItem(i);
            
            // Log detailed comparison for debugging
            logItemComparison(currentStack, item, "Inventory", i);
            
            if (item != null) {
                // We have a new item to set - compare with current item
                boolean shouldUpdate = false;
                ItemStack newItemStack = item.getItemStackData();
                
                // Check if current item is empty or different from the new item
                if (currentStack.isEmpty()) {
                    shouldUpdate = !newItemStack.isEmpty();
                } else {
                    // Compare item type
                    String currentItemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(currentStack.getItem()).toString();
                    if (!currentItemId.equals(item.getItemId())) {
                        shouldUpdate = true;
                    } 
                    // Compare count
                    else if (currentStack.getCount() != item.getCount()) {
                        shouldUpdate = true;
                    } 
                    // Compare NBT data
                    else if ((currentStack.hasTag() && item.getNbt() == null) || 
                             (!currentStack.hasTag() && item.getNbt() != null)) {
                        shouldUpdate = true;
                        DebugLogger.log("NBT mismatch: one has NBT, other doesn't", Config.DebugVerbosity.DEFAULT);
                    }
                    else if (currentStack.hasTag() && item.getNbt() != null) {
                        // Convert current NBT to JSON for comparison
                        JsonObject currentNbtJson = (JsonObject) NbtSerializer.serializeNbt(currentStack.getTag());
                        shouldUpdate = !currentNbtJson.equals(item.getNbt());
                        
                        if (shouldUpdate) {
                            DebugLogger.log("NBT mismatch detected between current and new item", Config.DebugVerbosity.DEFAULT);
                        }
                    }
                }
                
                if (shouldUpdate) {
                    player.getInventory().setItem(i, newItemStack);
                    DebugLogger.log("Updated slot " + i + " to " + item.getItemId() + " x" + item.getCount(), Config.DebugVerbosity.DEFAULT);
                }
            } else if (!currentStack.isEmpty()) {
                // No item at this position in the new inventory, but slot is not empty - clear it
                player.getInventory().setItem(i, ItemStack.EMPTY);
                DebugLogger.log("Cleared slot " + i, Config.DebugVerbosity.DEFAULT);
            }
        }
    }

    public static void applyNewEchest(ServerPlayer player, ContainerData newEchest) {
        // Log the operation
        DebugLogger.log("Applying new E-Chest to player: " + player.getName().getString(), Config.DebugVerbosity.MINIMAL);

        Map<Integer, ItemData> items = newEchest.getItems();        for (int i = 0; i < newEchest.getSize(); i++) {
            // Get the new item for this slot
            ItemData item = items.get(i);
            ItemStack currentStack = player.getEnderChestInventory().getItem(i);
            
            // Log detailed comparison for debugging
            logItemComparison(currentStack, item, "E-Chest", i);
            
            if (item != null) {
                // We have a new item to set - compare with current item
                boolean shouldUpdate = false;
                ItemStack newItemStack = item.getItemStackData();
                
                // Check if current item is empty or different from the new item
                if (currentStack.isEmpty()) {
                    shouldUpdate = !newItemStack.isEmpty();
                } else {
                    // Compare item type
                    String currentItemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(currentStack.getItem()).toString();
                    if (!currentItemId.equals(item.getItemId())) {
                        shouldUpdate = true;
                    } 
                    // Compare count
                    else if (currentStack.getCount() != item.getCount()) {
                        shouldUpdate = true;
                    } 
                    // Compare NBT data
                    else if ((currentStack.hasTag() && item.getNbt() == null) || 
                             (!currentStack.hasTag() && item.getNbt() != null)) {
                        shouldUpdate = true;
                        DebugLogger.log("E-Chest NBT mismatch: one has NBT, other doesn't", Config.DebugVerbosity.DEFAULT);
                    }
                    else if (currentStack.hasTag() && item.getNbt() != null) {
                        // Convert current NBT to JSON for comparison
                        JsonObject currentNbtJson = (JsonObject) NbtSerializer.serializeNbt(currentStack.getTag());
                        shouldUpdate = !currentNbtJson.equals(item.getNbt());
                        
                        if (shouldUpdate) {
                            DebugLogger.log("E-Chest NBT mismatch detected between current and new item", Config.DebugVerbosity.DEFAULT);
                        }
                    }
                }
                
                if (shouldUpdate) {
                    player.getEnderChestInventory().setItem(i, newItemStack);
                    DebugLogger.log("Updated ender chest slot " + i + " to " + item.getItemId() + " x" + item.getCount(), Config.DebugVerbosity.DEFAULT);
                }
            } else if (!currentStack.isEmpty()) {
                // No item at this position in the new inventory, but slot is not empty - clear it
                player.getEnderChestInventory().setItem(i, ItemStack.EMPTY);
                DebugLogger.log("Cleared ender chest slot " + i, Config.DebugVerbosity.DEFAULT);
            }
        }
    }

    /**
     * Generates a diff between original and new inventory for display to the player
     */
    public static InventoryDiff generateInventoryDiff(InventorySnapshot original, InventoryData newInventory) {
        return generateInventoryDiff(original, newInventory, null);
    }
    
    /**
     * Generates a diff between original and new inventory including ender chest for display to the player
     */
    public static InventoryDiff generateInventoryDiff(InventorySnapshot original, InventoryData newInventory, ContainerData newEchest) {
        DebugLogger.log("Generating inventory diff", Config.DebugVerbosity.MINIMAL);
        
        InventoryDiff diff = new InventoryDiff();
        Map<Integer, ItemData> newItems = newInventory.getItems();
        
        // Create a map of items in the original inventory for easy lookup
        java.util.Map<String, Integer> originalItemCounts = new java.util.HashMap<>();
        
        // First, add all items from the main inventory
        for (int i = 0; i < original.getMainInventory().length; i++) {
            ItemStack stack = original.getMainInventory()[i];
            if (!stack.isEmpty()) {
                String itemKey = getItemKey(stack);
                originalItemCounts.put(itemKey, originalItemCounts.getOrDefault(itemKey, 0) + stack.getCount());
            }
        }
        
        // Add armor items
        for (ItemStack stack : original.getArmorInventory()) {
            if (!stack.isEmpty()) {
                String itemKey = getItemKey(stack);
                originalItemCounts.put(itemKey, originalItemCounts.getOrDefault(itemKey, 0) + stack.getCount());
            }
        }
        
        // Add offhand items
        for (ItemStack stack : original.getOffhandInventory()) {
            if (!stack.isEmpty()) {
                String itemKey = getItemKey(stack);
                originalItemCounts.put(itemKey, originalItemCounts.getOrDefault(itemKey, 0) + stack.getCount());
            }
        }
        
        // Add ender chest items
        for (ItemStack stack : original.getEnderChest()) {
            if (!stack.isEmpty()) {
                String itemKey = getItemKey(stack);
                originalItemCounts.put(itemKey, originalItemCounts.getOrDefault(itemKey, 0) + stack.getCount());
            }
        }
        
        // Create a map for the new inventory
        java.util.Map<String, Integer> newItemCounts = new java.util.HashMap<>();            // Process new inventory items
        for (int i = 0; i < newInventory.getSize(); i++) {
            ItemData itemData = newItems.get(i);
            if (itemData != null) {
                String itemKey = itemData.getItemId();
                if (itemData.getNbt() != null) {
                    // Use the NBT hash to make the key unique
                    itemKey += ":" + itemData.getNbt().hashCode();
                    DebugLogger.log("Generated new item key for " + itemKey + " with NBT hash: " + itemData.getNbt().hashCode(), Config.DebugVerbosity.ALL);
                }
                newItemCounts.put(itemKey, newItemCounts.getOrDefault(itemKey, 0) + itemData.getCount());
            }
        }
          // Process new ender chest items if provided
        if (newEchest != null) {
            Map<Integer, ItemData> newEchestItems = newEchest.getItems();
            for (int i = 0; i < newEchest.getSize(); i++) {
                ItemData itemData = newEchestItems.get(i);
                if (itemData != null) {
                    String itemKey = itemData.getItemId();
                    if (itemData.getNbt() != null) {
                        // Use the NBT hash to make the key unique
                        itemKey += ":" + itemData.getNbt().hashCode();
                        DebugLogger.log("Generated new echest item key for " + itemKey + " with NBT hash: " + itemData.getNbt().hashCode(), Config.DebugVerbosity.ALL);
                    }
                    newItemCounts.put(itemKey, newItemCounts.getOrDefault(itemKey, 0) + itemData.getCount());
                }
            }
        }
          
        // Compare original and new inventories to generate diff
        java.util.Set<String> allItems = new java.util.HashSet<>();
        allItems.addAll(originalItemCounts.keySet());
        allItems.addAll(newItemCounts.keySet());
        
        for (String itemKey : allItems) {
            int originalCount = originalItemCounts.getOrDefault(itemKey, 0);
            int newCount = newItemCounts.getOrDefault(itemKey, 0);
            int diff_count = newCount - originalCount;
              if (diff_count != 0) {
                // Use the full item key for better identification
                String itemId = itemKey;
                // Extract just the item ID part without the NBT data for display
                if (itemKey.contains(":nbt")) {
                    itemId = itemKey.substring(0, itemKey.indexOf(":nbt"));
                }
                
                if (diff_count > 0) {
                    diff.addItem(itemId, diff_count);
                } else {
                    diff.removeItem(itemId, -diff_count);
                }
            }
        }
        
        return diff;
    }

    /**
     * Helper method to generate a unique key for an item based on its ID and NBT data
     */
    private static String getItemKey(ItemStack stack) {
        String itemKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (stack.hasTag()) {
            // Use our custom NBT serializer to get a consistent JSON representation
            JsonObject nbtJson = (JsonObject) NbtSerializer.serializeNbt(stack.getTag());
            // Log for debugging
            DebugLogger.log("Generated item key for " + itemKey + " with NBT: " + nbtJson, Config.DebugVerbosity.ALL);
            // Add NBT hash to make the key unique for different NBT data
            itemKey += ":" + nbtJson.hashCode();
        }
        return itemKey;
    }

    /**
     * Logs detailed comparison of an item with its NBT data
     * @param current The current item in the inventory
     * @param itemData The item data from the API
     * @param slotType The type of slot (for logging purposes)
     * @param slotIndex The index of the slot
     */
    public static void logItemComparison(ItemStack current, ItemData itemData, String slotType, int slotIndex) {
        if (current.isEmpty() && itemData == null) {
            // Both empty, nothing to compare
            return;
        }
        
        if (current.isEmpty()) {
            DebugLogger.log(slotType + " slot " + slotIndex + ": Current is empty, new item is " + 
                    itemData.getItemId() + " x" + itemData.getCount(), Config.DebugVerbosity.DEFAULT);
            if (itemData.getNbt() != null) {
                DebugLogger.log("New item has NBT: " + itemData.getNbt(), Config.DebugVerbosity.DEFAULT);
                // Add enhanced NBT debugging
                NbtDebugUtils.logJsonNbt(itemData.getNbt(), "New item NBT");
            }
            return;
        }
        
        if (itemData == null) {
            String currentId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(current.getItem()).toString();
            DebugLogger.log(slotType + " slot " + slotIndex + ": Current is " + currentId + 
                    " x" + current.getCount() + ", new is empty", Config.DebugVerbosity.DEFAULT);
            if (current.hasTag()) {
                NbtDebugUtils.logItemStackNbt(current, "Current item being removed");
            }
            return;
        }
        
        // Both have items, compare them
        String currentId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(current.getItem()).toString();
        
        DebugLogger.log(slotType + " slot " + slotIndex + ": Comparing items", Config.DebugVerbosity.DEFAULT);
        DebugLogger.log("Current: " + currentId + " x" + current.getCount(), Config.DebugVerbosity.DEFAULT);
        DebugLogger.log("New: " + itemData.getItemId() + " x" + itemData.getCount(), Config.DebugVerbosity.DEFAULT);
        
        if (!currentId.equals(itemData.getItemId())) {
            DebugLogger.log("Item types are different", Config.DebugVerbosity.DEFAULT);
            return;
        }
        
        if (current.getCount() != itemData.getCount()) {
            DebugLogger.log("Item counts are different", Config.DebugVerbosity.DEFAULT);
        }
        
        // Compare NBT data
        if (current.hasTag() && itemData.getNbt() != null) {
            DebugLogger.log("Both items have NBT data, comparing...", Config.DebugVerbosity.DEFAULT);
            NbtDebugUtils.logItemStackNbt(current, "Current item");
            // Use our enhanced NBT debugging
            NbtDebugUtils.logJsonNbt(itemData.getNbt(), "New item from API");
            
            // Convert current NBT to JSON for comparison
            JsonObject currentNbtJson = (JsonObject) NbtSerializer.serializeNbt(current.getTag());
            boolean nbtMatch = currentNbtJson.equals(itemData.getNbt());
            
            if (nbtMatch) {
                DebugLogger.log("NBT data matches!", Config.DebugVerbosity.DEFAULT);
            } else {
                DebugLogger.log("NBT data differs!", Config.DebugVerbosity.DEFAULT);
                // Check what would happen if we apply the new NBT
                ItemStack testStack = current.copy();
                try {
                    CompoundTag testNbt = NbtSerializer.CompoundTagAdapter.parseJsonToCompoundTag(itemData.getNbt());
                    testStack.setTag(testNbt);
                    NbtDebugUtils.logItemStackNbt(testStack, "Test applying new NBT");
                    
                    // Compare original item to test item
                    boolean wouldMatch = testStack.getTag().equals(current.getTag());
                    DebugLogger.log("After applying new NBT data, items would " + 
                        (wouldMatch ? "MATCH" : "STILL DIFFER"), Config.DebugVerbosity.DEFAULT);
                } catch (Exception e) {
                    DebugLogger.logError("Failed to apply test NBT: " + e.getMessage(), e);
                }
            }
        } else if (current.hasTag()) {
            DebugLogger.log("Current item has NBT but new item doesn't", Config.DebugVerbosity.DEFAULT);
            NbtDebugUtils.logItemStackNbt(current, "Current item");
        } else if (itemData.getNbt() != null) {
            DebugLogger.log("New item has NBT but current doesn't", Config.DebugVerbosity.DEFAULT);
            NbtDebugUtils.logJsonNbt(itemData.getNbt(), "New item NBT");
        } else {
            DebugLogger.log("Neither item has NBT data", Config.DebugVerbosity.DEFAULT);
        }
    }

    /**
     * Compares two inventory snapshots and returns detailed information about the differences.
     * This is used to provide better feedback when inventory verification fails.
     * 
     * @param snapshot The original inventory snapshot
     * @param current The current inventory snapshot
     * @return A string describing the differences, or null if inventories match
     */
    public static String getInventoryDifferences(InventorySnapshot snapshot, InventorySnapshot current) {
        StringBuilder differences = new StringBuilder();
        boolean hasDifferences = false;
        
        // Check main inventory
        if (snapshot.getMainInventory().length != current.getMainInventory().length) {
            differences.append("Main inventory size changed. ");
            hasDifferences = true;
        } else {
            for (int i = 0; i < snapshot.getMainInventory().length; i++) {
                if (!ItemStack.matches(snapshot.getMainInventory()[i], current.getMainInventory()[i])) {
                    differences.append("Item in slot ").append(i).append(" changed. ");
                    hasDifferences = true;
                }
            }
        }
        
        // Check armor inventory
        if (snapshot.getArmorInventory().length != current.getArmorInventory().length) {
            differences.append("Armor inventory size changed. ");
            hasDifferences = true;
        } else {
            for (int i = 0; i < snapshot.getArmorInventory().length; i++) {
                if (!ItemStack.matches(snapshot.getArmorInventory()[i], current.getArmorInventory()[i])) {
                    differences.append("Armor in slot ").append(i).append(" changed. ");
                    hasDifferences = true;
                }
            }
        }
        
        // Check offhand inventory
        if (snapshot.getOffhandInventory().length != current.getOffhandInventory().length) {
            differences.append("Offhand inventory size changed. ");
            hasDifferences = true;
        } else {
            for (int i = 0; i < snapshot.getOffhandInventory().length; i++) {
                if (!ItemStack.matches(snapshot.getOffhandInventory()[i], current.getOffhandInventory()[i])) {
                    differences.append("Offhand item changed. ");
                    hasDifferences = true;
                }
            }
        }
        
        return hasDifferences ? differences.toString() : null;
    }
}
