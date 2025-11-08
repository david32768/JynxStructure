package com.github.david32768.jynxstructure;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;

import static com.github.david32768.jynxfree.jynx.Global.LOG;
import static com.github.david32768.jynxstructure.my.Message.M510;
import static com.github.david32768.jynxstructure.my.Message.M511;
import static com.github.david32768.jynxstructure.my.Message.M512;
import static com.github.david32768.jynxstructure.my.Message.M513;

import com.github.david32768.jynxfree.jvm.ConstantPoolType;
import com.github.david32768.jynxfree.jvm.ConstantPoolType.EntryType;
import com.github.david32768.jynxfree.jynx.LogIllegalArgumentException;

public class CPEntry {

    private final ConstantPoolType type;
    private final Object value;

    private CPEntry(ConstantPoolType type, Object value) {
        this.type = type;
        this.value = value;
    }

    public ConstantPoolType getType() {
        return type;
    }
    
    public Object getValue() {
        return value;
    }

    public static CPEntry fromConstantPool(ByteBuffer block) {
        int tag = Byte.toUnsignedInt(block.get());
        Optional<ConstantPoolType> cpopt = ConstantPoolType.getInstance(tag);
        if (!cpopt.isPresent()) {
            // "tag %d not found"
            throw new LogIllegalArgumentException(M510, tag);
        }
        ConstantPoolType cp = cpopt.get();
        EntryType et = cp.getEntryType();
        Object value = switch(et) {
            case UTF8 -> fromCPUTF8(block);
            case INTEGER -> block.getInt();
            case FLOAT -> block.getFloat();
            case LONG -> block.getLong();
            case DOUBLE -> block.getDouble();
            case INDIRECT -> {
                int itemct = cp.poolct();
                int[] items = new int[itemct];
                for (int j = 0; j < itemct; ++j) {
                    items[j] = Short.toUnsignedInt(block.getShort());
                }
                yield items;
            }
            case BOOTSTRAP -> {
                int[] items = new int[2];
                items[0] = Short.toUnsignedInt(block.getShort());
                items[1] = Short.toUnsignedInt(block.getShort());
                yield items;
            }
            case HANDLE -> {
                int[] items = new int[2];
                items[0] = Byte.toUnsignedInt(block.get());
                items[1] = Short.toUnsignedInt(block.getShort());
                yield items;
            }
            default -> throw new AssertionError();
        };
        return new CPEntry(cp,value);
    }

    private static String fromCPUTF8(ByteBuffer block) {
        int size = Short.toUnsignedInt(block.getShort());
        byte[] entry = new byte[size];
        block.get(entry);
        return fromUTF8CP(ByteBuffer.wrap(entry));
    }

    private final static int BAD_CHAR = '?';
    
    public static String fromUTF8CP(ByteBuffer block) {
        StringBuilder sb = new StringBuilder(block.remaining());
        while (block.hasRemaining()) {
            int x = Byte.toUnsignedInt(block.get());
            int c;
            if (x < 0x80 && x > 0) {
                c = x;
            } else if ((x & 0xe0) == 0xc0 && block.remaining() >= 1) {
                int y = Byte.toUnsignedInt(block.get());
                if ((y & 0xc0) != 0x80) {
                    // "bad byte sequence %2x %2x"
                    LOG(M511, x, y);
                }
                c = ((x & 0x1f) << 6) + (y & 0x3f);
            } else if ((x & 0xf0) == 0xe0 && block.remaining() >= 2) {
                int y = Byte.toUnsignedInt(block.get());
                int z = Byte.toUnsignedInt(block.get());
                if ((y & 0xc0) != 0x80 || (z & 0xc0) != 0x80) {
                    // "bad byte sequence %2x %2x %2x"
                    LOG(M512, x, y, z);
                }
                c = ((x & 0xf) << 12) + ((y & 0x3f) << 6) + (z & 0x3f);
            } else {
                // "bad byte sequence = %x"
                LOG(M513, x);
                c = BAD_CHAR;
            }
            sb.append((char)c);
        }
        return sb.toString();
    }

    public String typeString() {
        String name = type.abbrev();
        var et = type.getEntryType();
        return switch(et) {
            case UTF8 -> String.format("%s(%d)", name, ((String)value).length());
            case INDIRECT -> String.format("%s%s", name, Arrays.toString((int[])value));
            case HANDLE, BOOTSTRAP -> {
                int[] ivalues = (int[])value;
                yield String.format("%s{%d}[%d]", name, ivalues[0], ivalues[1]);
            }
            default -> name;
        };
    }
    
    @Override
    public String toString() {
        String valuestr;
        if (value instanceof int[] intarr) {
            StringBuilder sb = new StringBuilder();
            sb.append('[').append(' ');
            for (int index : intarr) {
                sb.append(index).append(' ');
            }
            sb.append(']');
            valuestr = sb.toString();
        } else {
            valuestr = value.toString();
        }
        return String.format("type = %s value = %s",type,valuestr);
    }

}
