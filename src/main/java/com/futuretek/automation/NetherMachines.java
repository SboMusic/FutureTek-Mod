package com.futuretek.automation;

import java.util.List;


import javax.annotation.Nullable;

import com.futuretek.registry.ModBlockEntities;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * Fase 13 - macchine "avanzate" che richiedono materiali del Nether:
 *  - Incantatore elettrico: incanta con meno livelli di esperienza, ma consuma tanta energia
 *  - Campionatore di creature: click destro su un mob per "registrarlo"
 *  - Incubatore di creature: con il campione e l'energia fa nascere quel mob (piu' energia = piu' spesso)
 *  - Torretta laser: colpisce i mob vicini (lasciano drop ed esperienza, come se li uccidessi tu)
 */
public final class NetherMachines {
    private NetherMachines() {
    }

    // =====================================================================
    // Incantatore elettrico
    // =====================================================================
    public static class Enchanter extends SlotMachines.Entity {
        /** potenza (come i livelli del tavolo normale), livelli di esperienza, lapislazzuli, energia */
        public static final int[][] TIERS = {{10, 2, 1, 25_000}, {20, 5, 2, 75_000}, {30, 9, 3, 200_000}};
        /** pulsanti: 0-2 incanto a caso; 3 precedente, 4 successivo, 5 livello -, 6 livello +, 7 incanta quello scelto */
        public static final int PREV = 3, NEXT = 4, LEVEL_DOWN = 5, LEVEL_UP = 6, APPLY = 7;

        public Enchanter(BlockPos pos, BlockState state) {
            super(ModBlockEntities.ELECTRIC_ENCHANTER.get(), pos, state, 250_000, 1024, 3);
        }

        @Override
        protected boolean accepts(int slot, ItemStack stack) {
            return switch (slot) {
                case 1 -> stack.is(Tags.Items.GEMS_LAPIS);
                case 2 -> stack.getItem() instanceof Enchanting.Archive;
                default -> stack.isEnchantable() || stack.is(Items.BOOK) || stack.isEnchanted() || stack.is(Items.ENCHANTED_BOOK);
            };
        }

        @Override
        public int buttonCount() {
            return 8;
        }

        java.util.List<java.util.Map.Entry<net.minecraft.core.Holder.Reference<net.minecraft.world.item.enchantment.Enchantment>, Integer>> choices() {
            return Enchanting.usable(level.registryAccess(), items.getStackInSlot(2), items.getStackInSlot(0));
        }

        @Override
        public void serverTick() {
            v1 = energy.getEnergyStored() / 1000;
            ItemStack item = items.getStackInSlot(0);
            var list = choices();
            if (list.isEmpty()) {
                v2 = 0;
                v3 = 0;
            } else {
                v2 = Math.floorMod(v2, list.size());
                int max = list.get(v2).getValue();
                v3 = v3 <= 0 ? max : Math.max(1, Math.min(v3, max)); // di base il livello piu' alto conosciuto
            }
            if (status != 3 || item.isEmpty()) {
                status = item.isEmpty() ? 0 : (item.isEnchantable() || item.is(Items.BOOK) || !list.isEmpty()) ? 1 : 2;
            }
        }

        @Override
        public void onButton(Player player, int id) {
            if (id >= PREV) {
                var list = choices();
                if (list.isEmpty()) {
                    return;
                }
                switch (id) {
                    case PREV -> { v2 = Math.floorMod(v2 - 1, list.size()); v3 = list.get(v2).getValue(); }
                    case NEXT -> { v2 = Math.floorMod(v2 + 1, list.size()); v3 = list.get(v2).getValue(); }
                    case LEVEL_DOWN -> v3 = Math.max(1, v3 - 1);
                    case LEVEL_UP -> v3 = Math.min(list.get(Math.floorMod(v2, list.size())).getValue(), v3 + 1);
                    default -> applyChosen(player, list.get(Math.floorMod(v2, list.size())).getKey());
                }
                return;
            }
            int[] t = TIERS[id];
            ItemStack item = items.getStackInSlot(0);
            ItemStack lapis = items.getStackInSlot(1);
            if (item.isEmpty() || !item.isEnchantable()) {
                return;
            }
            boolean creative = player.getAbilities().instabuild;
            if (!creative && (player.experienceLevel < t[1] || lapis.getCount() < t[2] || energy.getEnergyStored() < t[3])) {
                player.displayClientMessage(Component.translatable("message.futuretek.enchanter_missing", t[1], t[2],
                        t[3] / 1000).withStyle(ChatFormatting.RED), true);
                return;
            }
            var table = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                    .getTag(EnchantmentTags.IN_ENCHANTING_TABLE);
            ItemStack result = EnchantmentHelper.enchantItem(level.random, item.copy(), t[0], level.registryAccess(), table);
            if (!creative) {
                player.onEnchantmentPerformed(result, t[1]);
                lapis.shrink(t[2]);
                energy.consume(t[3]);
            }
            items.setStackInSlot(0, result);
            done();
        }

