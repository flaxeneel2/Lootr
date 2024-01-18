package noobanidus.mods.lootr.init;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import noobanidus.mods.lootr.advancement.AdvancementTrigger;
import noobanidus.mods.lootr.advancement.ContainerTrigger;
import noobanidus.mods.lootr.advancement.LootedStatTrigger;
import noobanidus.mods.lootr.api.LootrAPI;

public class ModAdvancements {
  private static final DeferredRegister<CriterionTrigger<?>> REGISTER = DeferredRegister.create(BuiltInRegistries.TRIGGER_TYPES, LootrAPI.MODID);

  public static final DeferredHolder<CriterionTrigger<?>, AdvancementTrigger> ADVANCEMENT = REGISTER.register("advancement", () -> new AdvancementTrigger());
  public static final DeferredHolder<CriterionTrigger<?>, ContainerTrigger> CHEST = REGISTER.register("chest_opened", () -> new ContainerTrigger());
  public static final DeferredHolder<CriterionTrigger<?>, ContainerTrigger> BARREL = REGISTER.register("barrel_opened", () -> new ContainerTrigger());
  public static final DeferredHolder<CriterionTrigger<?>, ContainerTrigger> CART = REGISTER.register("cart_opened", () -> new ContainerTrigger());
  public static final DeferredHolder<CriterionTrigger<?>, ContainerTrigger> SHULKER = REGISTER.register("shulker_opened", () -> new ContainerTrigger());
  public static final DeferredHolder<CriterionTrigger<?>, LootedStatTrigger> STAT = REGISTER.register("score", () -> new LootedStatTrigger());


  public static void register (IEventBus bus) {
    REGISTER.register(bus);
  }
}
