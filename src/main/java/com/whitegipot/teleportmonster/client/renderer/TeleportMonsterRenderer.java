package com.whitegipot.teleportmonster.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.whitegipot.teleportmonster.TeleportMonsterMod;
import com.whitegipot.teleportmonster.client.model.TeleportMonsterModel;
import com.whitegipot.teleportmonster.entity.TeleportMonsterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class TeleportMonsterRenderer extends MobRenderer<TeleportMonsterEntity, TeleportMonsterModel> {
    private static final ResourceLocation TEXTURE = 
            new ResourceLocation(TeleportMonsterMod.MOD_ID, "textures/entity/teleport_monster.png");

    public TeleportMonsterRenderer(EntityRendererProvider.Context context) {
        super(context, new TeleportMonsterModel(context.bakeLayer(TeleportMonsterModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(TeleportMonsterEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(TeleportMonsterEntity entity, PoseStack poseStack, float partialTick) {
        if (entity.isTeleporting()) {
            float scale = 1.0F + (float)Math.sin(entity.tickCount * 0.5F) * 0.1F;
            poseStack.scale(scale, scale, scale);
        }
    }
}
