package com.elmakers.mine.bukkit.plugins.magic.spells;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerEvent;

import com.elmakers.mine.bukkit.plugins.magic.Spell;
import com.elmakers.mine.bukkit.plugins.magic.SpellEventType;
import com.elmakers.mine.bukkit.plugins.magic.SpellResult;
import com.elmakers.mine.bukkit.utilities.Target;
import com.elmakers.mine.bukkit.utilities.borrowed.ConfigurationNode;

public class FamiliarSpell extends Spell
{
	private String DEFAULT_FAMILIARS = "Chicken,Sheep,Cow,Pig,Wolf,Villager,MushroomCow,Ozelot,HorseEntity";
	private String DEFAULT_MONSTERS = "Creeper,PigZombie,Skeleton,Spider,Zombie,Giant,Silverfish,CaveSpider,Blaze,Bat,Witch";

	private final Random rand = new Random();
	private PlayerFamiliar familiars = new PlayerFamiliar();

	public enum FamiliarClass
	{
		SPECIFIC,
		ANY,
		FRIENDLY,
		MONSTER
	}

	public class PlayerFamiliar
	{
		public List<Creature> familiars = null;

		public boolean hasFamiliar()
		{
			return familiars != null;
		}

		public void setFamiliars(List<Creature> f)
		{
			familiars = f;
		}

		public void releaseFamiliar()
		{
			if (familiars != null)
			{
				for (Creature familiar : familiars)
				{
					familiar.setHealth(0);
				}
				familiars = null;
			}
		}

		public void releaseFamiliar(Entity entity)
		{
			if (familiars != null)
			{
				List<Creature> iterate = new ArrayList<Creature>(familiars);
				for (Creature familiar : iterate)
				{
					if (familiar.getUniqueId() == entity.getUniqueId()) {
						familiar.setHealth(0);
						familiars.remove(familiar);
					}
				}
				familiars = null;
			}
		}

		public boolean isFamiliar(Entity e)
		{
			if (familiars == null) return false;

			for (Creature c : familiars)
			{
				if (c.getEntityId() == e.getEntityId()) return true;
			}

			return false;
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public SpellResult onCast(ConfigurationNode parameters) 
	{
		if (!hasBuildPermission(getLocation().getBlock())) {
			return SpellResult.INSUFFICIENT_PERMISSION;
		}
		
		// For squid spawning
		noTargetThrough(Material.STATIONARY_WATER);
		noTargetThrough(Material.WATER);

		Target target = getTarget();
		if (target == null || !target.hasTarget())
		{
			castMessage("No target");
			return SpellResult.NO_TARGET;
		}
		Block originalTarget = target.getBlock(); 
		Block targetBlock = originalTarget;
		LivingEntity targetEntity = null;

		boolean hasFamiliar = familiars.hasFamiliar();

		if (hasFamiliar)
		{   // Dispel familiars if you target them and cast
			boolean isFamiliar = target.isEntity() && familiars.isFamiliar(target.getEntity());
			if (isFamiliar)
			{
				castMessage("You release your familiar(s)");
				checkListener();
				familiars.releaseFamiliar(target.getEntity());
				return SpellResult.CAST;
			}

			familiars.releaseFamiliar();
		}

		if (target.isEntity())
		{

			targetBlock = targetBlock.getRelative(BlockFace.SOUTH);
			Entity e = target.getEntity();
			if (e instanceof LivingEntity)
			{
				targetEntity = (LivingEntity)e;
			}
		}

		targetBlock = targetBlock.getRelative(BlockFace.UP);

		EntityType famType = EntityType.PIG;
		FamiliarClass famClass = FamiliarClass.FRIENDLY;
		int famCount = parameters.getInt("count", 1);

		String famTypeName = parameters.getString("type", "");

		if (famTypeName.equalsIgnoreCase("any"))
		{
			famClass = FamiliarClass.ANY;
		}
		else if (famTypeName.equalsIgnoreCase("mob"))
		{
			famClass = FamiliarClass.MONSTER;
		}
		else if (famTypeName.length() > 1)
		{
			// annoying- why do they have to CamelCase???
			String testType = famTypeName.toUpperCase();
			for (EntityType ct : EntityType.values())
			{
				String name = ct.getName();
				if (name != null && name.toUpperCase().equals(testType))
				{
					famType = ct;
					famClass = FamiliarClass.SPECIFIC;
				}
			}
		}

		if (originalTarget.getType() == Material.WATER || originalTarget.getType() == Material.STATIONARY_WATER)
		{
			famType = EntityType.SQUID;
			famClass = FamiliarClass.SPECIFIC;
		}

		List<Creature> newFamiliars = new ArrayList<Creature>();
		int spawnCount = 0;
		Location centerLoc = targetBlock.getLocation();
		for (int i = 0; i < famCount; i++)
		{
			if (famClass != FamiliarClass.SPECIFIC)
			{
				if (famClass == FamiliarClass.ANY)
				{
					int randomFamiliar = rand.nextInt(EntityType.values().length - 1);
					famType = EntityType.values()[randomFamiliar];                        
				}
				else
				{
					List<String> types = csv.parseStrings(DEFAULT_FAMILIARS);
					if (famClass == FamiliarClass.MONSTER)
					{
						types = csv.parseStrings(DEFAULT_MONSTERS);
					}
					int randomFamiliar = rand.nextInt(types.size());
					famType = EntityType.fromName(types.get(randomFamiliar));
				}
			}      

			Location targetLoc = centerLoc.clone();
			if (famCount > 1)
			{
				targetLoc.setX(targetLoc.getX() + rand.nextInt(2 * famCount) - famCount);
				targetLoc.setZ(targetLoc.getZ() + rand.nextInt(2 * famCount) - famCount);
			}
			if (famType != null) {
				Creature entity =  spawnFamiliar(targetLoc, famType, targetEntity);
				if (entity != null)
				{
					newFamiliars.add(entity);
					spawnCount++;
				}
			}
		}

		String typeMessage = "";
		if (famClass == FamiliarClass.SPECIFIC)
		{
			typeMessage = " " + famType.name().toLowerCase();
		}
		else if (famClass != FamiliarClass.ANY)
		{
			typeMessage = " " + famClass.name().toLowerCase();
		}
		castMessage("You create " + spawnCount + typeMessage +" familiar(s)!");
		familiars.setFamiliars(newFamiliars);
		checkListener();
		return SpellResult.CAST;

	}

	protected Creature spawnFamiliar(Location target, EntityType famType, LivingEntity targetEntity)
	{
		Creature familiar = null;
		try {
			Entity famEntity = getWorld().spawnEntity(target, famType);
			if (!(famEntity instanceof Creature)) return null;
	
			familiar = (Creature)famEntity;
			if (targetEntity != null)
			{
				familiar.setTarget(targetEntity);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return familiar;
	}

	protected void checkListener()
	{
		if (familiars.hasFamiliar())
		{
			controller.registerEvent(SpellEventType.PLAYER_QUIT, this);
		}
		else
		{
			controller.unregisterEvent(SpellEventType.PLAYER_QUIT, this);
		}
	}
	
	public void onPlayerQuit(PlayerEvent event)
	{
		if (familiars.hasFamiliar())
		{
			familiars.releaseFamiliar();
			checkListener();
		}
	}
}
