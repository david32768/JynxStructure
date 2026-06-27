package com.github.david32768.jynxstructure.attribute;

import java.util.Optional;

import static com.github.david32768.jynxfree.jynx.Global.JVM_VERSION;
import static com.github.david32768.jynxfree.jynx.Global.LOG;
import static com.github.david32768.jynxfree.jynx.Global.OPTION;

import static com.github.david32768.jynxstructure.my.Message.M506;
import static com.github.david32768.jynxstructure.my.Message.M519;

import com.github.david32768.jynxfree.jvm.AccessFlag;
import com.github.david32768.jynxfree.jvm.AttributeEntry;
import com.github.david32768.jynxfree.jvm.ConstantPoolType;
import com.github.david32768.jynxfree.jvm.Context;
import com.github.david32768.jynxfree.jynx.GlobalOption;
import com.github.david32768.jynxfree.jynx.LogIllegalArgumentException;
import com.github.david32768.jynxfree.jynx.LogUnexpectedEnumValueException;
import com.github.david32768.jynxstructure.AttributeBuffer;
import com.github.david32768.jynxstructure.CodeBuffer;
import com.github.david32768.jynxstructure.CPEntry;
import com.github.david32768.jynxstructure.IndentPrinter;

public class SimpleAttributeElement {

    protected final IndentPrinter ptr;
    protected final AttributeEntry entry;
    protected final AttributeBuffer buffer;
    protected final boolean detail;

    protected SimpleAttributeElement(IndentPrinter ptr, AttributeEntry entry, AttributeBuffer buffer) {
        this.ptr = ptr;
        this.entry = entry;
        this.buffer = buffer;
        this.detail = OPTION(GlobalOption.DETAIL) || OPTION(GlobalOption.DETAIL_INSTRUCTIONS);
    }
    
    public static SimpleAttributeElement of(IndentPrinter ptr, AttributeEntry entry, AttributeBuffer buffer) {
        return switch (entry) {
            case ANNOTATION -> new AnnotationElement(ptr, entry, buffer);
            case DEFAULT_ANNOTATION -> new DefaultAnnotationElement(ptr, entry, buffer);
            case PARAMETER_ANNOTATION -> new ParameterAnnotationElement(ptr, entry, buffer);
            case TYPE_ANNOTATION -> new TypeAnnotationElement(ptr, entry, buffer);
            case BOOTSTRAP -> new BootStrapAttributeElement(ptr, entry, buffer);
            case FRAME -> new FrameAttributeElement(ptr, entry, buffer);
            default -> new SimpleAttributeElement(ptr, entry, buffer);
        };
    }
    
    public void check() {
        switch(entry) {
            case LABEL -> buffer.asCodeBuffer().nextLabel();
            case LV_INDEX -> buffer.asCodeBuffer().nextVar();
            case INNERCLASS_ACCESS -> checkFlags(Context.INNER_CLASS);
            case METHOD_PARAMETER_ACCESS -> checkFlags(Context.PARAMETER);
            case USHORT -> buffer.nextUnsignedShort();
            case LABEL_LENGTH -> checkLabelLength(buffer.asCodeBuffer());
            case INLINE_UTF8 -> CPEntry.fromUTF8CP(buffer.bb());
            case CONSTANT, CLASSNAME, OPT_CLASSNAME, UTF8, OPT_UTF8,
                    OPT_NAME_TYPE, PACKAGENAME, STRING -> checkCPEntry();
            default -> throw new LogUnexpectedEnumValueException(entry);
        }
    }
    
    private void checkFlags(Context context) {
        int flags = buffer.nextUnsignedShort();
        AccessFlag.getEnumSet(flags, context, JVM_VERSION());
    }
    
    private void checkLabelLength(CodeBuffer buffer) {
        int start = buffer.nextLabel();
        int end = buffer.nextEndOffset(start);
    }

    private void checkCPEntry() {
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
}
