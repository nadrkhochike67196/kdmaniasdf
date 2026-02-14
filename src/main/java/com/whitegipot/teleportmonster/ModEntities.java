package com.whitegipot.teleportmonster;

import com.whitegipot.teleportmonster.entity.TeleportMonsterEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = 
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TeleportMonsterMod.MOD_ID);

    public static final RegistryObject<EntityType<TeleportMonsterEntity>> TELEPORT_MONSTER = 
            ENTITIES.register("teleport_monster", () -> EntityType.Builder.of(TeleportMonsterEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build("teleport_monster"));
}
