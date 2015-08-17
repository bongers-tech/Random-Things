package lumien.randomthings.tileentity;

import static lumien.randomthings.tileentity.TileEntityEnderBridge.BRIDGESTATE.IDLE;
import static lumien.randomthings.tileentity.TileEntityEnderBridge.BRIDGESTATE.SCANNING;
import static lumien.randomthings.tileentity.TileEntityEnderBridge.BRIDGESTATE.WAITING;

import java.util.List;

import lumien.randomthings.block.BlockEnderBridge;
import lumien.randomthings.block.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.gui.IUpdatePlayerListBox;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class TileEntityEnderBridge extends TileEntityBase implements IUpdatePlayerListBox
{
	enum BRIDGESTATE
	{
		IDLE, SCANNING, WAITING
	}

	BRIDGESTATE state;

	boolean redstonePowered;
	int scanningCounter;

	public TileEntityEnderBridge()
	{
		redstonePowered = false;
		scanningCounter = 2;
		state = IDLE;
	}

	@Override
	public void update()
	{
		if (!worldObj.isRemote)
		{
			if (state == SCANNING)
			{
				IBlockState blockState = worldObj.getBlockState(pos);
				EnumFacing facing = (EnumFacing) blockState.getValue(BlockEnderBridge.FACING);
				BlockPos nextPos = new BlockPos(pos.offset(facing, scanningCounter));
				IBlockState nextState = worldObj.getBlockState(nextPos);
				if (nextState.getBlock() == ModBlocks.enderAnchor)
				{
					List<EntityPlayerMP> playerList = worldObj.getEntitiesWithinAABB(EntityPlayerMP.class, new AxisAlignedBB(pos.getX() - 2, pos.getY() - 2, pos.getZ() - 2, pos.getX() + 2, pos.getY() + 2, pos.getZ() + 2));
					if (!playerList.isEmpty())
					{
						BlockPos target = nextPos.up();
						for (EntityPlayerMP player : playerList)
						{
							player.playerNetServerHandler.setPlayerLocation(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, player.rotationYaw, player.rotationPitch);
						}
					}

					state = WAITING;
				}
				else if (!nextState.getBlock().isAir(worldObj, nextPos))
				{
					state = WAITING;
				}

				scanningCounter++;
			}
		}
	}

	public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState blockState, Block neighborBlock)
	{
		if (!worldIn.isRemote)
		{
			boolean powered = worldIn.isBlockIndirectlyGettingPowered(pos) != 0;

			if (powered != redstonePowered)
			{
				if (state == IDLE && powered)
				{
					scanningCounter = 2;
					state = SCANNING;
				}
				else if (state == SCANNING && !powered)
				{
					state = IDLE;
				}
				else if (state == WAITING && !powered)
				{
					state = IDLE;
				}
				redstonePowered = powered;
			}
		}
	}

	@Override
	public void writeDataToNBT(NBTTagCompound compound)
	{
		compound.setInteger("state", state.ordinal());
		compound.setBoolean("redstonePowered", redstonePowered);
		compound.setInteger("scanningCounter", scanningCounter);
	}

	@Override
	public void readDataFromNBT(NBTTagCompound compound)
	{
		state = BRIDGESTATE.values()[compound.getInteger("state")];
		redstonePowered = compound.getBoolean("redstonePowered");
		scanningCounter = compound.getInteger("scanningCounter");
	}
}