        /** Incanta con l'incantesimo e il livello scelti dall'archivio */
        void applyChosen(Player player, net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> ench) {
            int[] c = Enchanting.cost(v3);
            ItemStack item = items.getStackInSlot(0);
            ItemStack lapis = items.getStackInSlot(1);
            boolean creative = player.getAbilities().instabuild;
            if (!creative && (player.experienceLevel < c[0] || lapis.getCount() < c[1] || energy.getEnergyStored() < c[2])) {
                player.displayClientMessage(Component.translatable("message.futuretek.enchanter_missing", c[0], c[1],
                        c[2] / 1000).withStyle(ChatFormatting.RED), true);
                return;
            }
            ItemStack result = Enchanting.apply(item, ench, v3);
            if (result == null) {
                player.displayClientMessage(Component.translatable("message.futuretek.enchanter_incompatible")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }
            if (!creative) {
                player.giveExperienceLevels(-c[0]);
                lapis.shrink(c[1]);
                energy.consume(c[2]);
            }
            items.setStackInSlot(0, result);
            done();
        }

        private void done() {
            status = 3;
            setChanged();
            level.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.1F);
            ((ServerLevel) level).sendParticles(ParticleTypes.ENCHANT, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2,
                    worldPosition.getZ() + 0.5, 30, 0.4, 0.3, 0.4, 0.5);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            // prima l'incantatore aveva 2 caselle: ora 3 (la terza e' per l'archivio)
            if (tag.contains("Items") && tag.getCompound("Items").getInt("Size") < 3) {
                tag.getCompound("Items").putInt("Size", 3);
            }
            super.loadAdditional(tag, registries);
        }
    }

    // =====================================================================
    // Campionatore di creature
    // =====================================================================
    public static class MobSampler extends Item {
        public MobSampler(Properties props) {
            super(props.stacksTo(1));
        }

        @Nullable
        public static EntityType<?> sampled(ItemStack stack) {
            CustomData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if (data == null || !data.contains("Mob")) {
                return null;
            }
            ResourceLocation id = ResourceLocation.tryParse(data.copyTag().getString("Mob"));
            return id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        }

        public static void store(ItemStack stack, EntityType<?> type) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Mob", BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }

