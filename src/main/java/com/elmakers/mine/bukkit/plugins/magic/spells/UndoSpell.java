package com.elmakers.mine.bukkit.plugins.magic.spells;

import org.bukkit.block.Block;

import com.elmakers.mine.bukkit.plugins.magic.Spell;
import com.elmakers.mine.bukkit.plugins.magic.SpellResult;
import com.elmakers.mine.bukkit.utilities.borrowed.ConfigurationNode;

public class UndoSpell extends Spell
{
	@Override
	public SpellResult onCast(ConfigurationNode parameters) 
	{
		if (parameters.containsKey("player"))
		{
			String undoPlayer = parameters.getString("player");
			boolean undone = controller.undo(undoPlayer);
			if (undone)
			{
				sendMessage("You revert " + undoPlayer + "'s construction");
			}
			else
			{
				sendMessage("There is nothing to undo for " + undoPlayer);
			}
			return undone ? SpellResult.CAST : SpellResult.FAIL;
		}

		if (parameters.containsKey("type"))
		{
			String typeString = (String)parameters.getString("type");
			if (typeString.equals("commit"))
			{
				if (mage.commit()) {
					sendMessage("Undo queue cleared");
					return SpellResult.CAST;
				} else {
					castMessage("Nothing to commit");
					return SpellResult.FAIL;
				}
			}
			else if (typeString.equals("commitall"))
			{
				if (controller.commitAll()) {
					sendMessage("All undo queues cleared");
					return SpellResult.CAST;
				} else {
					castMessage("Nothing in any undo queues");
					return SpellResult.FAIL;
				}
			}
			boolean targetAll = typeString.equals("targetall");
			if (typeString.equals("target") || targetAll)
			{
				// targetThrough(Material.GLASS);
				Block target = getTargetBlock();
				if (target != null)
				{
					boolean undone = false;
					if (targetAll)
					{
						String playerName = controller.undoAny(target);
						if (playerName != null) 
						{
							undone = true;
							if (!mage.getName().equals(playerName))
							{
								mage.sendMessage("Undid one of " + playerName + "'s spells");
							}
						}
					}
					else
					{
						undone = mage.undo(target);
						if (undone) {
							sendMessage("You revert your construction");
						}
					}

					if (undone)
					{
						return SpellResult.CAST;
					}
				}
				return SpellResult.NO_TARGET;
			}
		}

		/*
		 * No target, or target isn't yours- just undo last
		 */
		boolean undone = controller.undo(getPlayer().getName());
		if (undone)
		{
			castMessage("You revert your construction");
		}
		else
		{
			castMessage("Nothing to undo");
		}
		return undone ? SpellResult.CAST : SpellResult.FAIL;	
	}
}
