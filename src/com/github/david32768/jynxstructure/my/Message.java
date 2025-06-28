package com.github.david32768.jynxstructure.my;

import static com.github.david32768.jynxfree.jynx.LogMsgType.*;

import com.github.david32768.jynxfree.jynx.JynxMessage;
import com.github.david32768.jynxfree.jynx.LogMsgType;

public enum Message implements JynxMessage {

    M500(ERROR,"magic number is %#x; should be %#x"),
    M501(ERROR,"label offset (%d) is negative or greater than code size (%d)"),
    M502(ERROR,"%s %s has %d extra bytes at end"),
    M503(ERROR,"startpc (%d) > endpc (%d)"),
    M504(ERROR,"offset %d is not an instruction"),
    M505(ERROR,"maximum bootcp used by constant pool (%d)  is greater than supplied in attribute (size = %d)"),
    M506(ERROR,"non-optional constant pool entry is missing; expected %s"),
    M507(ERROR,"boot argument %s is not loadable by %s"),
    M508(ERROR,"actual end(%#x) of %s does not match expected %#x"),
    M509(ERROR,"size (%#x) is greater than (%#x) remaining"),
    M510(ERROR,"tag %d not found"),
    M511(ERROR,"bad byte sequence %2x %2x"),
    M512(ERROR,"bad byte sequence %2x %2x %2x"),
    M513(ERROR,"bad byte sequence = %x"),
    M514(ERROR,"entry %d (%s) refers to index %d (%s) but expected to be in %s"),
    M515(ERROR,"number of cp entries (%d) for entry %d does not equal number required for type (5d)"),
    M516(ERROR,"low %d must be less than or equal to high %d"),
    M517(ERROR,"duplicate attribute %s in contexr %s"),
    M518(ERROR,"local variable %d is >= max locals %d"),
    M519(ERROR,"cpentry type %s is invalid for %s"),
    M520(ERROR,"CP index %d is not in [1,%d]"),
    M521(ERROR,"invalid tag %d"),
    M522(ERROR,"CPIndex %d is invalid as points to middle of %s entry"),
    M523(ERROR,"typeref %s (%#x) not valid in context %s"),
    M524(ERROR,"branches to middle of instruction - %s"),
    M526(ERROR,"type_path_kind = %d is not in range [0,3]"),
    M527(ERROR,"type_argument_index is %d but must be 0 for type_path_kind [0,2]"),
    ;

    private final LogMsgType logtype;
    private final String format;
    private final String msg;

    private Message(LogMsgType logtype, String format) {
        this.logtype = logtype;
        this.format = logtype.prefix(name()) + format;
        this.msg = format;
    }
    
    
    private Message(String format) {
        this(ERROR,format);
    }

    @Override
    public String format(Object... objs) {
        return String.format(format,objs);
    }

    @Override
    public String getFormat() {
        return msg;
    }
    
    @Override
    public LogMsgType getLogtype() {
        return logtype;
    }
    
}
