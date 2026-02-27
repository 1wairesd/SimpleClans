package net.sacredlabyrinth.phaed.simpleclans.hooks.protection.providers;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.sacredlabyrinth.phaed.simpleclans.hooks.protection.Coordinate;
import net.sacredlabyrinth.phaed.simpleclans.hooks.protection.Land;
import net.sacredlabyrinth.phaed.simpleclans.hooks.protection.ProtectionProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

@SuppressWarnings("unused")
public class WorldGuard6Provider implements ProtectionProvider {

    private Object worldGuardInstance;
    private Method getRegionManager;
    private Method getApplicableRegions;
    private Method getPoints;
    private Method getX;
    private Method getZ;
    private boolean isWorldGuard7;

    @SuppressWarnings("JavaReflectionMemberAccess")
    @Override
    public void setup() throws NoSuchMethodException, ClassNotFoundException {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if (plugin == null) {
            throw new IllegalStateException("WorldGuard plugin not found");
        }

        // Try WorldGuard 7 first
        try {
            Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Method getInstance = worldGuardClass.getMethod("getInstance");
            worldGuardInstance = getInstance.invoke(null);
            
            Method getPlatform = worldGuardClass.getMethod("getPlatform");
            Object platform = getPlatform.invoke(worldGuardInstance);
            
            Method getRegionContainer = platform.getClass().getMethod("getRegionContainer");
            Object regionContainer = getRegionContainer.invoke(platform);
            
            getRegionManager = regionContainer.getClass().getMethod("get", 
                Class.forName("com.sk89q.worldedit.world.World"));
            
            // WorldGuard 7 uses BukkitAdapter
            Class<?> bukkitAdapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Method adaptWorld = bukkitAdapter.getMethod("adapt", World.class);
            Method asBlockVector = bukkitAdapter.getMethod("asBlockVector", Location.class);
            
            getApplicableRegions = RegionManager.class.getMethod("getApplicableRegions", 
                Class.forName("com.sk89q.worldedit.math.BlockVector3"));
            
            Class<?> blockVector2 = Class.forName("com.sk89q.worldedit.math.BlockVector2");
            getX = blockVector2.getMethod("getX");
            getZ = blockVector2.getMethod("getZ");
            
            isWorldGuard7 = true;
            return;
        } catch (Exception ignored) {
            // WorldGuard 7 not available, try WorldGuard 6
        }

        // WorldGuard 6
        try {
            Class<?> worldGuardPluginClass = Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
            Method inst = worldGuardPluginClass.getMethod("inst");
            worldGuardInstance = inst.invoke(null);
            getRegionManager = worldGuardPluginClass.getMethod("getRegionManager", World.class);
            getApplicableRegions = RegionManager.class.getMethod("getApplicableRegions", Location.class);
            getPoints = ProtectedRegion.class.getMethod("getPoints");
            Class<?> blockVector = Class.forName("com.sk89q.worldedit.BlockVector2");
            getX = blockVector.getMethod("getX");
            getZ = blockVector.getMethod("getZ");
            isWorldGuard7 = false;
        } catch (Exception e) {
            throw new NoSuchMethodException("Failed to initialize WorldGuard provider: " + e.getMessage());
        }
    }

    private @Nullable RegionManager getRegionManager(@Nullable World world) {
        if (world != null && worldGuardInstance != null) {
            try {
                if (isWorldGuard7) {
                    // WorldGuard 7: adapt world first
                    Class<?> bukkitAdapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
                    Method adaptWorld = bukkitAdapter.getMethod("adapt", World.class);
                    Object weWorld = adaptWorld.invoke(null, world);
                    return (RegionManager) getRegionManager.invoke(worldGuardInstance, weWorld);
                } else {
                    // WorldGuard 6: direct call
                    return (RegionManager) getRegionManager.invoke(worldGuardInstance, world);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Set<ProtectedRegion> getApplicableRegions(RegionManager regionManager, Location location) {
        try {
            Object result;
            if (isWorldGuard7) {
                // WorldGuard 7: convert location to BlockVector3
                Class<?> bukkitAdapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
                Method asBlockVector = bukkitAdapter.getMethod("asBlockVector", Location.class);
                Object blockVector = asBlockVector.invoke(null, location);
                result = getApplicableRegions.invoke(regionManager, blockVector);
            } else {
                // WorldGuard 6: direct location
                result = getApplicableRegions.invoke(regionManager, location);
            }
            
            if (result instanceof ApplicableRegionSet) {
                return ((ApplicableRegionSet) result).getRegions();
            } else if (result instanceof Iterable) {
                Set<ProtectedRegion> regions = new HashSet<>();
                for (Object region : (Iterable<?>) result) {
                    if (region instanceof ProtectedRegion) {
                        regions.add((ProtectedRegion) region);
                    }
                }
                return regions;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptySet();
    }

    @NotNull
    private Land getLand(ProtectedRegion region) {
        List<Coordinate> coordinates = getCoordinates(region);
        return new Land(getIdPrefix() + region.getId(), region.getOwners().getUniqueIds(), coordinates);
    }

    @NotNull
    private List<Coordinate> getCoordinates(ProtectedRegion region) {
        List<Coordinate> coordinates = new ArrayList<>();
        try {
            List<?> points;
            if (isWorldGuard7) {
                // WorldGuard 7: getPoints() returns List<BlockVector2> directly
                points = region.getPoints();
            } else {
                // WorldGuard 6: need to call getPoints() via reflection
                points = (List<?>) getPoints.invoke(region);
            }
            
            for (Object point : points) {
                double x = ((Number) getX.invoke(point)).doubleValue();
                double z = ((Number) getZ.invoke(point)).doubleValue();
                coordinates.add(new Coordinate(x, z));
            }
        } catch (ReflectiveOperationException ex) {
            ex.printStackTrace();
        }
        return coordinates;
    }

    @Override
    public @NotNull Set<Land> getLandsAt(@NotNull Location location) {
        HashSet<Land> lands = new HashSet<>();
        RegionManager regionManager = getRegionManager(location.getWorld());

        if (regionManager != null) {
            for (ProtectedRegion region : getApplicableRegions(regionManager, location)) {
                lands.add(getLand(region));
            }
        }
        return lands;
    }

    @Override
    public @NotNull Set<Land> getLandsOf(@NotNull OfflinePlayer player, @NotNull World world) {
        HashSet<Land> lands = new HashSet<>();
        RegionManager regionManager = getRegionManager(world);

        if (regionManager != null) {
            for (ProtectedRegion region : regionManager.getRegions().values()) {
                if (!region.getOwners().getUniqueIds().contains(player.getUniqueId())) {
                    continue;
                }
                lands.add(getLand(region));
            }
        }
        return lands;
    }

    @Override
    public @NotNull String getIdPrefix() {
        return "wg";
    }

    @Override
    public void deleteLand(@NotNull String id, @NotNull World world) {
        id = id.replaceFirst(getIdPrefix(), "");
        RegionManager regionManager = getRegionManager(world);
        if (regionManager != null) {
            regionManager.removeRegion(id);
        }
    }

    @Override
    public @Nullable Class<? extends Event> getCreateLandEvent() {
        return null;
    }

    @Override
    public @Nullable Player getPlayer(Event event) {
        return null;
    }

    @Override
    public @Nullable String getRequiredPluginName() {
        return "WorldGuard";
    }
}
