package me.kall.myhead;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mod(Main.MOD_ID)
public final class Main {
    public static final String MOD_ID = "myhead";
    public static final String MOD_NAME = "BanGDreamItsMyHead";

    public Main(@NotNull FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, Config.INSTANCE);
        MinecraftForge.EVENT_BUS.addListener(this::onLivingDamage);
    }

    public void onLivingDamage(@NotNull LivingDamageEvent event) {
        if (event.isCanceled()) return;
        LivingEntity attacked = event.getEntity();
        Entity directEntity = event.getSource().getDirectEntity();
        if (!(directEntity instanceof Projectile projectile)) return;
        if (projectile.getOwner() == null || attacked.level.isClientSide()) return;
        Entity owner = projectile.getOwner();

        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(attacked.getType());
        ResourceLocation sourceId = ForgeRegistries.ENTITY_TYPES.getKey(projectile.getType());

        if (Config.getProjectileBlacklist().contains(sourceId) || Config.getEntityBlacklist().contains(entityId)) return;

        if (isHeadShot(attacked, projectile)) {
            event.setAmount(event.getAmount() * Config.DAMAGE_BONUS.get().floatValue());
            if (Config.PLAY_DING.get()) attacked.playSound(SoundEvents.ARROW_HIT_PLAYER, 1.0F, 1.0F);
            if (Config.ACTION_BAR_NOTIFY.get() && owner instanceof ServerPlayer) ((ServerPlayer) owner).displayClientMessage(Component.translatable(MOD_ID + ".headshot.notify"), true);
        }
    }

    private static boolean isHeadShot(@NotNull LivingEntity attacked, @NotNull Projectile projectile) {
        double hitY = projectile.getY();
        double eyeY = attacked.getEyeY();
        double maxY = attacked.getBoundingBox().maxY;
        double radius = maxY - eyeY;
        if (Config.STRICT_HEADSHOT.get()) return hitY >= eyeY;
        return hitY >= eyeY - radius;
    }

    private static final class Config {
        private static final ForgeConfigSpec INSTANCE;
        private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENTITIES, PROJECTILES;
        private static final ForgeConfigSpec.DoubleValue DAMAGE_BONUS;
        private static final ForgeConfigSpec.BooleanValue PLAY_DING, ACTION_BAR_NOTIFY, STRICT_HEADSHOT;

        public static Set<ResourceLocation> ENTITY_BLACKLIST = null, PROJECTILE_BLACKLIST = null;

        static {
            ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
            builder.push(Main.MOD_NAME);
            ENTITIES = builder.defineList("EntitiesThatWillNeverBeHeadshot", Lists.newArrayList(), Predicates.alwaysTrue());
            PROJECTILES = builder.defineList("ProjectilesThatWillNeverCauseHeadshot", Lists.newArrayList(), Predicates.alwaysTrue());
            DAMAGE_BONUS = builder.defineInRange("HeadshotDamageMultiplier", 2.00, 1.00, Double.MAX_VALUE);
            PLAY_DING = builder.define("PlayDingSoundOnHeadshot", true);
            ACTION_BAR_NOTIFY = builder.define("NotifyShooterOnActionBar", false);
            STRICT_HEADSHOT = builder.comment("If enabled, headshot won't take place unless the projectile hurts the position above the attacked's eyes.").define("StrictHeadshot", false);
            builder.pop();
            INSTANCE = builder.build();
        }

        public static Set<ResourceLocation> getEntityBlacklist() {
            if (ENTITY_BLACKLIST == null) ENTITY_BLACKLIST = ENTITIES.get().stream().map(ResourceLocation::parse).collect(Collectors.toSet());
            return ENTITY_BLACKLIST;
        }

        public static Set<ResourceLocation> getProjectileBlacklist() {
            if (PROJECTILE_BLACKLIST == null) PROJECTILE_BLACKLIST = PROJECTILES.get().stream().map(ResourceLocation::parse).collect(Collectors.toSet());
            return PROJECTILE_BLACKLIST;
        }
    }
}
