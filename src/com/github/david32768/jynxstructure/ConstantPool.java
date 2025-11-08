package com.github.david32768.jynxstructure;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.david32768.jynxfree.jynx.Global.LOG;
import static com.github.david32768.jynxfree.jynx.Global.OPTION;
import static com.github.david32768.jynxstructure.my.Message.M505;
import static com.github.david32768.jynxstructure.my.Message.M514;
import static com.github.david32768.jynxstructure.my.Message.M515;
import static com.github.david32768.jynxstructure.my.Message.M520;
import static com.github.david32768.jynxstructure.my.Message.M522;
import static com.github.david32768.jynxstructure.my.Message.M529;
import static com.github.david32768.jynxstructure.my.Message.M530;
import static com.github.david32768.jynxstructure.my.Message.M534;
import static com.github.david32768.jynxstructure.my.Message.M535;
import static com.github.david32768.jynxstructure.my.Message.M536;

import com.github.david32768.jynxfree.jvm.ConstantPoolType;
import com.github.david32768.jynxfree.jvm.HandleType;
import com.github.david32768.jynxfree.jvm.JvmVersion;
import com.github.david32768.jynxfree.jynx.GlobalOption;
import com.github.david32768.jynxfree.jynx.LogIllegalArgumentException;
import com.github.david32768.jynxfree.jynx.LogUnexpectedEnumValueException;
import com.github.david32768.jynxfree.jynx.StringUtil;

public class ConstantPool {

    private final CPEntry[] entries;
    private final JvmVersion jvmVersion;
    private final List<CPEntry[]> bootstraps;
    private final BitSet used;
    private final BitSet usedboot;

    private ConstantPool(CPEntry[] entries, JvmVersion jvmversion) {
        this.entries = entries.clone();
        this.jvmVersion = jvmversion;
        this.bootstraps = new ArrayList<>();
        this.used = new BitSet(entries.length);
        this.usedboot = new BitSet();
    }

    public void addBootstrap(CPEntry[] bootstrap) {
        this.bootstraps.add(bootstrap);
    }
    
    public int last() {
        int last = entries.length - 1;
        return entries[last] == null? last - 1: last;
    }

    public static ConstantPool getInstance(ByteBuffer bb, JvmVersion jvmversion) {
        int entryct = Short.toUnsignedInt(bb.getShort());
        CPEntry[] entries = new CPEntry[entryct];
        for (int i = 1; i < entryct;++i) {
            CPEntry cp  = CPEntry.fromConstantPool(bb);
            entries[i] = cp;
            ConstantPoolType type = cp.getType();
            if (type.usesTwoSlots()) {
                ++i;
            }
        }
        return new ConstantPool(entries, jvmversion);
    }

    public ConstantPoolType getType(int index) {
        return getEntry(index).getType();
    }
    
    public Object getValue(int index) {
        return getEntry(index).getValue();
    }
    
    public CPEntry getEntry(int index) {
        if (index < 1 || index >= entries.length) {
            // "CP index %d is not in [1,%d]"
            throw new LogIllegalArgumentException(M520, index, entries.length - 1);
        }
        CPEntry result = entries[index];
        if (result == null) {
            CPEntry previous = entries[index - 1]; 
            if (previous != null && previous.getType().usesTwoSlots()) {
                // "CPIndex %d is invalid as points to middle of %s entry"
                throw new LogIllegalArgumentException(M522, index , previous.getType());
            } else {
                throw new AssertionError();
            }
        }
        usedIndex(index);
        return result;
    }

    private void checkPoolType (int base,int index,EnumSet<ConstantPoolType> expected) {
        ConstantPoolType actual = getType(index);
        if (!expected.contains(actual)){
            // "entry %d (%s) refers to index %d (%s) but expected to be in %s"
            throw new LogIllegalArgumentException(M514, base,getType(base), index, actual, expected);
        }
    }
    
    private void checkPoolType (int base,int index, ConstantPoolType expected) {
        checkPoolType(base,index,EnumSet.of(expected));
    }
    
    public void check() {
        for (int i = 1; i < entries.length; ++i) {
            CPEntry cp = entries[i];
            if (cp == null) {
                continue;
            }
            ConstantPoolType.EntryType et = cp.getType().getEntryType();
            Object value = cp.getValue();
            switch(et) {
                case INDIRECT -> {
                    int[] indices = (int[])value;
                    ConstantPoolType[] types = cp.getType().getPool();
                    if (indices.length != types.length) {
                        // "number of cp entries (%d) for entry %d does not equal number required for type (5d)"
                        throw new LogIllegalArgumentException(M515, indices.length, i, types.length);
                    }
                    for (int j = 0; j < indices.length; ++j) {
                        checkPoolType(i,indices[j],types[j]);
                    }
                }
                case HANDLE -> {
                    int[] indices = (int[])value;
                    int tag = indices[0];
                    int index = indices[1];
                    HandleType ht = HandleType.getInstance(tag);
                    checkPoolType(i,index,ht.getValidCPT(jvmVersion));
                }
                case BOOTSTRAP -> {
                    int[] indices = (int[])value;
                    ConstantPoolType[] types = cp.getType().getPool();
                    int bootstrap = indices[0];
                    usedboot.set(bootstrap);
                    checkPoolType(i,indices[1],types[0]);
                }
                case LONG, DOUBLE -> ++i;
                case UTF8, INTEGER, FLOAT -> {}
                default -> {
                    throw new LogUnexpectedEnumValueException(et);
                }
            }
        }
    }
    