        public static boolean canSample(LivingEntity target) {
            return target instanceof Mob && !(target instanceof Player) && !target.getType().is(Tags.EntityTypes.BOSSES);
        }

        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
            if (!canSample(target)) {
                if (!player.level().isClientSide) {
                    player.displayClientMessage(Component.translatable("message.futuretek.sampler_refused"), true);
                }
                return InteractionResult.FAIL;
            }
            if (!player.level().isClientSide) {
                ItemStack held = player.getItemInHand(hand);
                store(held, target.getType());
                player.displayClientMessage(Component.translatable("message.futuretek.sampler_done",
                        target.getType().getDescription()), true);
                player.level().playSound(null, target.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1, 1.4F);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return sampled(stack) != null;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            EntityType<?> type = sampled(stack);
            tooltip.add(type == null ? Component.translatable("tooltip.futuretek.mob_sampler").withStyle(ChatFormatting.GRAY)
                    : Component.translatable("tooltip.futuretek.mob_sampler.has", type.getDescription())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    // =====================================================================
    // Incubatore di creature
    // =====================================================================
    public static class Incubator extends SlotMachines.Entity {
        public static final int COST = 6000;
        public static final int MIN_DELAY = 40;
        public static final int MAX_NEARBY = 6;
        private int cooldown;

        public Incubator(BlockPos pos, BlockState state) {
            super(ModBlockEntities.MOB_INCUBATOR.get(), pos, state, 60_000, 1024, 1);
        }

        @Override
        protected boolean accepts(int slot, ItemStack stack) {
            return stack.getItem() instanceof MobSampler && MobSampler.sampled(stack) != null;
        }

        @Override
        public void serverTick() {
            v1 = energy.getEnergyStored() * 100 / energy.getMaxEnergyStored();
            if (cooldown > 0) {
                cooldown--;
            }
            EntityType<?> type = MobSampler.sampled(items.getStackInSlot(0));
            if (type == null) {
                status = 0;
                setLit(false);
                return;
            }
            if (energy.getEnergyStored() < COST) {
                status = 2;
                setLit(false);
                return;
            }
            if (cooldown > 0) {
                return;
            }
            AABB area = new AABB(worldPosition).inflate(4, 2, 4).move(0, 1, 0);
            int nearby = level.getEntities(type, area, e -> e.isAlive()).size();
            if (nearby >= MAX_NEARBY) {
                status = 3;
                setLit(false);
                cooldown = 20;
                return;
            }
            ServerLevel server = (ServerLevel) level;
            for (int tries = 0; tries < 8; tries++) {
                BlockPos p = worldPosition.offset(level.random.nextInt(5) - 2, 1, level.random.nextInt(5) - 2);
                if (!level.noCollision(type.getSpawnAABB(p.getX() + 0.5, p.getY(), p.getZ() + 0.5))) {
                    continue;
                }
                Entity spawned = type.spawn(server, p, MobSpawnType.SPAWNER);
                if (spawned != null) {
                    server.sendParticles(ParticleTypes.SOUL, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 12, 0.3, 0.5, 0.3, 0.02);
                    energy.consume(COST);
                    cooldown = MIN_DELAY;
                    status = 1;
                    setLit(true);
                    return;
                }
            }
            status = 4; // niente spazio libero sopra
            cooldown = 20;
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putInt("Cooldown", cooldown);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            cooldown = tag.getInt("Cooldown");
        }
    }

    // =====================================================================
    // Torretta laser
    // =====================================================================
    public static class LaserTurret extends SlotMachines.Entity {
        public static final int COST = 150;
        public static final float DAMAGE = 8.0F;
        public static final int MAX_TARGETS = 4;
        private boolean onlyMonsters = true;

        public LaserTurret(BlockPos pos, BlockState state) {
            super(ModBlockEntities.LASER_TURRET.get(), pos, state, 40_000, 1024, 0);
        }

        @Override
        public int buttonCount() {
            return 1;
        }

        @Override
        public void onButton(Player player, int id) {
            onlyMonsters = !onlyMonsters;
            setChanged();
        }

        public AABB area() {
            return new AABB(worldPosition).inflate(3, 0, 3).expandTowards(0, 3, 0).move(0, -1, 0);
        }

        public boolean isTarget(LivingEntity e) {
            if (!e.isAlive() || e instanceof Player || e instanceof ArmorStand || e instanceof Npc || e.hasCustomName()
                    || (e instanceof OwnableEntity own && own.getOwnerUUID() != null)
                    || e.getType().is(Tags.EntityTypes.BOSSES) || e instanceof com.futuretek.robot.RobotEntity) {
                return false;
            }
            return !onlyMonsters || e instanceof Enemy;
        }

        @Override
        public void serverTick() {
            int mode = onlyMonsters ? 0 : 10; // lo stato dice anche cosa colpisce: 0-2 solo mostri, 10-12 tutti
            // raccoglie gli oggetti caduti nella sua area e li mette nel baule (o tubo) attaccato
            if (level.getGameTime() % 20 == 5) {
                for (net.minecraft.world.entity.item.ItemEntity drop : level.getEntitiesOfClass(
                        net.minecraft.world.entity.item.ItemEntity.class, area().inflate(1))) {
                    ItemStack rest = com.futuretek.logistics.ItemOutput.insert(level, worldPosition, drop.getItem().copy(), false);
                    if (rest.isEmpty()) {
                        drop.discard();
                    } else {
                        drop.setItem(rest);
                    }
                }
            }
            if (level.getGameTime() % 10 != 0) {
                status = status % 10 + mode;
                return;
            }
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area(), this::isTarget);
            if (targets.isEmpty()) {
                status = mode;
                v1 = 0;
                setLit(false);
                return;
            }
            ServerLevel server = (ServerLevel) level;
            FakePlayer shooter = FakePlayerFactory.getMinecraft(server);
            shooter.setPos(worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5);
            int hits = 0;
            Vec3 from = Vec3.atCenterOf(worldPosition).add(0, 0.6, 0);
            for (LivingEntity target : targets) {
                if (hits >= MAX_TARGETS || energy.getEnergyStored() < COST) {
                    break;
                }
                target.invulnerableTime = 0;
                if (target.hurt(server.damageSources().playerAttack(shooter), DAMAGE)) {
                    energy.consume(COST);
                    hits++;
                    Vec3 to = target.getBoundingBox().getCenter();
                    Vec3 step = to.subtract(from).normalize().scale(0.4);
                    double len = from.distanceTo(to);
                    Vec3 p = from;
                    for (double d = 0; d < len; d += 0.4) {
                        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                        p = p.add(step);
                    }
                }
            }
            if (hits > 0) {
                level.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.3F, 2.0F);
            }
            v1 = hits;
            status = (hits > 0 ? 1 : 2) + mode;
            setLit(hits > 0);
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putBoolean("OnlyMonsters", onlyMonsters);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            onlyMonsters = !tag.contains("OnlyMonsters") || tag.getBoolean("OnlyMonsters");
        }
    }

    /** Per i test: un modo comodo di creare un campionatore gia' pieno */
    public static ItemStack sampleOf(Item sampler, EntityType<?> type) {
        ItemStack stack = new ItemStack(sampler);
        MobSampler.store(stack, type);
        return stack;
    }

}
