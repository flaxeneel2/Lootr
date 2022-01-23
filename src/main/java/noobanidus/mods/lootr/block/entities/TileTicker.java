package noobanidus.mods.lootr.block.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import noobanidus.mods.lootr.Lootr;
import noobanidus.mods.lootr.api.blockentity.ILootBlockEntity;
import noobanidus.mods.lootr.config.ConfigManager;
import noobanidus.mods.lootr.event.HandleChunk;
import noobanidus.mods.lootr.util.StructureUtil;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Lootr.MODID)
public class TileTicker {
  private final static Object listLock = new Object();
  private final static Object worldLock = new Object();
  private static boolean tickingList = false;
  private final static Set<Entry> tileEntries = new LinkedHashSet<>();
  private final static Set<Entry> pendingEntries = new LinkedHashSet<>();

  public static void addEntry(ResourceKey<Level> dimension, BlockPos position) {
    if (ConfigManager.isDimensionBlocked(dimension)) {
      return;
    }
    Entry newEntry = new Entry(dimension, position);
    synchronized (listLock) {
      if (tickingList) {
        pendingEntries.add(newEntry);
      } else {
        tileEntries.add(newEntry);
      }
    }
  }

  @SubscribeEvent
  public static void serverTick(TickEvent.ServerTickEvent event) {
    if (event.phase == TickEvent.Phase.END) {
      Set<Entry> toRemove = new HashSet<>();
      Set<Entry> copy;
      synchronized (listLock) {
        tickingList = true;
        copy = new HashSet<>(tileEntries);
        tickingList = false;
      }
      synchronized (worldLock) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        outer: for (Entry entry : copy) {
          ServerLevel level = server.getLevel(entry.getDimension());
          // TODO: Make this configurable
          if (entry.age(server) > (180 * 20)) {
            Lootr.LOG.error("Removed an entry older than three minutes: " + entry);
            toRemove.add(entry);
            continue;
          }
          if (level == null) {
            throw new IllegalStateException("got a null world for tile ticker in dimension " + entry.getDimension() + " at " + entry.getPosition());
          }
          if (!level.isAreaLoaded(entry.getPosition(), 1)) {
            toRemove.add(entry);
            continue;
          }
          for (ChunkPos pos : entry.getChunkPositions()) {
            synchronized (HandleChunk.LOADED_CHUNKS) {
              if (!HandleChunk.LOADED_CHUNKS.contains(pos)) {
                continue outer;
              }
            }
          }
          BlockEntity blockEntity = level.getBlockEntity(entry.getPosition());
          if (!(blockEntity instanceof RandomizableContainerBlockEntity be) || blockEntity instanceof ILootBlockEntity) {
            toRemove.add(entry);
            continue;
          }
          if (be.lootTable == null || ConfigManager.isBlacklisted(be.lootTable)) {
            toRemove.add(entry);
            continue;
          }
          if (!ConfigManager.getLootStructureBlacklist().isEmpty()) {
            StructureFeature<?> startAt = StructureUtil.featureFor(level, entry.getPosition());
            if (startAt != null && ConfigManager.getLootStructureBlacklist().contains(startAt.getRegistryName())) {
              toRemove.add(entry);
              continue;
            }
          }
          ResourceLocation table = be.lootTable;
          long seed = be.lootTableSeed;
          be.unpackLootTable(null);
          Clearable.tryClear(be);
          BlockState stateAt = level.getBlockState(entry.getPosition());
          BlockState replacement = ConfigManager.replacement(stateAt);
          if (replacement == null) {
            toRemove.add(entry);
            continue;
          }
          level.destroyBlock(entry.getPosition(), false);
          level.setBlock(entry.getPosition(), replacement, 2);
          blockEntity = level.getBlockEntity(entry.getPosition());
          if (blockEntity instanceof ILootBlockEntity) {
            ((RandomizableContainerBlockEntity) blockEntity).setLootTable(table, seed);
          } else {
            Lootr.LOG.error("replacement " + replacement + " is not an ILootTile " + entry.getDimension() + " at " + entry.getPosition());
          }

          toRemove.add(entry);
        }
      }
      synchronized (listLock) {
        tickingList = true;
        tileEntries.removeAll(toRemove);
        tileEntries.addAll(pendingEntries);
        tickingList = false;
        pendingEntries.clear();
      }
    }
  }

  public static class Entry {
    private final ResourceKey<Level> dimension;
    private final BlockPos position;
    private final Set<ChunkPos> chunks = new HashSet<>();
    private final long addedAt;

    public Entry(ResourceKey<Level> dimension, BlockPos position) {
      this.dimension = dimension;
      this.position = position;

      ChunkPos chunkPos = new ChunkPos(this.position);

      int oX = chunkPos.x;
      int oZ = chunkPos.z;
      chunks.add(chunkPos);

      for (int x = -2; x <= 2; x++) {
        for (int z = -2; z <= 2; z++) {
          chunks.add(new ChunkPos(oX + x, oZ + z));
        }
      }

      this.addedAt = ServerLifecycleHooks.getCurrentServer().getTickCount();
    }

    public ResourceKey<Level> getDimension() {
      return dimension;
    }

    public BlockPos getPosition() {
      return position;
    }

    public Set<ChunkPos> getChunkPositions() {
      return chunks;
    }

    public long age (MinecraftServer server) {
      return server.getTickCount() - addedAt;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Entry entry = (Entry) o;

      if (addedAt != entry.addedAt) return false;
      if (!dimension.equals(entry.dimension)) return false;
      return position.equals(entry.position);
    }

    @Override
    public int hashCode() {
      int result = dimension.hashCode();
      result = 31 * result + position.hashCode();
      result = 31 * result + (int) (addedAt ^ (addedAt >>> 32));
      return result;
    }

    @Override
    public String toString() {
      return "Entry{" +
          "dimension=" + dimension +
          ", position=" + position +
          ", addedAt=" + addedAt +
          '}';
    }
  }
}
