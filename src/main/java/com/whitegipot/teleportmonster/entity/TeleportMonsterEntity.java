package com.whitegipot.teleportmonster.entity;

import com.whitegipot.teleportmonster.entity.ai.TeleportAttackGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class TeleportMonsterEntity extends Monster {
    private static final EntityDataAccessor<Boolean> IS_TELEPORTING = 
            SynchedEntityData.defineId(TeleportMonsterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TELEPORT_COOLDOWN = 
            SynchedEntityData.defineId(TeleportMonsterEntity.class, EntityDataSerializers.INT);

    private int teleportCooldown = 0;

    public TeleportMonsterEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 10;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TeleportAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_TELEPORTING, false);
        this.entityData.define(TELEPORT_COOLDOWN, 0);
    }

    public boolean isTeleporting() {
        return this.entityData.get(IS_TELEPORTING);
    }

    public void setTeleporting(boolean teleporting) {
        this.entityData.set(IS_TELEPORTING, teleporting);
    }

    public int getTeleportCooldown() {
        return this.teleportCooldown;
    }

    public void setTeleportCooldown(int cooldown) {
        this.teleportCooldown = cooldown;
        this.entityData.set(TELEPORT_COOLDOWN, cooldown);
    }

    @Override
    public void tick() {
        super.tick();
        
        if (this.teleportCooldown > 0) {
            this.teleportCooldown--;
        }
        
        if (this.isTeleporting() && this.level().isClientSide) {
            for (int i = 0; i < 20; i++) {
                double d0 = this.random.nextGaussian() * 0.02D;
                double d1 = this.random.nextGaussian() * 0.02D;
                double d2 = this.random.nextGaussian() * 0.02D;
                this.level().addParticle(ParticleTypes.PORTAL, 
                        this.getRandomX(1.0D), 
                        this.getRandomY(), 
                        this.getRandomZ(1.0D), 
                        d0, d1, d2);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && source.getEntity() instanceof LivingEntity) {
            if (this.random.nextFloat() < 0.25F && this.teleportCooldown <= 0) {
                this.teleportRandomly();
            }
        }
        
        if (this.getHealth() < this.getMaxHealth() * 0.3F && this.teleportCooldown <= 0) {
            this.teleportRandomly();
        }
        
        return super.hurt(source, amount);
    }

    public boolean teleportRandomly() {
        if (this.level().isClientSide() || this.teleportCooldown > 0) {
            return false;
        }

        double d0 = this.getX() + (this.random.nextDouble() - 0.5D) * 16.0D;
        double d1 = this.getY() + (double)(this.random.nextInt(16) - 8);
        double d2 = this.getZ() + (this.random.nextDouble() - 0.5D) * 16.0D;
        return this.teleportTo(d0, d1, d2);
    }

    public boolean teleportToTarget(LivingEntity target) {
        if (this.level().isClientSide() || this.teleportCooldown > 0) {
            return false;
        }

        Vec3 targetPos = target.position();
        double distance = this.distanceTo(target);
        
        if (distance > 12.0D) {
            Vec3 direction = targetPos.subtract(this.position()).normalize();
            double newDistance = Math.max(3.0D, distance - 8.0D);
            Vec3 newPos = this.position().add(direction.scale(newDistance));
            return this.teleportTo(newPos.x, newPos.y, newPos.z);
        }
        
        return false;
    }

    public boolean teleportBehindTarget(LivingEntity target) {
        if (this.level().isClientSide() || this.teleportCooldown > 0) {
            return false;
        }

        Vec3 targetPos = target.position();
        Vec3 lookAngle = target.getLookAngle();
        Vec3 behindPos = targetPos.subtract(lookAngle.scale(2.0D));
        
        return this.teleportTo(behindPos.x, behindPos.y, behindPos.z);
    }

    private boolean teleportTo(double x, double y, double z) {
        BlockPos.MutableBlockPos blockpos = new BlockPos.MutableBlockPos(x, y, z);
        
        while(blockpos.getY() > this.level().getMinBuildHeight() && !this.level().getBlockState(blockpos).blocksMotion()) {
            blockpos.move(0, -1, 0);
        }
        
        if (!this.level().getBlockState(blockpos).blocksMotion()) {
            return false;
        }

        this.setTeleporting(true);
        
        if (this.level().isClientSide) {
            for (int i = 0; i < 32; i++) {
                this.level().addParticle(ParticleTypes.PORTAL, 
                        this.getX(), this.getY() + this.random.nextDouble() * 2.0D, this.getZ(),
                        this.random.nextGaussian(), 0.0D, this.random.nextGaussian());
            }
        }
        
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), 
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
        
        boolean success = this.randomTeleport(x, blockpos.getY() + 1, z, true);
        
        if (success) {
            this.level().playSound(null, x, blockpos.getY() + 1, z, 
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
            
            if (!this.level().isClientSide) {
                for (int i = 0; i < 32; i++) {
                    this.level().addParticle(ParticleTypes.PORTAL, 
                            x, blockpos.getY() + 1 + this.random.nextDouble() * 2.0D, z,
                            this.random.nextGaussian(), 0.0D, this.random.nextGaussian());
                }
            }
            
            this.setTeleportCooldown(100);
        }
        
        this.setTeleporting(false);
        return success;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TeleportCooldown", this.teleportCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.teleportCooldown = tag.getInt("TeleportCooldown");
    }
}
