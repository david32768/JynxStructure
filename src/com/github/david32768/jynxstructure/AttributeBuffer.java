package com.github.david32768.jynxstructure;

import java.nio.ByteBuffer;

import com.github.david32768.jynxfree.jvm.Context;

public class AttributeBuffer extends Buffer {

    private final Context context;
    private final String name;

    public AttributeBuffer(ConstantPool pool, ByteBuffer bb, Context context, String name) {
        super(pool, bb);
        this.context = context;
        this.name = name;
    }

    public Context context() {
        return context;
    }

    public String name() {
        return name;
    }
        
    public int level() {
        return switch (context) {
            case COMPONENT, CODE -> 2;
            case CLASS -> 0;
            default -> 1;
        };
    }
    
    public CodeBuffer codeBuffer(IndentPrinter ptr) {
        assert context == Context.METHOD;
        nextShort(); // max stack
        int maxlocals = nextShort(); //max locals
        int codesz = nextSize(); // code length
        ptr.println("; code size = %#x", codesz);
        InstBuffer instbuff = new InstBuffer(pool, extract(codesz), maxlocals, codesz);
        instbuff.checkInsn(ptr.shift());
        CodeBuffer result = instbuff.codeBuffer(bb.duplicate());
        bb.position(bb.limit());
        return result;
    }
    
}
