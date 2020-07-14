package com.darkender.plugins.unstabletnt;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.type.TNT;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class UnstableTNT extends JavaPlugin implements Listener
{
    private NamespacedKey unstableFlag = new NamespacedKey(this, "unstable");
    private List<NamespacedKey> addedRecipes = new ArrayList<NamespacedKey>();
    
    @Override
    public void onEnable()
    {
        // Set up the itemstack with the persistent meta
        ItemStack unstableTNT = new ItemStack(Material.TNT);
        ItemMeta meta = unstableTNT.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "Unstable TNT");
        PersistentDataContainer persistData = meta.getPersistentDataContainer();
        persistData.set(unstableFlag, PersistentDataType.BYTE, (byte) 1);
        unstableTNT.setItemMeta(meta);
        
        int added = 0;
        for(Recipe regular : Bukkit.getRecipesFor(new ItemStack(Material.TNT)))
        {
            if(regular instanceof ShapedRecipe)
            {
                ShapedRecipe copyFrom = (ShapedRecipe) regular;
                
                NamespacedKey recipeKey = new NamespacedKey(this, "unstabletnt-" + (added + 1));
                ShapedRecipe clone = new ShapedRecipe(recipeKey, unstableTNT);
                
                // Copy the shape and items, just replace sand with soul sand
                boolean modified = false;
                clone.shape(copyFrom.getShape());
                for(Character c : copyFrom.getIngredientMap().keySet())
                {
                    if(copyFrom.getIngredientMap().get(c).getType() == Material.SAND)
                    {
                        clone.setIngredient(c, Material.SOUL_SAND);
                        modified = true;
                    }
                    else
                    {
                        clone.setIngredient(c, copyFrom.getIngredientMap().get(c).getType());
                    }
                }
                
                // Don't add the recipe if there were no changes
                if(modified)
                {
                    Bukkit.addRecipe(clone);
                    addedRecipes.add(recipeKey);
                    added++;
                }
            }
        }
        
        Bukkit.getLogger().info("Added " + added + " new recipe" + (added > 1 ? "s" : ""));
        getServer().getPluginManager().registerEvents(this, this);
    }
    
    @Override
    public void onDisable()
    {
        Bukkit.getLogger().info("Cleaning up " + addedRecipes.size() + " recipe" + (addedRecipes.size() > 1 ? "s" : ""));
        
        Iterator<Recipe> iter = getServer().recipeIterator();
        while (iter.hasNext())
        {
            Recipe check = iter.next();
            if(check instanceof ShapedRecipe)
            {
                if(addedRecipes.contains(((ShapedRecipe) check).getKey()))
                {
                    iter.remove();
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event)
    {
        if(event.isCancelled() || event.getBlockPlaced().getType() != Material.TNT)
        {
            return;
        }
        
        ItemMeta meta = event.getItemInHand().getItemMeta();
        if(meta == null)
        {
            return;
        }
        PersistentDataContainer data = meta.getPersistentDataContainer();
        if(data.has(unstableFlag, PersistentDataType.BYTE) && data.get(unstableFlag, PersistentDataType.BYTE) == (byte) 1)
        {
            TNT blockData = (TNT) event.getBlockPlaced().getBlockData();
            blockData.setUnstable(true);
            event.getBlockPlaced().setBlockData(blockData);
        }
    }
}
