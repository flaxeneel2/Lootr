package noobanidus.mods.lootr.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import noobanidus.mods.lootr.api.LootrAPI;
import noobanidus.mods.lootr.config.ConfigManager;
import noobanidus.mods.lootr.entity.LootrChestMinecartEntity;
import noobanidus.mods.lootr.init.ModEntities;
import noobanidus.mods.lootr.network.OpenCart;
import noobanidus.mods.lootr.network.PacketHandler;
import noobanidus.mods.lootr.entity.EntityTicker;

@Mod.EventBusSubscriber(modid= LootrAPI.MODID)
public class HandleCart {
  @SubscribeEvent
  public static void onEntityJoin(EntityJoinLevelEvent event) {
    if (event.getEntity().getType() == EntityType.CHEST_MINECART) {
      MinecartChest chest = (MinecartChest) event.getEntity();
      if (!chest.level.isClientSide && chest.lootTable != null && ConfigManager.CONVERT_MINESHAFTS.get() && !ConfigManager.getLootBlacklist().contains(chest.lootTable)) {
        LootrChestMinecartEntity lootr = new LootrChestMinecartEntity(chest.level, chest.getX(), chest.getY(), chest.getZ());
        lootr.setLootTable(chest.lootTable, chest.lootTableSeed);
        event.setCanceled(true);
        EntityTicker.addEntity(lootr);
      }
    }
  }
}
