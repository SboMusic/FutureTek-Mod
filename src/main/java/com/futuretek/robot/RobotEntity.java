package com.futuretek.robot;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.futuretek.registry.ModBlocks;
import com.futuretek.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Robot di FutureTek: lavora DA SOLO, come un giocatore.
 * Ha una BASE DI RICARICA e un BAULE (quello accanto alla base, oppure uno scelto col Pannello di controllo).
 * Lavori (click destro a mano vuota per cambiarlo):
 *  0 CONTADINO   - con la ZAPPA: raccoglie le colture mature vicino alla base e ripianta
 *  1 GUARDIA     - ti segue e attacca i mostri (con una spada in mano fa piu' danno)
 *  2 SENTINELLA  - difende la zona della base
 *  3 MINATORE    - col PICCONE: scava una scala fino in fondo al mondo e poi gallerie a serpentina,
 *                  prende i minerali che vede, tappa acqua e lava, mette le torce se ne ha
 *  4 TAGLIALEGNA - con l'ASCIA: abbatte gli alberi vicino alla base e ripianta gli alberelli
 * Regole comuni: rompe i blocchi col tempo giusto per l'attrezzo che ha (e lo consuma);
 * quando l'attrezzo si rompe va al baule a prenderne un altro (se non c'e' aspetta alla base);
 * quando e' pieno porta tutto al baule; se il baule e' pieno si ferma alla base finche' non gli dici "continua";
 * quando e' scarico torna alla base a ricaricarsi.
 */
public class RobotEntity extends PathfinderMob {
    public static final int CAPACITY = 200_000;
    public static final int IDLE_COST = 4;
    public static final int BLOCK_COST = 40;
    public static final int WORK_RADIUS = 12;
    public static final int MODES = 5;
    public static final int FARMER = 0, GUARD = 1, SENTRY = 2, MINER = 3, LUMBERJACK = 4;

    /** Stati (si vedono col click destro e col pannello) */
    public static final int WORKING = 0, RECHARGING = 1, NO_TOOL = 2, CHEST_FULL = 3, NO_CHEST = 4, DONE = 5,
            FETCHING = 6, DEPOSITING = 7, IDLE = 8;

    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ENERGY = SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> INFINITE = SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.INT);

    @Nullable private BlockPos dock;
    @Nullable private BlockPos chest;
    @Nullable private UUID owner;
    private final SimpleContainer inventory = new SimpleContainer(27);
    private boolean charging;

    // --- miniera: il percorso gia' scavato (scala + gallerie), per tornare indietro e ripartire ---
    private final List<BlockPos> path = new ArrayList<>();
    private Direction mineDir = Direction.NORTH;
    private boolean stairsDone;
    private int segment;      // gallerie a serpentina: segmento corrente
    private int segmentLength;
    public static final int LONG_SEGMENT = 32, SHORT_SEGMENT = 4, MAX_SEGMENTS = 31;

    public RobotEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setDropChance(EquipmentSlot.MAINHAND, 2.0F); // l'attrezzo cade sempre se il robot viene distrutto
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 5).add(Attributes.ARMOR, 8).add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MODE, FARMER).define(ENERGY, 0).define(INFINITE, false).define(STATE, WORKING);
    }

    // =====================================================================
    // dati
    // =====================================================================
    public int getMode() {
        return entityData.get(MODE);
    }

    public void setMode(int mode) {
        entityData.set(MODE, Math.floorMod(mode, MODES));
        setTarget(null);
        getNavigation().stop();
        setState(WORKING);
        resetWork();
    }

    public int getState() {
        return entityData.get(STATE);
    }

    void setState(int state) {
        entityData.set(STATE, state);
    }

    public int getEnergy() {
        return entityData.get(ENERGY);
    }

    public void setEnergy(int energy) {
        entityData.set(ENERGY, Math.max(0, Math.min(CAPACITY, energy)));
    }

    public boolean isInfinite() {
        return entityData.get(INFINITE);
    }

    public void setInfinite(boolean infinite) {
        entityData.set(INFINITE, infinite);
    }

    @Nullable
    public BlockPos getDock() {
        return dock;
    }

    public void setDock(@Nullable BlockPos dock) {
        this.dock = dock == null ? null : dock.immutable();
    }

    @Nullable
    public BlockPos getChest() {
        return chest;
    }

    public void setChest(@Nullable BlockPos chest) {
        this.chest = chest == null ? null : chest.immutable();
        if (getState() == NO_CHEST || getState() == CHEST_FULL) {
            setState(WORKING);
        }
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    public List<BlockPos> getMinePath() {
        return path;
    }

    public boolean hasPower() {
        return isInfinite() || getEnergy() > 0;
    }

    /** Usa energia per un'azione; false se non basta */
    public boolean use(int amount) {
        if (isInfinite()) {
            return true;
        }
        if (getEnergy() < amount) {
            return false;
        }
        setEnergy(getEnergy() - amount);
        return true;
    }

    boolean needsRecharge() {
        return dock != null && !isInfinite() && getEnergy() < CAPACITY / 4;
    }

    @Nullable
    Player ownerPlayer() {
        return owner == null ? null : level().getPlayerByUUID(owner);
    }

    /** "Continua": dopo baule pieno, fine lavoro o attesa dell'attrezzo */
    public void resume() {
        if (getState() == DONE && getMode() == MINER) {
            segment = 0; // ricomincia le gallerie da dove e' arrivato, girando
            segmentLength = 0;
            mineDir = mineDir.getClockWise();
        }
        setState(WORKING);
    }

    /** Che attrezzo serve per il lavoro attuale (null = nessuno) */
    @Nullable
    public TagKey<Item> toolTag() {
        return switch (getMode()) {
            case FARMER -> ItemTags.HOES;
            case MINER -> ItemTags.PICKAXES;
            case LUMBERJACK -> ItemTags.AXES;
            default -> null;
        };
    }

    public boolean isWorker() {
        return toolTag() != null;
    }

    boolean hasTool() {
        TagKey<Item> tag = toolTag();
        return tag == null || getMainHandItem().is(tag);
    }

    // =====================================================================
    // obiettivi
    // =====================================================================
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25, true) {
            @Override
            public boolean canUse() {
                return hasPower() && super.canUse();
            }
        });
        goalSelector.addGoal(2, new BrainGoal());
        goalSelector.addGoal(3, new FollowOwnerGoal());
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.6) {
            @Override
            public boolean canUse() {
                return hasPower() && getMode() == SENTRY && super.canUse();
            }
        });
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return hasPower() && super.canUse() && !(getLastHurtByMob() instanceof Player);
            }
        });
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                e -> e instanceof Enemy) {
            @Override
            public boolean canUse() {
                return hasPower() && (getMode() == GUARD || getMode() == SENTRY) && super.canUse();
            }
        });
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            return;
        }
        // ricarica alla base
        charging = false;
        if (dock != null && distanceToSqr(Vec3.atBottomCenterOf(dock.above())) < 2.5 && getEnergy() < CAPACITY && !isInfinite()) {
            IEnergyStorage source = level().getCapability(Capabilities.EnergyStorage.BLOCK, dock, null);
            if (source != null) {
                int got = source.extractEnergy(Math.min(1024, CAPACITY - getEnergy()), false);
                if (got > 0) {
                    setEnergy(getEnergy() + got);
                    charging = true;
                    if (tickCount % 20 == 0) {
                        ((ServerLevel) level()).sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1, getZ(), 4, 0.3, 0.4, 0.3, 0);
                    }
                }
            }
        }
        if (!charging && !isInfinite()) {
            setEnergy(getEnergy() - IDLE_COST);
        }
        if (!hasPower()) {
            getNavigation().stop();
        }
        // raccoglie gli oggetti per terra vicino a se' (come un giocatore)
        if (isWorker() && hasPower() && tickCount % 5 == 0) {
            for (ItemEntity item : level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(2.5, 1.5, 2.5))) {
                if (item.isAlive() && !item.hasPickUpDelay()) {
                    ItemStack rest = inventory.addItem(item.getItem().copy());
                    if (rest.isEmpty()) {
                        item.discard();
                    } else {
                        item.setItem(rest);
                    }
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = use(100) && super.doHurtTarget(target);
        if (hit && getMainHandItem().isDamageableItem()) {
            getMainHandItem().hurtAndBreak(1, this, EquipmentSlot.MAINHAND);
        }
        return hit;
    }

    // =====================================================================
    // click del giocatore
    // =====================================================================
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level().isClientSide) {
            return held.is(ModItems.ROBOT_CONTROLLER.get()) ? InteractionResult.PASS : InteractionResult.SUCCESS;
        }
        if (held.is(ModItems.ROBOT_CONTROLLER.get())) {
            return InteractionResult.PASS; // ci pensa il pannello
        }
        if (held.is(ModItems.INFINITE_CORE.get())) {
            if (!isInfinite()) {
                setInfinite(true);
                held.consume(1, player);
                player.displayClientMessage(Component.translatable("message.futuretek.robot_infinite"), true);
                playSound(SoundEvents.BEACON_ACTIVATE, 1, 1.5F);
            }
            return InteractionResult.CONSUME;
        }
        // dargli un attrezzo (piccone, ascia, zappa, spada): lui ti ridà quello che aveva
        if (held.is(ItemTags.PICKAXES) || held.is(ItemTags.AXES) || held.is(ItemTags.HOES) || held.is(ItemTags.SWORDS)) {
            ItemStack old = getMainHandItem();
            setItemSlot(EquipmentSlot.MAINHAND, held.copy());
            player.setItemInHand(hand, old);
            if (getState() == NO_TOOL) {
                setState(WORKING);
            }
            playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.8F, 1.2F);
            return InteractionResult.CONSUME;
        }
        if (!held.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            ItemStack item = RobotItem.fromRobot(this);
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                spawnAtLocation(inventory.removeItemNoUpdate(i));
            }
            if (!getMainHandItem().isEmpty()) {
                spawnAtLocation(getMainHandItem());
            }
            if (!player.getInventory().add(item)) {
                spawnAtLocation(item);
            }
            discard();
            return InteractionResult.CONSUME;
        }
        if (owner == null) {
            owner = player.getUUID();
        }
        int s = getState();
        if (s == CHEST_FULL || s == DONE || s == NO_TOOL || s == NO_CHEST) {
            resume(); // e' fermo: il click lo fa ripartire
            player.displayClientMessage(Component.translatable("message.futuretek.robot_resume"), true);
        } else {
            setMode(getMode() + 1);
            player.displayClientMessage(describe(), true);
        }
        playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.6F);
        return InteractionResult.CONSUME;
    }

    /** "Robot: Minatore · Lavoro · energia 80% · Piccone di ferro" */
    public Component describe() {
        ItemStack tool = getMainHandItem();
        Component toolText = tool.isEmpty() ? Component.translatable("message.futuretek.robot_no_tool_held")
                : tool.isDamageableItem() ? Component.translatable("message.futuretek.robot_tool", tool.getHoverName(),
                        tool.getMaxDamage() - tool.getDamageValue()) : tool.getHoverName();
        return Component.translatable("message.futuretek.robot_status",
                Component.translatable("gui.futuretek.robot_mode." + getMode()),
                Component.translatable("gui.futuretek.robot_state." + getState()),
                isInfinite() ? "∞" : String.valueOf(getEnergy() * 100 / CAPACITY), toolText);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recent) {
        super.dropCustomDeathLoot(level, source, recent);
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            spawnAtLocation(inventory.removeItemNoUpdate(i));
        }
        spawnAtLocation(new ItemStack(ModItems.ROBOT_CHASSIS.get()));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // =====================================================================
    // salvataggio
    // =====================================================================
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Mode", getMode());
        tag.putInt("State", getState());
        tag.putInt("RobotEnergy", getEnergy());
        tag.putBoolean("Infinite", isInfinite());
        if (dock != null) {
            tag.put("Dock", NbtUtils.writeBlockPos(dock));
        }
        if (chest != null) {
            tag.put("Chest", NbtUtils.writeBlockPos(chest));
        }
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.put("RobotItems", inventory.createTag(registryAccess()));
        tag.putLongArray("MinePath", path.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putString("MineDir", mineDir.getSerializedName());
        tag.putBoolean("StairsDone", stairsDone);
        tag.putInt("Segment", segment);
        tag.putInt("SegmentLength", segmentLength);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(MODE, Math.floorMod(tag.getInt("Mode"), MODES));
        setState(tag.getInt("State"));
        setEnergy(tag.getInt("RobotEnergy"));
        setInfinite(tag.getBoolean("Infinite"));
        dock = NbtUtils.readBlockPos(tag, "Dock").orElse(null);
        chest = NbtUtils.readBlockPos(tag, "Chest").orElse(null);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        inventory.fromTag(tag.getList("RobotItems", 10), registryAccess());
        path.clear();
        for (long l : tag.getLongArray("MinePath")) {
            path.add(BlockPos.of(l));
        }
        Direction d = Direction.byName(tag.getString("MineDir"));
        mineDir = d == null || d.getAxis().isVertical() ? Direction.NORTH : d;
        stairsDone = tag.getBoolean("StairsDone");
        segment = tag.getInt("Segment");
        segmentLength = tag.getInt("SegmentLength");
    }

    // =====================================================================
    // il "cervello" dei lavoratori
    // =====================================================================
    private void resetWork() {
        breaking = null;
        workTarget = null;
        chop.clear();
        ores.clear();
        if (getMode() != MINER) {
            path.clear();
            stairsDone = false;
            segment = 0;
            segmentLength = 0;
        }
    }

    // rottura dei blocchi "a tempo", come un giocatore
    @Nullable private BlockPos breaking;
    private int breakTicks;
    private int breakNeeded;
    @Nullable private BlockPos workTarget;
    private final ArrayDeque<BlockPos> chop = new ArrayDeque<>();
    private final ArrayDeque<BlockPos> ores = new ArrayDeque<>();
    private final Set<BlockPos> skip = new HashSet<>();
    private int waitTicks;
    // per non restare bloccati
    @Nullable private BlockPos travelTarget;
    private double travelBest;
    private int travelStuck;

    class BrainGoal extends Goal {
        BrainGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return hasPower() && (isWorker() || needsRecharge() || getState() == RECHARGING);
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            think();
        }

        @Override
        public void stop() {
            cancelBreak();
        }
    }

    void think() {
        // 1) ricarica
        if (needsRecharge() || (getState() == RECHARGING && getEnergy() < CAPACITY * 0.95 && dock != null)) {
            setState(RECHARGING);
            goHome();
            return;
        }
        if (getState() == RECHARGING) {
            setState(WORKING);
        }
        if (!isWorker()) {
            return;
        }
        // 2) fermo in attesa (baule pieno, lavoro finito, niente baule): resta alla base
        int s = getState();
        if (s == CHEST_FULL || s == DONE || s == NO_CHEST) {
            if (s == NO_CHEST && findChest() != null) {
                setState(WORKING);
            }
            goHome();
            return;
        }
        // 3) pieno: porta tutto al baule
        if (isFull() || s == DEPOSITING) {
            setState(DEPOSITING);
            deposit();
            return;
        }
        // 4) attrezzo (senza attrezzo nel baule riprova ogni 2 secondi)
        if (!hasTool()) {
            if (s == NO_TOOL && tickCount % 40 != 0) {
                goHome();
                return;
            }
            fetchTool();
            return;
        }
        // il minatore ha finito tutte le gallerie: porta a casa il bottino e aspetta
        if (getMode() == MINER && stairsDone && segment >= MAX_SEGMENTS) {
            setState(hasLoot() ? DEPOSITING : DONE);
            return;
        }
        if (s != WORKING && s != IDLE) {
            setState(WORKING);
        }
        if (s == IDLE && tickCount % 20 != 0) {
            goHome();
            return;
        }
        // 5) lavoro
        switch (getMode()) {
            case MINER -> mine();
            case LUMBERJACK -> lumber();
            case FARMER -> farm();
            default -> {
            }
        }
    }

    // --- movimento ---
    /** Va verso un punto; true quando ci e' arrivato. Se resta bloccato a lungo si teletrasporta vicino. */
    boolean travel(BlockPos target, double reach) {
        Vec3 c = Vec3.atBottomCenterOf(target);
        double d = position().distanceTo(c);
        if (d <= reach) {
            getNavigation().stop();
            travelTarget = null;
            return true;
        }
        if (!target.equals(travelTarget)) {
            travelTarget = target;
            travelBest = d;
            travelStuck = 0;
        }
        if (d < travelBest - 0.3) {
            travelBest = d;
            travelStuck = 0;
        } else if (++travelStuck > 100) {
            BlockPos safe = safeSpotNear(target);
            if (safe != null) {
                teleportTo(safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5);
            }
            travelStuck = 0;
            travelBest = position().distanceTo(c);
        }
        if (getNavigation().isDone() || tickCount % 40 == 0) {
            getNavigation().moveTo(c.x, c.y, c.z, 1.15);
        }
        return false;
    }

    @Nullable
    BlockPos safeSpotNear(BlockPos target) {
        for (BlockPos p : BlockPos.withinManhattan(target, 2, 2, 2)) {
            if (level().getBlockState(p).getCollisionShape(level(), p).isEmpty()
                    && level().getBlockState(p.above()).getCollisionShape(level(), p.above()).isEmpty()
                    && !level().getBlockState(p.below()).getCollisionShape(level(), p.below()).isEmpty()) {
                return p.immutable();
            }
        }
        return null;
    }

    /** Torna alla base (il minatore risale prima la sua scala) */
    void goHome() {
        if (followPathBack()) {
            return;
        }
        BlockPos home = dock != null ? dock.above() : null;
        if (home != null) {
            travel(home, 0.7);
        } else {
            getNavigation().stop();
        }
    }

    /** Se e' dentro la miniera, fa un passo indietro lungo il percorso. true = sta ancora risalendo */
    boolean followPathBack() {
        if (path.isEmpty()) {
            return false;
        }
        int at = nearestPathIndex();
        if (at <= 0) {
            return false;
        }
        travel(path.get(at - 1), 0.6);
        return true;
    }

    int nearestPathIndexWithin(double maxDistSqr) {
        int best = -1;
        double bestD = maxDistSqr;
        for (int i = path.size() - 1; i >= 0; i--) {
            double d = path.get(i).distToCenterSqr(getX(), getY() + 0.5, getZ());
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    /**
     * Controlla il pavimento della scala vicino al robot. Se manca un blocco (grotta, minerale scavato sotto,
     * sabbia caduta...) ci mette uno dei blocchi che ha. Se il robot e' caduto proprio nel buco, prima risale
     * sul gradino e poi lo tappa. true = ha fatto qualcosa in questo tick.
     */
    boolean repairStairs() {
        if (path.isEmpty()) {
            return false;
        }
        int at = nearestPathIndexWithin(36);
        if (at < 0) {
            return false;
        }
        for (int i = Math.max(0, at - 2); i <= Math.min(path.size() - 1, at + 2); i++) {
            BlockPos step = path.get(i);
            BlockPos floor = step.below();
            if (!level().getBlockState(floor).getCollisionShape(level(), floor).isEmpty()) {
                continue;
            }
            if (blockPosition().equals(floor) || blockPosition().equals(floor.below())) {
                // e' caduto nel buco: torna sul gradino
                teleportTo(step.getX() + 0.5, step.getY() + 0.05, step.getZ() + 0.5);
                getNavigation().stop();
            }
            placeFiller(floor);
            return true;
        }
        return false;
    }

    /** Non scava mai i minerali che fanno da pavimento alla scala o alla galleria */
    boolean isFloorOfPath(BlockPos pos) {
        for (int i = Math.max(0, path.size() - 12); i < path.size(); i++) {
            if (path.get(i).below().equals(pos)) {
                return true;
            }
        }
        return nextMineStep().below().equals(pos);
    }

    int nearestPathIndex() {
        int best = -1;
        double bestD = 2.25; // deve essere vicino (1,5 blocchi) a un punto del percorso
        for (int i = path.size() - 1; i >= 0; i--) {
            double d = path.get(i).distToCenterSqr(getX(), getY() + 0.5, getZ());
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    // --- baule ---
    @Nullable
    IItemHandler chestHandler() {
        BlockPos c = findChest();
        return c == null ? null : level().getCapability(Capabilities.ItemHandler.BLOCK, c, null);
    }

    @Nullable
    BlockPos findChest() {
        if (chest != null && level().getCapability(Capabilities.ItemHandler.BLOCK, chest, null) != null) {
            return chest;
        }
        if (dock != null) {
            for (Direction dir : Direction.values()) {
                BlockPos p = dock.relative(dir);
                if (!level().getBlockState(p).is(ModBlocks.ROBOT_DOCK.get())
                        && level().getCapability(Capabilities.ItemHandler.BLOCK, p, null) != null) {
                    chest = p.immutable();
                    return chest;
                }
            }
        }
        return null;
    }

    boolean isFull() {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Oggetti che tiene per se' (non li mette nel baule) */
    boolean keep(ItemStack stack, int[] kept) {
        if (getMode() == MINER && (stack.is(Items.TORCH) || stack.is(Items.COBBLESTONE) || stack.is(Items.COBBLED_DEEPSLATE))) {
            int idx = stack.is(Items.TORCH) ? 0 : 1;
            if (kept[idx] < 64) {
                kept[idx] += stack.getCount();
                return true;
            }
        }
        if (getMode() == LUMBERJACK && stack.is(ItemTags.SAPLINGS) && kept[2] < 16) {
            kept[2] += stack.getCount();
            return true;
        }
        if (getMode() == FARMER && isSeed(stack) && kept[3] < 32) {
            kept[3] += stack.getCount();
            return true;
        }
        return false;
    }

    /** Ha qualcosa da portare al baule (oltre a quello che tiene per lavorare)? */
    boolean hasLoot() {
        int[] kept = new int[4];
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack s = inventory.getItem(i);
            if (!s.isEmpty() && !keep(s, kept)) {
                return true;
            }
        }
        return false;
    }

    /** Nessun lavoro da fare: se ha del raccolto lo porta al baule, se no aspetta alla base */
    void idle() {
        if (hasLoot() && findChest() != null) {
            setState(DEPOSITING);
        } else {
            setState(IDLE);
            goHome();
        }
    }

    /** Arriva al baule (anche lontano), mette tutto dentro; baule pieno = si ferma */
    boolean goToChest() {
        BlockPos c = findChest();
        if (c == null) {
            setState(NO_CHEST);
            return false;
        }
        if (followPathBack()) {
            return false;
        }
        return travel(c, 2.2);
    }

    void deposit() {
        if (!goToChest()) {
            return;
        }
        IItemHandler h = chestHandler();
        if (h == null) {
            setState(NO_CHEST);
            return;
        }
        boolean full = false;
        int[] kept = new int[4];
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack s = inventory.getItem(i);
            if (s.isEmpty() || keep(s, kept)) {
                continue;
            }
            ItemStack rest = ItemHandlerHelper.insertItemStacked(h, s, false);
            inventory.setItem(i, rest);
            full |= !rest.isEmpty();
        }
        takeSupplies(h);
        setState(full ? CHEST_FULL : WORKING);
    }

    /** Al baule prende anche torce (minatore) e alberelli (taglialegna) se ce ne sono */
    void takeSupplies(IItemHandler h) {
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            boolean want = (getMode() == MINER && s.is(Items.TORCH) && inventory.countItem(Items.TORCH) < 32)
                    || (getMode() == LUMBERJACK && s.is(ItemTags.SAPLINGS) && countTag(ItemTags.SAPLINGS) < 8);
            if (want && inventory.canAddItem(s)) {
                ItemStack got = h.extractItem(i, 16, false);
                ItemStack rest = inventory.addItem(got);
                if (!rest.isEmpty()) {
                    ItemHandlerHelper.insertItemStacked(h, rest, false);
                }
            }
        }
    }

    int countTag(TagKey<Item> tag) {
        int n = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(tag)) {
                n += inventory.getItem(i).getCount();
            }
        }
        return n;
    }

    /** Senza attrezzo: va al baule a cercarne uno; se non c'e' aspetta alla base */
    void fetchTool() {
        TagKey<Item> tag = toolTag();
        // magari ne ha uno nello zaino
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(tag)) {
                ItemStack old = getMainHandItem();
                setItemSlot(EquipmentSlot.MAINHAND, inventory.removeItemNoUpdate(i));
                inventory.setItem(i, old);
                return;
            }
        }
        IItemHandler h = chestHandler();
        int slot = -1;
        if (h != null) {
            for (int i = 0; i < h.getSlots(); i++) {
                if (h.getStackInSlot(i).is(tag)) {
                    slot = i;
                    break;
                }
            }
        }
        if (slot < 0) {
            setState(NO_TOOL);
            goHome();
            return;
        }
        setState(FETCHING);
        if (!goToChest()) {
            return;
        }
        ItemStack tool = h.extractItem(slot, 1, false);
        if (!tool.isEmpty()) {
            if (!getMainHandItem().isEmpty()) {
                inventory.addItem(getMainHandItem());
            }
            setItemSlot(EquipmentSlot.MAINHAND, tool);
            playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.8F, 1.2F);
            deposit(); // gia' che c'e', svuota lo zaino
        }
    }

    // --- rompere un blocco col tempo giusto ---
    /** Continua a rompere il blocco; true quando e' rotto (o non c'e' piu' niente da rompere) */
    boolean breakBlock(BlockPos pos) {
        BlockState state = level().getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty() && state.getCollisionShape(level(), pos).isEmpty()) {
            cancelBreak();
            return true;
        }
        ItemStack tool = getMainHandItem();
        if (!pos.equals(breaking)) {
            cancelBreak();
            breaking = pos.immutable();
            breakTicks = 0;
            float hardness = state.getDestroySpeed(level(), pos);
            float speed = Math.max(1.0F, tool.getDestroySpeed(state));
            boolean right = !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state);
            breakNeeded = Math.max(1, (int) Math.ceil(hardness * (right ? 30 : 100) / speed));
        }
        getLookControl().setLookAt(Vec3.atCenterOf(pos));
        breakTicks++;
        if (breakTicks % 5 == 1) {
            swing(InteractionHand.MAIN_HAND);
        }
        level().destroyBlockProgress(getId(), pos, Math.min(9, breakTicks * 10 / breakNeeded));
        if (breakTicks < breakNeeded) {
            return false;
        }
        if (!use(BLOCK_COST)) {
            return false;
        }
        List<ItemStack> drops = Block.getDrops(state, (ServerLevel) level(), pos, level().getBlockEntity(pos), this, tool);
        level().destroyBlock(pos, false, this);
        for (ItemStack d : drops) {
            ItemStack rest = inventory.addItem(d);
            if (!rest.isEmpty()) {
                spawnAtLocation(rest);
            }
        }
        if (tool.isDamageableItem()) {
            tool.hurtAndBreak(1, this, EquipmentSlot.MAINHAND);
        }
        cancelBreak();
        return true;
    }

    void cancelBreak() {
        if (breaking != null) {
            level().destroyBlockProgress(getId(), breaking, -1);
        }
        breaking = null;
    }

    boolean canBreak(BlockPos pos, BlockState s) {
        return s.getDestroySpeed(level(), pos) >= 0 && level().getBlockEntity(pos) == null
                && !s.is(ModBlocks.ROBOT_DOCK.get());
    }

    // =====================================================================
    // MINATORE
    // =====================================================================
    /** Il prossimo punto della miniera (dove mettere i piedi) */
    BlockPos nextMineStep() {
        BlockPos last = path.isEmpty() ? null : path.get(path.size() - 1);
        if (last == null) {
            // primo gradino: due blocchi davanti alla base, dalla parte opposta al baule
            BlockPos base = dock != null ? dock : blockPosition().below();
            BlockPos c = findChest();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (c != null && base.relative(d).equals(c)) {
                    mineDir = d.getOpposite();
                }
            }
            return base.relative(mineDir, 2);
        }
        if (!stairsDone) {
            return last.relative(mineDir).below();
        }
        return last.relative(tunnelDir());
    }

    Direction tunnelDir() {
        if (segment % 2 == 1) {
            return mineDir.getClockWise();
        }
        return (segment / 2) % 2 == 0 ? mineDir : mineDir.getOpposite();
    }

    void mine() {
        // se e' caduto in un buco o manca un pezzo di pavimento della scala, lo sistema
        if (repairStairs()) {
            return;
        }
        // va al fronte della miniera
        int at = nearestPathIndex();
        if (!path.isEmpty() && at < path.size() - 1) {
            if (at < 0) {
                int near = nearestPathIndexWithin(36); // un po' fuori strada (caduto): riparte dal punto piu' vicino
                travel(path.get(near < 0 ? 0 : near), 0.6);
            } else {
                travel(path.get(at + 1), 0.6);
            }
            return;
        }
        // minerali visti lungo la strada
        while (!ores.isEmpty()) {
            BlockPos ore = ores.peek();
            BlockState s = level().getBlockState(ore);
            if (!s.is(Tags.Blocks.ORES) || ore.distToCenterSqr(getEyePosition()) > 30 || isFloorOfPath(ore)) {
                ores.poll();
                continue;
            }
            if (breakBlock(ore)) {
                ores.poll();
                scanOres(ore);
            }
            return;
        }
        BlockPos next = nextMineStep();
        if (path.isEmpty() && dock != null && !travel(dock.above(), 1.0)) {
            return; // il primo gradino lo scava stando sulla base
        }
        // fondo raggiunto? (bedrock sotto o vicino al fondo del mondo)
        if (!stairsDone && (next.getY() <= level().getMinBuildHeight() + 6
                || level().getBlockState(next.below()).getDestroySpeed(level(), next.below()) < 0)) {
            stairsDone = true;
            segment = 0;
            segmentLength = 0;
            return;
        }
        if (stairsDone && segmentLength >= (segment % 2 == 0 ? LONG_SEGMENT : SHORT_SEGMENT)) {
            segment++;
            segmentLength = 0;
            return;
        }
        // blocchi da togliere per questo passo (dall'alto in basso)
        int height = stairsDone ? 2 : 3;
        for (int h = height - 1; h >= 0; h--) {
            BlockPos p = next.above(h);
            BlockState s = level().getBlockState(p);
            if (!s.isAir() && !canBreak(p, s)) {
                blocked();
                return;
            }
            if (!s.getCollisionShape(level(), p).isEmpty() || (!s.isAir() && s.getFluidState().isEmpty())) {
                plugFluids(p, next, height);
                if (breakBlock(p)) {
                    scanOres(p);
                }
                return;
            }
            if (!s.getFluidState().isEmpty()) {
                if (!placeFiller(p)) {
                    blocked();
                }
                return;
            }
        }
        // pavimento: se sotto c'e' il vuoto o un liquido, ci mette un blocco
        BlockPos floor = next.below();
        if (level().getBlockState(floor).getCollisionShape(level(), floor).isEmpty()) {
            if (!placeFiller(floor)) {
                blocked();
            }
            return;
        }
        path.add(next.immutable());
        if (stairsDone) {
            segmentLength++;
        }
        // una torcia ogni 10 passi
        if (path.size() % 10 == 0 && inventory.countItem(Items.TORCH) > 0
                && Blocks.TORCH.defaultBlockState().canSurvive(level(), next) && level().getBlockState(next).isAir()) {
            level().setBlock(next, Blocks.TORCH.defaultBlockState(), Block.UPDATE_ALL);
            removeOne(Items.TORCH);
        }
    }

    /** Ostacolo che non si puo' rompere: la scala finisce qui, oppure si passa alla galleria successiva */
    void blocked() {
        if (!stairsDone) {
            stairsDone = true;
            segment = 0;
        } else {
            segment++;
        }
        segmentLength = 0;
    }

    /** Prima di scavare, tappa l'acqua e la lava che toccano il blocco (fuori dal passaggio) */
    void plugFluids(BlockPos p, BlockPos step, int height) {
        for (Direction d : Direction.values()) {
            BlockPos q = p.relative(d);
            boolean inPassage = q.getX() == step.getX() && q.getZ() == step.getZ() && q.getY() >= step.getY()
                    && q.getY() < step.getY() + height;
            if (!inPassage && !level().getBlockState(q).getFluidState().isEmpty()) {
                placeFiller(q);
            }
        }
    }

    boolean placeFiller(BlockPos p) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack s = inventory.getItem(i);
            if (s.getItem() instanceof BlockItem bi && (s.is(Items.COBBLESTONE) || s.is(Items.COBBLED_DEEPSLATE)
                    || s.is(Items.DIRT) || s.is(Items.NETHERRACK) || s.is(Tags.Items.COBBLESTONES)
                    || s.is(Tags.Items.STONES) || s.is(Items.TUFF))) {
                level().setBlock(p, bi.getBlock().defaultBlockState(), Block.UPDATE_ALL);
                s.shrink(1);
                swing(InteractionHand.MAIN_HAND);
                return true;
            }
        }
        // niente blocchi: ne "fabbrica" uno con un po' di energia (per non annegare)
        if (use(200)) {
            level().setBlock(p, Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_ALL);
            return true;
        }
        return false;
    }

    /** Guarda i 6 lati: i minerali li mette in lista (cosi' segue anche le vene) */
    void scanOres(BlockPos p) {
        for (Direction d : Direction.values()) {
            BlockPos q = p.relative(d);
            if (level().getBlockState(q).is(Tags.Blocks.ORES) && !ores.contains(q) && !isFloorOfPath(q)) {
                ores.add(q.immutable());
            }
        }
    }

    void removeOne(Item item) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(item)) {
                inventory.getItem(i).shrink(1);
                return;
            }
        }
    }

    // =====================================================================
    // TAGLIALEGNA
    // =====================================================================
    boolean naturalLog(BlockPos log) {
        for (BlockPos q : BlockPos.betweenClosed(log.offset(-3, -1, -3), log.offset(3, 5, 3))) {
            BlockState s = level().getBlockState(q);
            if (s.getBlock() instanceof LeavesBlock && !s.getValue(LeavesBlock.PERSISTENT)) {
                return true;
            }
        }
        return false;
    }

    void lumber() {
        if (!chop.isEmpty()) {
            BlockPos log = chop.peek();
            if (!level().getBlockState(log).is(BlockTags.LOGS)) {
                chop.poll();
                if (chop.isEmpty()) {
                    replant();
                }
                return;
            }
            if (log.distToCenterSqr(getEyePosition()) > 8 * 8) {
                travel(log, 3);
                return;
            }
            if (breakBlock(log)) {
                chop.poll();
                if (chop.isEmpty()) {
                    replant();
                }
            }
            return;
        }
        if (workTarget == null) {
            if (tickCount % 20 != 0) {
                return;
            }
            workTarget = findTree();
            if (workTarget == null) {
                idle();
                return;
            }
        }
        setState(WORKING);
        if (!travel(workTarget, 2.5)) {
            if (travelStuck > 190) {
                skip.add(workTarget);
                workTarget = null;
            }
            return;
        }
        // tutto l'albero: tronchi collegati (anche in diagonale)
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        open.add(workTarget);
        List<BlockPos> logs = new ArrayList<>();
        while (!open.isEmpty() && logs.size() < 160) {
            BlockPos p = open.poll();
            if (!seen.add(p) || !level().getBlockState(p).is(BlockTags.LOGS)) {
                continue;
            }
            logs.add(p);
            for (BlockPos q : BlockPos.betweenClosed(p.offset(-1, 0, -1), p.offset(1, 1, 1))) {
                open.add(q.immutable());
            }
        }
        chop.addAll(logs);
        replantAt = workTarget;
        workTarget = null;
    }

    @Nullable private BlockPos replantAt;

    void replant() {
        if (replantAt == null) {
            return;
        }
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack s = inventory.getItem(i);
            if (s.is(ItemTags.SAPLINGS) && s.getItem() instanceof BlockItem bi) {
                BlockState sap = bi.getBlock().defaultBlockState();
                if (level().getBlockState(replantAt).isAir() && sap.canSurvive(level(), replantAt)) {
                    level().setBlock(replantAt, sap, Block.UPDATE_ALL);
                    s.shrink(1);
                }
                break;
            }
        }
        replantAt = null;
    }

    @Nullable
    BlockPos findTree() {
        BlockPos center = dock != null ? dock : blockPosition();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-WORK_RADIUS, -4, -WORK_RADIUS),
                center.offset(WORK_RADIUS, 6, WORK_RADIUS))) {
            BlockState s = level().getBlockState(p);
            if (!s.is(BlockTags.LOGS) || level().getBlockState(p.below()).is(BlockTags.LOGS) || skip.contains(p)) {
                continue;
            }
            if (!level().getBlockState(p.below()).is(BlockTags.DIRT)) {
                continue; // la base di un albero sta sulla terra
            }
            double d = p.distToCenterSqr(position());
            if (d < bestD && naturalLog(p)) {
                bestD = d;
                best = p.immutable();
            }
        }
        return best;
    }

    // =====================================================================
    // CONTADINO
    // =====================================================================
    static boolean isSeed(ItemStack stack) {
        return stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof CropBlock;
    }

    void farm() {
        if (workTarget == null) {
            if (tickCount % 20 != 0) {
                return;
            }
            workTarget = findCrop();
            if (workTarget == null) {
                idle();
                return;
            }
        }
        setState(WORKING);
        BlockState s = level().getBlockState(workTarget);
        boolean ripe = s.getBlock() instanceof CropBlock crop && crop.isMaxAge(s);
        boolean empty = s.isAir() && level().getBlockState(workTarget.below()).is(Blocks.FARMLAND);
        if (!ripe && !empty) {
            workTarget = null;
            return;
        }
        if (!travel(workTarget, 2.0)) {
            if (travelStuck > 190) {
                skip.add(workTarget);
                workTarget = null;
            }
            return;
        }
        if (ripe) {
            CropBlock crop = (CropBlock) s.getBlock();
            if (!use(BLOCK_COST)) {
                return;
            }
            List<ItemStack> drops = Block.getDrops(s, (ServerLevel) level(), workTarget, null, this, getMainHandItem());
            ItemStack seed = crop.getCloneItemStack(level(), workTarget, s);
            for (ItemStack d : drops) {
                if (ItemStack.isSameItem(d, seed)) {
                    d.shrink(1); // un seme lo ripianta subito
                    break;
                }
            }
            level().setBlock(workTarget, crop.getStateForAge(0), Block.UPDATE_ALL);
            level().levelEvent(2001, workTarget, Block.getId(s));
            for (ItemStack d : drops) {
                ItemStack rest = inventory.addItem(d);
                if (!rest.isEmpty()) {
                    spawnAtLocation(rest);
                }
            }
            getMainHandItem().hurtAndBreak(1, this, EquipmentSlot.MAINHAND);
            swing(InteractionHand.MAIN_HAND);
        } else {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack seed = inventory.getItem(i);
                if (isSeed(seed)) {
                    BlockState crop = ((BlockItem) seed.getItem()).getBlock().defaultBlockState();
                    if (crop.canSurvive(level(), workTarget)) {
                        level().setBlock(workTarget, crop, Block.UPDATE_ALL);
                        seed.shrink(1);
                        swing(InteractionHand.MAIN_HAND);
                    }
                    break;
                }
            }
        }
        workTarget = null;
    }

    @Nullable
    BlockPos findCrop() {
        BlockPos center = dock != null ? dock : blockPosition();
        boolean hasSeeds = false;
        for (int i = 0; i < inventory.getContainerSize() && !hasSeeds; i++) {
            hasSeeds = isSeed(inventory.getItem(i));
        }
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-WORK_RADIUS, -3, -WORK_RADIUS),
                center.offset(WORK_RADIUS, 3, WORK_RADIUS))) {
            if (skip.contains(p)) {
                continue;
            }
            BlockState s = level().getBlockState(p);
            boolean ripe = s.getBlock() instanceof CropBlock crop && crop.isMaxAge(s);
            boolean empty = hasSeeds && s.isAir() && level().getBlockState(p.below()).is(Blocks.FARMLAND);
            if (ripe || empty) {
                double d = p.distToCenterSqr(position());
                if (d < bestD) {
                    bestD = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    // =====================================================================
    // GUARDIA
    // =====================================================================
    class FollowOwnerGoal extends Goal {
        FollowOwnerGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Player p = ownerPlayer();
            return hasPower() && getMode() == GUARD && p != null && getTarget() == null && distanceToSqr(p) > 25;
        }

        @Override
        public boolean canContinueToUse() {
            Player p = ownerPlayer();
            return hasPower() && getMode() == GUARD && p != null && getTarget() == null && distanceToSqr(p) > 9;
        }

        @Override
        public void tick() {
            Player p = ownerPlayer();
            if (p == null) {
                return;
            }
            if (distanceToSqr(p) > 24 * 24) {
                teleportTo(p.getX(), p.getY(), p.getZ());
                getNavigation().stop();
            } else {
                getNavigation().moveTo(p, 1.2);
            }
        }
    }
}
