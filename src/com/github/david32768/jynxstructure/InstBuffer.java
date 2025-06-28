package com.github.david32768.jynxstructure;

import java.lang.classfile.Opcode;

import java.nio.ByteBuffer;

import static com.github.david32768.jynxfree.jynx.Global.LOG;
import static com.github.david32768.jynxfree.jynx.Global.OPTION;
import static com.github.david32768.jynxstructure.my.Message.M516;

import com.github.david32768.jynxfree.classfile.Opcodes;
import com.github.david32768.jynxfree.jvm.ConstantPoolType;
import com.github.david32768.jynxfree.jvm.NumType;
import com.github.david32768.jynxfree.jvm.OpArg;
import com.github.david32768.jynxfree.jvm.OpPart;
import com.github.david32768.jynxfree.jvm.StandardAttribute;
import com.github.david32768.jynxfree.jynx.GlobalOption;
import com.github.david32768.jynxfree.jynx.StringUtil;

public class InstBuffer extends AbstractCodeBuffer {
    
    public InstBuffer(ConstantPool pool, ByteBuffer bb, int maxlocal, int codesz) {
        super(pool, bb, maxlocal, codesz);
    }

    public CodeBuffer codeBuffer(ByteBuffer codebb) {
        labels.checkLabels();
        return new CodeBuffer(pool, codebb, StandardAttribute.Code.toString(), maxlocal, labels);
    }
    
    
    public int nextBranchLabel(int instoff) {
        return labels.labelOffset(instoff, nextInt());
    }
    
    public int nextIfLabel(int instoff) {
        return labels.labelOffset(instoff, nextShort());
    }
    
    public void setPosLabel(int instoff) {
        labels.setPosLabel(instoff);
    }
   
    public void align4(int offset) {
        int align = offset & 0x3;
        int padding = align == 0?0:4 - align;
        advance(padding );       
    }
    
    public void checkInsn(IndentPrinter ptr) {
        int start = position();
        while(hasRemaining()) {
            int instoff = position() - start;
            setPosLabel(instoff);
            int opcode = nextUnsignedByte();
            boolean wide = opcode == Opcodes.WIDE;
            Opcode op;
            if (wide) {
                opcode = nextUnsignedByte();
                op = Opcodes.widePrepended(opcode);
                
            } else {
                op = Opcodes.of(opcode);                
            }
            OpArg arg = OpArg.of(op);
            boolean print = OPTION(GlobalOption.DETAIL);
            switch(arg) {
                case arg_switch -> {
                    align4(instoff + 1);
                    int deflab = nextBranchLabel(instoff);
                    if (print) {
                        ptr.println("%5d:  %s default @%d .array", instoff, op.name().toLowerCase(), deflab);
                    }
                    boolean lookup = op == Opcode.LOOKUPSWITCH;
                    long low = lookup? 1: nextInt();
                    long high = lookup? nextSize(): nextInt();
                    if (!lookup && low > high) {
                        // "low %d must be less than or equal to high %d"
                        LOG(M516,low,high);
                    }
                    for (long i = low; i <= high; ++i) {
                        int value = lookup? nextInt(): (int)i;
                        int brlab = nextBranchLabel(instoff);
                        if (print) {
                            ptr.println("             %d -> @%d", value, brlab);
                        }
                    }
                    if (print) {
                        ptr.println("        .end_array");
                    }
                }
                default -> {
                    String extra = extra(op, arg, instoff);
                    if (print) {
                        ptr.println("%5d:  %s%s", instoff, op.name().toLowerCase(), extra);
                    }
                    assert start + instoff + op.sizeIfFixed() == position();
                }
            }
        }
    }
    
    public String extra(Opcode op, OpArg arg, int instoff) {
        String result = "";
        for (OpPart fmt:arg.getParts()) {
            String extra;
            switch(fmt) {
                case CP -> {
                    CPEntry cp;
                    if (op == Opcode.LDC) {
                        cp = nextCPEntryByte();
                    } else {
                        cp = nextCPEntry();
                    }
                    extra = stringValue(cp);
                    ConstantPoolType cpt = cp.getType();
                    if (cpt == ConstantPoolType.CONSTANT_String) {
                        extra = StringUtil.QuoteEscape(extra);
                    }
                    arg.checkCPType(cpt);
                }
                case LABEL -> {
                    int jmplab = Opcodes.isWide(op)?
                            nextBranchLabel(instoff):
                            nextIfLabel(instoff);
                    extra = "@" + Integer.toString(jmplab);
                }
                case VAR -> {
                    int var;
                    if (Opcodes.isImmediate(op)) {
                        var = Opcodes.numericSuffix(op);
                    } else if (Opcodes.isWide(op)) {
                        var = nextUnsignedShort();
                    } else {
                        var = nextUnsignedByte();
                    }
                    checkLocalVar(var);
                    extra = Opcodes.isImmediate(op)? "": Integer.toString(var);
                }
                case INCR -> {
                    int incr;
                    if (Opcodes.isWide(op)) {
                        incr = nextShort();
                    } else {
                        incr = nextByte();
                    }
                    extra = Integer.toString(incr);
                }
                case BYTE -> {
                    int b = nextByte();
                    extra = Integer.toString(b);
                }
                case SHORT -> {
                    int s = nextShort();
                    extra = Integer.toString(s);
                }
                case TYPE -> {
                    int t = nextUnsignedByte();
                    extra = NumType.getInstance(t).externalName();
                }
                case UBYTE -> {
                    int u = nextUnsignedByte();
                    extra = Integer.toString(u);
                }
                case ZERO -> {
                    int z = nextByte();
                    extra = "";
                }
                default -> throw new EnumConstantNotPresentException(fmt.getClass(), fmt.name());
            }
            if (!extra.isEmpty()) {
                result += " " + extra;
            }
        }
        return result;
    }
    
}
