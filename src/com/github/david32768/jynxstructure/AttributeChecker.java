package com.github.david32768.jynxstructure;

import java.util.Optional;

import static com.github.david32768.jynxfree.jynx.Global.JVM_VERSION;
import static com.github.david32768.jynxfree.jynx.Global.LOG;
import static com.github.david32768.jynxstructure.my.Message.M506;
import static com.github.david32768.jynxstructure.my.Message.M507;
import static com.github.david32768.jynxstructure.my.Message.M519;
import static com.github.david32768.jynxstructure.my.Message.M521;

import com.github.david32768.jynxfree.jvm.AccessFlag;
import com.github.david32768.jynxfree.jvm.AttributeEntry;
import com.github.david32768.jynxfree.jvm.ConstantPoolType;
import com.github.david32768.jynxfree.jvm.Context;
import com.github.david32768.jynxfree.jvm.FrameType;
import com.github.david32768.jynxfree.jvm.JvmVersion;
import com.github.david32768.jynxfree.jynx.LogIllegalArgumentException;

public class AttributeChecker {

    private AttributeChecker() {
    }
    
    public static void check(AttributeEntry[] entries, AttributeBuffer buffer) {
        for (AttributeEntry entry:entries) {
            AttributeChecker.checkEntry(entry, buffer);
        }
    }

    private static void checkEntry(AttributeEntry entry, AttributeBuffer buffer) {
        switch(entry) {
            case ANNOTATION, DEFAULT_ANNOTATION, PARAMETER_ANNOTATION, TYPE_ANNOTATION ->
                AnnotationEntry.get(entry).accept(buffer);
            case LABEL -> buffer.asCodeBuffer().nextLabel();
            case LV_INDEX -> buffer.asCodeBuffer().nextVar();
            case INNERCLASS_ACCESS -> {
                int flags =  buffer.nextUnsignedShort();
                AccessFlag.getEnumSet(flags, Context.INNER_CLASS, JVM_VERSION());
            }
            case METHOD_PARAMETER_ACCESS -> {
                int flags = buffer.nextUnsignedShort();
                AccessFlag.getEnumSet(flags, Context.PARAMETER, JVM_VERSION());
            }
            case USHORT -> buffer.nextUnsignedShort();
            case LABEL_LENGTH -> checkLabelLength(buffer.asCodeBuffer());
            case INLINE_UTF8 -> CPEntry.fromUTF8CP(buffer.bb());
            case FRAME -> checkStackFrame(buffer.asCodeBuffer());
            case BOOTSTRAP -> AttributeChecker.checkBootstrap(buffer);
            case CONSTANT, CLASSNAME, OPT_CLASSNAME, UTF8, OPT_UTF8, OPT_NAME_TYPE, PACKAGENAME, STRING -> {
                assert entry.isCP();
                Optional<CPEntry> optentry = buffer.nextOptCPEntry();
                if (optentry.isPresent() || !entry.isOptional()) {
                    // "non-optional constant pool entry is missing; expected %s"
                    CPEntry cp = optentry.orElseThrow(() -> new LogIllegalArgumentException(M506,entry));
                    ConstantPoolType cptype = cp.getType();
                    if (!entry.contains(cptype)) {
                        // "cpentry type %s is invalid for %s"
                        LOG(M519, cptype, entry);
                    }
                }
            }
            default -> throw new EnumConstantNotPresentException(entry.getClass(), entry.name());
        }
    }
    
    private static void checkLabelLength(CodeBuffer buffer) {
        int start = buffer.nextLabel();
        int end = buffer.nextEndOffset(start);
    }

    private static void checkBootstrap(AttributeBuffer buffer) {
        CPEntry methodcp = buffer.nextCPEntry(ConstantPoolType.CONSTANT_MethodHandle);
        JvmVersion jvmversion = JVM_VERSION();
        int argct = buffer.nextUnsignedShort();
        CPEntry[] entries = new CPEntry[1 + argct];
        entries[0] = methodcp;
        for (int k = 0; k < argct; ++k) {
            CPEntry argcp = buffer.nextCPEntry();
            ConstantPoolType cptk = argcp.getType();
            if (!cptk.isLoadableBy(jvmversion)) {
                // "boot argument %s is not loadable by %s"
                LOG(M507, cptk, jvmversion);
            }
            entries[k + 1] = argcp;
        }
        buffer.pool().addBootstrap(entries);
    }

    private static void checkStackFrame(CodeBuffer buffer) {
        int tag = buffer.nextUnsignedByte();
        int delta;
        if (tag < 64) { // same frame
            delta = tag;
        } else if (tag < 128) { // same_locals_1_stack_item_frame
            delta = tag - 64;
            checkVerificationTypeInfo(buffer,1);
        } else {
            switch (tag) {
                default ->
                    // future use
                    // "invalid tag %d"
                    throw new LogIllegalArgumentException(M521,tag);
                case 247 -> {
                    // same_locals_1_stack_item_frame_extended
                    delta = buffer.nextUnsignedShort();
                    checkVerificationTypeInfo(buffer,1);
                }
                case 248, 249, 250 -> // chop
                    delta = buffer.nextUnsignedShort();
                case 251 -> // same_frame_extended
                    delta = buffer.nextUnsignedShort();
                case 252, 253, 254 -> {
                    // append_frame
                    delta = buffer.nextUnsignedShort();
                    checkVerificationTypeInfo(buffer,tag - 251);
                }
                case 255 -> {
                    // full_frame
                    delta = buffer.nextUnsignedShort();
                    int locals = buffer.nextUnsignedShort();
                    checkVerificationTypeInfo(buffer,locals);
                    int stack = buffer.nextUnsignedShort();
                    checkVerificationTypeInfo(buffer,stack);
                }
            }
        }
        buffer.addDelta(delta);
    }
    
    private static void checkVerificationTypeInfo(CodeBuffer buffer,int size) {
        for (int i = 0; i <size; ++i) {
            int tag = buffer.nextUnsignedByte();
            FrameType ft = FrameType.fromJVMType(tag);
            switch (ft) {
                case ft_Object -> {
                    buffer.nextCPEntry(ConstantPoolType.CONSTANT_Class);
                }
                case ft_Uninitialized -> {
                    buffer.asCodeBuffer().nextLabel();
                }
                default -> {
                    assert ft.asmType() != null;
                }
            }
        }
    }
}
