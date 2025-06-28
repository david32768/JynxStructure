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
            AttributeChecker.check(entries, ptr, buffer);
        }
    }

    private int itemCount() {
        int ct;
        switch (attr.type()) {
            case FIXED:
                ct = 1;
                break;
            case ARRAY1:
                ct = buffer.nextUnsignedByte();
                break;
            case ARRAY:
                ct = buffer.nextUnsignedShort();
                break;
            default:
                throw new AssertionError();
        }
        return ct;
    }
    
}
