package com.github.david32768.jynxstructure;

import com.github.david32768.jynxfree.jvm.AttributeEntry;
import com.github.david32768.jynxfree.jvm.StandardAttribute;

public class SimpleAttribute extends AttributeInstance {

    public SimpleAttribute(StandardAttribute attr, AttributeBuffer buffer) {
        super(attr, buffer);
    }

    @Override
    public void checkCPEntries(IndentPrinter ptr) {
        int ct = itemCount();
        AttributeEntry[] entries = attr.entries();
        for (int i = 0; i < ct; ++i) {
            AttributeChecker.check(entries, buffer);
        }
    }

    private int itemCount() {
        return switch (attr.type()) {
            case FIXED -> 1;
            case ARRAY1 -> buffer.nextUnsignedByte();
            case ARRAY -> buffer.nextUnsignedShort();
            default -> throw new AssertionError();
        };
    }
    
}