    public void resetUsed() {
        used.clear();
    }
    
    public void usedIndex(int cpindex) {
        used.set(cpindex);
        CPEntry cp = entries[cpindex];
        ConstantPoolType.EntryType et = cp.getType().getEntryType();
        Object value = cp.getValue();
        switch(et) {
            case INDIRECT -> {
                int[] indices = (int[])value;
                for (int j = 0; j < indices.length; ++j) {
                    usedIndex(indices[j]);
                }
            }
            case HANDLE, BOOTSTRAP -> {
                int[] indices = (int[])value;
                int index = indices[1];
                usedIndex(index);
            }
            case UTF8, INTEGER, FLOAT, LONG, DOUBLE -> {}
            default -> {
                throw new LogUnexpectedEnumValueException(et);
            }
        }
    }
    
    public void checkUsed() {
        BitSet saved = (BitSet)used.clone();
        int ct = 0;
        for (int i = 1; i < entries.length; ++i) {
            CPEntry cpentry = entries[i];
            if (cpentry != null && !saved.get(i)) {
                if (OPTION(GlobalOption.DETAIL) || OPTION(GlobalOption.DETAIL_CONSTANT_POOL)) {
                    // "Constant Pool Entry %d is not used: type = %s value = %s"
                    LOG(M530, i, cpentry.typeString(), StringUtil.printable(stringValue(cpentry)));
                }
                ++ct;
            }
        }
        if (ct != 0) {
            // "%d constant pool entries are not used"
            LOG(M529, ct);
        }
    }
    
    private String toString(int index) {
        CPEntry cpe = getEntry(index);
        return stringValue(cpe);
    }
    
    public String stringValue(CPEntry cpe) {
        Object value = cpe.getValue();
        ConstantPoolType cpt = cpe.getType();
        return switch(cpt.getEntryType()) {
            case INDIRECT -> {
                int[] indices = (int[])value;
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (int i:indices) {
                    if (!first) {
                        sb.append(' ');
                    }
                    sb.append(toString(i));
                    first = false;
                }
                yield sb.toString();
            }
            case HANDLE -> {
                int[] indices = (int[])value;
                HandleType ht = HandleType.getInstance(indices[0]);
                yield String.format("%s %s", ht.name(), toString(indices[1]));
            }
            case BOOTSTRAP -> {
                int[] indices = (int[])value;
                int bootstrap = indices[0];
                yield String.format("BOOTSTRAP[%d] %s", bootstrap, toString(indices[1]));
            }
            case LONG -> {
                yield value.toString() + 'L';
            }
            case FLOAT -> {
                yield value.toString() + 'F';
            }
            default -> {
                yield value.toString();
            }
        };
    }

    public void printCP(IndentPrinter ptr) {
        ptr.println("CONSTANT POOL ENTRIES");
        IndentPrinter entryptr = ptr.shift();
        Map<String,Integer> strmap = new HashMap<>();
        for (int i = 0; i < entries.length; ++i) {
            CPEntry cp = entries[i];
            if (cp == null) {
                continue;
            }
            String cpstr = stringValue(cp);
            ConstantPoolType cpt = cp.getType();
            if (cpt == ConstantPoolType.CONSTANT_String) {
                cpstr = StringUtil.QuoteEscape(cpstr);
            } else {
                cpstr = StringUtil.printable(cpstr);
            }
            entryptr.println("%-4d %-24s %s", i, cp.typeString(), cpstr);
            Integer previous = strmap.put(cp.getType().name() + " " + cpstr, i);
            if (previous != null) {
                // "*** entry %d is duplicate of %d ***"
                entryptr.println(M534.format(i, previous));
            }
        }
    }

    public void checkBootstraps() {
        int bootct = bootstraps.size();
        int errct = 0;
        for (int i = 0; i < bootct; ++i) {
            if (!usedboot.get(i)) {
                ++errct;
            }
        }
        if (errct != 0) {
            // "%d bootstraps are not used"
            LOG(M535, errct);
        }
        int maxboot = usedboot.length() - 1;
        if (maxboot >= 0 && bootct <= maxboot) {
            // "maximum bootcp used by constant pool (%d)  is greater than supplied in attribute (size = %d)"
            LOG(M505, maxboot, bootct);
        }
    }
    
    public void printBoot(IndentPrinter ptr) {
        if (bootstraps.isEmpty()) {
            return;
        }
        ptr.println("BOOTSTRAP ENTRIES");
        IndentPrinter bootptr = ptr.shift();
        for (int i = 0; i < bootstraps.size(); ++i) {
            printBoot(bootptr,i);
        }
    }

    private void printBoot(IndentPrinter bootptr, int index) {
        CPEntry[] boots = bootstraps.get(index);
        IndentPrinter methodptr = bootptr.shift();
        IndentPrinter argptr = methodptr.shift();
        CPEntry methodcp = boots[0];
        bootptr.println("BOOTSTRAP %d",index);
        methodptr.println(stringValue(methodcp));
        for (int k = 1; k < boots.length; ++k) {
            CPEntry argcp = boots[k];
            argptr.println("%d %-24s %s",k - 1,argcp.typeString(),stringValue(argcp));
        }
        if (!usedboot.get(index)) {
            // "BootStrap %d is not used"
            LOG(M536, index);
        }
    }
    
}
