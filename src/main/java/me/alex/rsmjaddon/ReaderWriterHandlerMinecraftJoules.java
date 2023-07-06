package me.alex.rsmjaddon;

import buildcraft.api.mj.*;
import com.raoulvdberge.refinedstorage.RS;
import com.raoulvdberge.refinedstorage.api.network.readerwriter.IReader;
import com.raoulvdberge.refinedstorage.api.network.readerwriter.IReaderWriterChannel;
import com.raoulvdberge.refinedstorage.api.network.readerwriter.IReaderWriterHandler;
import com.raoulvdberge.refinedstorage.api.network.readerwriter.IWriter;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class ReaderWriterHandlerMinecraftJoules implements IReaderWriterHandler {
    private final String NBT_ENERGY_STORED = "MJEnergyStored";
    public static final String ID = "minecraftjoules";
    private final MJRWHandler storageReader;
    private final MJRWHandler storageWriter;
    private final MjBattery storage;

    public ReaderWriterHandlerMinecraftJoules(@Nullable NBTTagCompound tag) {
        int capacity = RS.INSTANCE.config.readerWriterChannelEnergyCapacity;

        this.storage = new MjBattery(capacity * MjAPI.MJ);
        readFromNBT(storage, tag);

        this.storageReader = new MJRWHandler();
        this.storageWriter = new MJRWHandler();
    }

    private static final IMjConnector NULL_CAP = new IMjReceiver() {
        @Override
        public long getPowerRequested() {
            return 0;
        }

        @Override
        public long receivePower(long microJoules, boolean simulate) {
            return 0;
        }

        @Override
        public boolean canConnect(@Nonnull IMjConnector other) {
            return true;
        }
    };

    private class MJRWHandler implements IMjReceiver {
        @Override
        public long getPowerRequested() {
            return (long) ((storage.getCapacity()*1.3) - storage.getStored());
        }

        @Override
        public long receivePower(long microJoules, boolean simulate) {
            if (storage.isFull()) {
                return microJoules;
            }

            return storage.addPower(Math.min(microJoules, getPowerRequested()), simulate);
        }

        @Override
        public boolean canConnect(@Nonnull IMjConnector other) {
            return true;
        }
    }

    @Override
    public void update(IReaderWriterChannel iReaderWriterChannel) {
        if (iReaderWriterChannel.getWriters().isEmpty()) return;

        int toSend = (int) Math.floor((float) storage.getStored() / iReaderWriterChannel.getWriters().size());
        int toExtract = 0;
        int returned = 0;

        for (IWriter writer : iReaderWriterChannel.getWriters()) {
            if (writer.canUpdate()) {
                TileEntity tile = writer.getWorld().getTileEntity(writer.getPos().offset(writer.getDirection()));

                if (tile != null && tile.hasCapability(MjAPI.CAP_RECEIVER, writer.getDirection().getOpposite())) {
                    if (storage.extractPower(toSend)) {
                        IMjReceiver receiver = tile.getCapability(MjAPI.CAP_RECEIVER, writer.getDirection().getOpposite());

                        if (receiver != null) {
                            long amountSent = Math.min(toSend, receiver.getPowerRequested());
                            returned += receiver.receivePower(amountSent, false);
                            toExtract += amountSent;
                        }
                    }
                }
            }
        }

        storage.extractPower(toExtract-returned);
    }

    @Override
    public void onWriterDisabled(IWriter iWriter) {
    }

    @Override
    public boolean hasCapabilityReader(IReader iReader, Capability<?> capability) {
        return capability == MjAPI.CAP_RECEIVER || capability == MjAPI.CAP_CONNECTOR;
    }

    @Override
    public <T> T getCapabilityReader(IReader iReader, Capability<T> capability) {
        return new MjCapabilityHelper(storageReader).getCapability(capability, null);
    }

    @Override
    public boolean hasCapabilityWriter(IWriter iWriter, Capability<?> capability) {
        return capability == MjAPI.CAP_RECEIVER || capability == MjAPI.CAP_CONNECTOR;
    }

    @Override
    public <T> T getCapabilityWriter(IWriter iWriter, Capability<T> capability) {
        return new MjCapabilityHelper(storageWriter).getCapability(capability, null);
    }

    @Override
    public Object getNullCapability() {
        return NULL_CAP;
    }

    @Override
    public NBTTagCompound writeToNbt(NBTTagCompound tag) {
        tag.setLong(NBT_ENERGY_STORED, storage.getStored());

        return tag;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<ITextComponent> getStatusReader(IReader iReader, IReaderWriterChannel iReaderWriterChannel) {
        return getStatus();
    }

    @Override
    public List<ITextComponent> getStatusWriter(IWriter iWriter, IReaderWriterChannel iReaderWriterChannel) {
        return getStatus();
    }

    private void readFromNBT(MjBattery storage, NBTTagCompound tag) {
        if (tag != null && tag.hasKey(NBT_ENERGY_STORED)) {
            storage.addPowerChecking(tag.getLong(NBT_ENERGY_STORED), false);
        }
    }

    public List<ITextComponent> getStatus() {
        long currentlyStored = storage.getStored();
        long maximum = storage.getCapacity();
        float percentage = ((float) currentlyStored / maximum) * 100;

        String stored = MjAPI.formatMj(currentlyStored);
        String capacity = MjAPI.formatMj(maximum);

        return Collections.singletonList(new TextComponentString(stored + "/" + capacity + " MJ (" + percentage + " %)"));
    }
}
