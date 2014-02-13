package com.elmakers.mine.bukkit.plugins.magic.spells;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.elmakers.mine.bukkit.blocks.BlockList;
import com.elmakers.mine.bukkit.blocks.MaterialBrush;
import com.elmakers.mine.bukkit.plugins.magic.BrushSpell;
import com.elmakers.mine.bukkit.plugins.magic.SpellResult;
import com.elmakers.mine.bukkit.utilities.borrowed.ConfigurationNode;

public class PillarSpell extends BrushSpell 
{
	int MAX_SEARCH_DISTANCE = 255;

	@Override
	public SpellResult onCast(ConfigurationNode parameters) 
	{
		Block attachBlock = getTargetBlock();
		if (attachBlock == null)
		{
			castMessage("No target");
			return SpellResult.NO_TARGET;
		}	

		BlockFace direction = BlockFace.UP;	
		String typeString = parameters.getString("type", "");
		if (typeString.equals("down"))
		{
			direction = BlockFace.DOWN;
		}

		Block targetBlock = attachBlock.getRelative(direction);
		int distance = 0;

		if (!hasBuildPermission(targetBlock)) {
			return SpellResult.INSUFFICIENT_PERMISSION;
		}
		while (isTargetable(targetBlock.getType()) && distance <= MAX_SEARCH_DISTANCE)
		{
			distance++;
			attachBlock = targetBlock;
			targetBlock = attachBlock.getRelative(direction);
		}
		if (isTargetable(targetBlock.getType()))
		{
			castMessage("Can't pillar any further");
			return SpellResult.NO_TARGET;
		}
		if (!hasBuildPermission(targetBlock)) {
			return SpellResult.INSUFFICIENT_PERMISSION;
		}

		MaterialBrush buildWith = getMaterialBrush();

		BlockList pillarBlocks = new BlockList();
		Block pillar = getBlockAt(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ());
		buildWith.setTarget(attachBlock.getLocation(), pillar.getLocation());
		buildWith.update(mage, pillar.getLocation());
		pillarBlocks.add(pillar);
		buildWith.modify(pillar);

		castMessage("Creating a pillar of " + attachBlock.getType().name().toLowerCase());
		
		pillarBlocks.setTimeToLive(parameters.getInt("undo", 0));
		
		mage.registerForUndo(pillarBlocks);
		controller.updateBlock(pillar);
		
		return SpellResult.CAST;
	}
}
