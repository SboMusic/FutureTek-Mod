package com.futuretek.robot;

import java.util.List;

import com.futuretek.block.MachineBlock;
import com.futuretek.blockentity.MachineBlockEntity;
import com.futuretek.energy.EnergyUtil;
import com.futuretek.registry.ModBlockEntities;
import com.futuretek.registry.ModBlocks;
import com.futuretek.registry.ModEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Oggetto robot e base di ricarica */
public final class RobotParts {
    private RobotParts() {
    }

    /** Base di ricarica: prende energia dai cavi e la passa al robot che ci sta sopra */
    public static class DockBlock extends MachineBlock {
        public DockBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return simpleCodec(DockBlock::new);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new DockEntity(pos, state);
        }
    }

    public static class DockEntity extends MachineBlockEntity {
        public DockEntity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.ROBOT_DOCK.get(), pos, state, 100_000, 1024, 1024);
        }

        @Override
        public void serverTick() {
            setLit(energy.getEnergyStored() > 0);
        }
    }

    /** Il robot "spento": click destro su una base di ricarica (o per terra) per accenderlo */
    public static class RobotItemBase extends Item {
        public RobotItemBase(Properties props) {
            super(props.stacksTo(1));
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            if (!(context.getLevel() instanceof ServerLevel level)) {
                return InteractionResult.SUCCESS;
            }
            BlockPos clicked = context.getClickedPos();
            boolean onDock = level.getBlockState(clicked).is(ModBlocks.ROBOT_DOCK.get());
            BlockPos at = onDock ? clicked.above() : clicked.relative(context.getClickedFace());
            ItemStack stack = context.getItemInHand();
            RobotEntity robot = ModEntities.ROBOT.get().spawn(level, at, MobSpawnType.SPAWN_EGG);
            if (robot == null) {
                return InteractionResult.FAIL;
            }
            CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            robot.setEnergy(data.getInt("Energy"));
            robot.setMode(data.getInt("Mode"));
            robot.setInfinite(data.getBoolean("Infinite"));
            robot.setDock(onDock ? clicked.immutable() : null);
            if (context.getPlayer() != null) {
                // il pannello in mano all'altro braccio viene collegato subito
                ItemStack other = context.getPlayer().getOffhandItem();
                if (other.getItem() instanceof Controller) {
                    Controller.link(other, robot);
                }
            }
            if (context.getPlayer() != null) {
                robot.setOwner(context.getPlayer().getUUID());
                if (!onDock) {
                    context.getPlayer().displayClientMessage(Component.translatable("message.futuretek.robot_no_dock"), true);
                }
            }
            stack.shrink(1);
            return InteractionResult.CONSUME;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            tooltip.add(Component.translatable("gui.futuretek.energy", EnergyUtil.format(data.getInt("Energy")),
                    EnergyUtil.format(RobotEntity.CAPACITY)).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("message.futuretek.robot_job",
                    Component.translatable("gui.futuretek.robot_mode." + data.getInt("Mode"))).withStyle(ChatFormatting.AQUA));
            if (data.getBoolean("Infinite")) {
                tooltip.add(Component.translatable("item.futuretek.infinite_core").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            com.futuretek.alchemy.AlchemyItem.addDescription(this, tooltip);
        }
    }

    /**
     * Pannello di controllo dei robot:
     *  - click destro su un robot: lo collega al pannello
     *  - click destro su un baule (o qualsiasi contenitore, anche lontano): diventa il baule del robot
     *  - click destro su una base di ricarica: diventa la base del robot
     *  - click destro in aria: stato del robot · Shift + click destro in aria: "continua"
     */
    public static class Controller extends Item {
        public Controller(Properties props) {
            super(props.stacksTo(1));
        }

        @javax.annotation.Nullable
        public static java.util.UUID linked(ItemStack stack) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            return tag.hasUUID("Robot") ? tag.getUUID("Robot") : null;
        }

        public static void link(ItemStack stack, RobotEntity robot) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Robot", robot.getUUID());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }

        @javax.annotation.Nullable
        static RobotEntity robot(net.minecraft.world.level.Level level, ItemStack stack) {
            java.util.UUID id = linked(stack);
            if (id == null || !(level instanceof ServerLevel server)) {
                return null;
            }
            return server.getEntity(id) instanceof RobotEntity r && r.isAlive() ? r : null;
        }

        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, net.minecraft.world.entity.player.Player player,
                net.minecraft.world.entity.LivingEntity target, net.minecraft.world.InteractionHand hand) {
            if (!(target instanceof RobotEntity robot)) {
                return InteractionResult.PASS;
            }
            if (!player.level().isClientSide) {
                link(player.getItemInHand(hand), robot);
                player.displayClientMessage(Component.translatable("message.futuretek.controller_linked",
                        Component.translatable("gui.futuretek.robot_mode." + robot.getMode())), true);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            net.minecraft.world.level.Level level = context.getLevel();
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            net.minecraft.world.entity.player.Player player = context.getPlayer();
            RobotEntity robot = robot(level, context.getItemInHand());
            if (robot == null) {
                if (player != null) {
                    player.displayClientMessage(Component.translatable(linked(context.getItemInHand()) == null
                            ? "message.futuretek.controller_none" : "message.futuretek.controller_far"), true);
                }
                return InteractionResult.FAIL;
            }
            BlockPos pos = context.getClickedPos();
            if (level.getBlockState(pos).is(ModBlocks.ROBOT_DOCK.get())) {
                robot.setDock(pos);
                if (player != null) {
                    player.displayClientMessage(Component.translatable("message.futuretek.controller_dock"), true);
                }
                return InteractionResult.CONSUME;
            }
            if (level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, null) != null) {
                robot.setChest(pos);
                if (player != null) {
                    player.displayClientMessage(Component.translatable("message.futuretek.controller_chest",
                            pos.getX(), pos.getY(), pos.getZ()), true);
                }
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }

        @Override
        public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,
                net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide) {
                return net.minecraft.world.InteractionResultHolder.success(stack);
            }
            RobotEntity robot = robot(level, stack);
            if (robot == null) {
                player.displayClientMessage(Component.translatable(linked(stack) == null
                        ? "message.futuretek.controller_none" : "message.futuretek.controller_far"), true);
                return net.minecraft.world.InteractionResultHolder.fail(stack);
            }
            if (player.isShiftKeyDown()) {
                robot.resume();
                player.displayClientMessage(Component.translatable("message.futuretek.robot_resume"), true);
            } else {
                player.displayClientMessage(robot.describe(), false);
                BlockPos c = robot.getChest();
                player.displayClientMessage(Component.translatable("message.futuretek.controller_where",
                        robot.blockPosition().getX(), robot.blockPosition().getY(), robot.blockPosition().getZ(),
                        c == null ? "-" : c.getX() + " " + c.getY() + " " + c.getZ()), false);
            }
            return net.minecraft.world.InteractionResultHolder.consume(stack);
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return linked(stack) != null;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable(linked(stack) == null ? "tooltip.futuretek.robot_controller.free"
                    : "tooltip.futuretek.robot_controller.linked").withStyle(ChatFormatting.AQUA));
            com.futuretek.alchemy.AlchemyItem.addDescription(this, tooltip);
        }
    }
}
