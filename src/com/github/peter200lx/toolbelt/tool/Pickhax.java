package com.github.peter200lx.toolbelt.tool;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import com.github.peter200lx.toolbelt.Tool;

public class Pickhax extends Tool  {

	public Pickhax(String modName, Server server, boolean debug,
			boolean permissions, boolean useEvent) {
		super(modName, server, debug, permissions, permissions);
	}

	public static String name = "phax";

	private HashMap<String, Long> pCooldown = new HashMap<String, Long>();

	private Integer range;

	@Override
	public String getToolName() {
		return name;
	}

	@Override
	public void handleInteract(PlayerInteractEvent event){
		Player subject = event.getPlayer();
		if(pCooldown.containsKey(subject.getName()) &&
				(System.currentTimeMillis() < (pCooldown.get(subject.getName())+500)))
			return;
		pCooldown.put(subject.getName(), System.currentTimeMillis());
		Boolean physics = false;
		Block target;
		switch(event.getAction()) {
		case LEFT_CLICK_BLOCK:
			physics = true;
		case RIGHT_CLICK_BLOCK:
			target = event.getClickedBlock();
			break;
		case LEFT_CLICK_AIR:
			physics = true;
		case RIGHT_CLICK_AIR:
			if(subject.isSneaking()&&hasRangePerm(subject)&&(range > 0))
				target = subject.getTargetBlock(null, range);
			else if(range <= 0){
				subject.sendMessage(ChatColor.RED+"Ranged block removal isn't enabled");
				return;
			}else if(!hasRangePerm(subject)) {
				subject.sendMessage(ChatColor.RED+"You don't have ranged delete permission");
				return;
			}else {
				subject.sendMessage(ChatColor.RED+"Sorry, didn't catch that, try crouching");
				return;
			}
			break;
		default:
			return;
		}
		if(target != null && !stopOverwrite.contains(target.getType())       &&
				(onlyAllow.isEmpty() || onlyAllow.contains(target.getType())) ){
			if(spawnBuild(target,event.getPlayer())) {
				if(isUseEvent())
					if(safeBreak(target,event.getPlayer(),physics))
						subject.sendBlockChange(target.getLocation(), 0, (byte)0);
				else {
					target.setTypeId(0,physics);
					subject.sendBlockChange(target.getLocation(), 0, (byte)0);
				}
			}
		}else if((target != null)&&!target.getType().equals(Material.AIR)) {
			event.getPlayer().sendMessage(ChatColor.RED + "You can't insta-delete "+
					ChatColor.GOLD+target.getType());
		}
	}

	private boolean hasRangePerm(CommandSender subject) {
		if(isPermissions())
			return subject.hasPermission(getPermStr()+".range");
		else
			return true;
	}

	@Override
	public boolean printUse(CommandSender sender) {
		if(hasPerm(sender)) {
			sender.sendMessage("Click with the "+ChatColor.GOLD+getType()+
					ChatColor.WHITE+" to delete a block (Right-click for no-physics)");
			return true;
		}
		return false;
	}

	@Override
	public boolean loadConf(String tSet, FileConfiguration conf) {

		//Load the default restriction configuration
		if(!loadGlobalRestrictions(tSet,conf))
			return false;

		range = conf.getInt(tSet+"."+name+".range", 25);
		if(isDebug())
			log.info("["+modName+"][loadConf] Crouched PickHax range distance is set to "+range);

		List<Integer> intL = conf.getIntegerList(tSet+"."+name+".onlyAllow");

		if(!intL.isEmpty())
		{
			if(isDebug())
				log.info( "["+modName+"][loadConf] As "+name+".onlyAllow has items,"+
						" it overwrites the global");

			onlyAllow = loadMatList(intL,new HashSet<Material>(),tSet+"."+name+".onlyAllow");
			if(onlyAllow == null)
				return false;

			if(isDebug()) {
				logMatSet(onlyAllow,"loadGlobalRestrictions",name+".onlyAllow:");
				log.info( "["+modName+"][loadConf] As "+name+".onlyAllow has items,"+
						" only those materials are usable");
			}
		} else if(isDebug()&& !onlyAllow.isEmpty()) {
			log.info( "["+modName+"][loadConf] As global.onlyAllow has items,"+
					" only those materials are usable");
		}

		intL = conf.getIntegerList(tSet+"."+name+".stopOverwrite");

		if(!intL.isEmpty())
		{
			if(isDebug())
				log.info( "["+modName+"][loadConf] As "+name+".stopOverwrite has items,"+
						" it overwrites the global");

			stopOverwrite = loadMatList(intL,defStop(),tSet+"."+name+".stopOverwrite");
			if(stopOverwrite == null)
				return false;

			if(isDebug()) logMatSet(stopOverwrite,"loadGlobalRestrictions",
					name+".stopOverwrite:");
		}
		return true;
	}
}
