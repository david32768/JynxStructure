package com.github.david32768.jynxstructure.attribute;

import static com.github.david32768.jynxfree.jynx.Global.JVM_VERSION;

import static com.github.david32768.jynxstructure.my.Message.M521;
import static com.github.david32768.jynxstructure.my.Message.M537;

import com.github.david32768.jynxfree.jvm.AttributeEntry;
import com.github.david32768.jynxfree.jvm.ConstantPoolType;
import com.github.david32768.jynxfree.jvm.FrameTag;
import com.github.david32768.jynxfree.jvm.FrameType;
import com.github.david32768.jynxfree.jynx.LogIllegalArgumentException;

import com.github.david32768.jynxstructure.AttributeBuffer;
import com.github.david32768.jynxstructure.CPEntry;
import com.github.david32768.jynxstructure.IndentPrinter;

public class FrameAttributeElement extends SimpleAttributeElement {
    
    public FrameAttributeElement(IndentPrinter ptr, AttributeEntry entry, AttributeBuffer buffer) {
        super(ptr, entry, buffer);
    }
    
    @Override
    public void check() {
        int itag = buffer.nextUnsignedByte();
        checkFrame(itag);
    }
    
    private void checkFrame(int itag) {
        FrameTag tag = FrameTag.of(itag, JVM_VERSION());
        if (tag == null) {
            // future use
            // "invalid tag %d"
            throw new LogIllegalArgumentException(M521, itag);            
        }
        
        if (tag == FrameTag.early_larval_frame) {
            int unsetct = buffer.nextUnsignedShort();
            CPEntry[] cpentries = new CPEntry[unsetct];
            for (int i = 0; i < unsetct; ++i) {
                cpentries[i] = buffer.nextCPEntry(ConstantPoolType.CONSTANT_NameAndType);
            }
            int basetag = buffer.nextUnsignedByte();
            if (basetag == 246) {
                // "base tag in early larval cannot be %d"
                throw new LogIllegalArgumentException(M537, basetag);
            }
            checkFrame(basetag);
            if (detail) {
                var ptr1 = ptr.shift();
                ptr1.println("%s", tag);
                var ptr2 = ptr1.shift();
                for (var cpentry : cpentries) {
                    ptr2.println("%s", buffer.pool().stringValue(cpentry));
                }
            }
            return;            
        }
        
        int delta = switch (tag) {
            case early_larval_frame -> throw new AssertionError();
            case same_frame -> itag;
            case same_locals_1_stack_item_frame -> itag - 64;
            default -> buffer.nextUnsignedShort();
        };
        int offset = buffer.asCodeBuffer().addDelta(delta);
        if (detail) {
            ptr.println("@%d %s (%d)",
                    offset, tag.name(), itag);
        }        
        
        switch (tag) {
            case early_larval_frame -> throw new AssertionError();
            case same_frame, chop_frame, same_frame_extended -> {}
            case same_locals_1_stack_item_frame, same_locals_1_stack_item_frame_extended -> {
                checkVerificationTypeInfo(1);
            }
            case append_frame -> {
                checkVerificationTypeInfo(itag - 251);
            }
            case full_frame -> {
                int locals = buffer.nextUnsignedShort();
                checkVerificationTypeInfo(locals);
                int stack = buffer.nextUnsignedShort();
                checkVerificationTypeInfo(stack);
            }
        }
    }

    private void checkVerificationTypeInfo(int size) {
        var ptr1 = ptr.shift();
        for (int i = 0; i < size; ++i) {
            int tag = buffer.nextUnsignedByte();
            FrameType ft = FrameType.fromJVMType(tag);
            switch (ft) {
                case ft_Object -> {
                    var cpentry = buffer.nextCPEntry(ConstantPoolType.CONSTANT_Class);
                    if (detail) {
                        ptr1.println("%s %s", ft, buffer.pool().stringValue(cpentry));
                    }
                }
                case ft_Uninitialized -> {
                    int offset = buffer.asCodeBuffer().nextNewLabel();
                    if (detail) {
                        ptr1.println("%s @%d", ft, offset);
                    }
                }
                default -> {
                    assert ft.asmType() != null;
                    if (detail) {
                        ptr1.println("%s", ft);
                    }
                }
            }
        }
    }
}
