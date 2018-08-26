package com.playmonumenta.epicstructures.managers;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.util.HashMap;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.registry.WorldData;

import com.boydti.fawe.object.schematic.Schematic;

import com.playmonumenta.epicstructures.Plugin;

public class StructureManager {
	private HashMap<String, Schematic> mSchematics = new HashMap<String, Schematic>();
	Plugin mPlugin;
	org.bukkit.World mWorld;

	public StructureManager(Plugin plugin, org.bukkit.World world) {
		mPlugin = plugin;
		mWorld = world;
	}

	public Schematic loadSchematic(String baseFolderName, String baseName) throws Exception {
		if (baseFolderName == null || baseFolderName.isEmpty() || baseName == null || baseName.isEmpty()) {
			throw new Exception("Structure name is empty!");
		}

		Schematic schem;
		schem = mSchematics.get(baseFolderName + baseName);
		if (schem == null) {
			// Schematic not already loaded - need to read it from disk and load it into RAM

			final String fileName = _getFileName(baseFolderName, baseName);

			File file = new File(fileName);
			if (!file.exists()) {
				throw new Exception("Structure '" + baseName + "' does not exist");
			}

			schem = ClipboardFormat.SCHEMATIC.load(file);

			// Cache the schematic for fast access later
			mSchematics.put(baseFolderName + baseName, schem);
		}

		return schem;
	}

	// This code adapted from forum post here: https://www.spigotmc.org/threads/saving-schematics-to-file-with-worldedit-api.276078/
	public void saveSchematic(String baseFolderName, String baseName, Vector minpos, Vector maxpos, Runnable whenDone) throws Exception {
		if (baseFolderName == null || baseFolderName.isEmpty() || baseName == null || baseName.isEmpty()) {
			throw new Exception("Structure name is empty!");
		}

		final String fileName = _getFileName(baseFolderName, baseName);

		File file = new File(fileName);
		if (!file.exists()) {
			file.getParentFile().mkdirs();
			file.createNewFile();
		}

		World world = new BukkitWorld(mWorld);
		WorldData worldData = world.getWorldData();
		CuboidRegion cReg = new CuboidRegion(world, minpos, maxpos);

		BlockArrayClipboard clipboard = new BlockArrayClipboard(cReg);
		Extent source = WorldEdit.getInstance().getEditSessionFactory().getEditSession(world, -1);
		ForwardExtentCopy copy = new ForwardExtentCopy(source, cReg, clipboard.getOrigin(), clipboard, minpos);
		copy.setSourceMask(new ExistingBlockMask(source));

		// FAWE will accept null whenDone just fine. Last argument does nothing.
		Operations.completeSmart(copy, whenDone, false);

		ClipboardFormat.SCHEMATIC.getWriter(new FileOutputStream(file)).write(clipboard, worldData);

		// Re-load the schematic from disk into the cache
		mSchematics.remove(baseFolderName + baseName);
		loadSchematic(baseFolderName, baseName);
	}

	private String _getFileName(String baseFolderName, String baseName) {
		return Paths.get(mPlugin.getDataFolder().toString(), baseFolderName, baseName + ".schematic").toString();
	}
}
