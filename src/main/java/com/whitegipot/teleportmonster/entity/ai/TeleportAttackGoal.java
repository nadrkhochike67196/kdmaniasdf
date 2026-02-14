package com.whitegipot.teleportmonster.entity.ai;

import com.whitegipot.teleportmonster.entity.TeleportMonsterEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;

public class TeleportAttackGoal extends Goal {
    private final TeleportMonsterEntity entity;
    private final double speedModifier;
    private int attackTime = -1;
    private int seeTime;

    public TeleportAttackGoal(TeleportMonsterEntity entity, double speedModifier, boolean mustSee) {
        this.entity = entity;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.entity.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.entity.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        return !this.entity.getNavigation().isDone();
    }

    @Override
    public void start() {
        this.entity.setAggressive(true);
        this.seeTime = 0;
    }

    @Override
    public void stop() {
        this.entity.setAggressive(false);
        this.attackTime = -1;
    }

    @Override
    public void tick() {
        LivingEntity target = this.entity.getTarget();
        if (target == null) {
            return;
        }

        double distance = this.entity.distanceToSqr(target.getX(), target.getY(), target.getZ());
        boolean canSee = this.entity.getSensing().hasLineOfSight(target);

        if (canSee) {
            ++this.seeTime;
        } else {
            this.seeTime = 0;
        }

        if (this.entity.getTeleportCooldown() <= 0) {
            if (distance > 144.0D && canSee) {
                this.entity.teleportToTarget(target);
            } else if (distance < 64.0D && distance > 9.0D && this.entity.getRandom().nextFloat() < 0.1F) {
                this.entity.teleportBehindTarget(target);
            }
        }

        if (distance <= 4.0D && this.attackTime <= 0) {
            this.attackTime = 20;
            this.entity.doHurtTarget(target);
        }

        this.attackTime = Math.max(this.attackTime - 1, 0);
        
        this.entity.getLookControl().setLookAt(target, 30.0F, 30.0F);
        this.entity.getNavigation().moveTo(target, this.speedModifier);
    }
}
