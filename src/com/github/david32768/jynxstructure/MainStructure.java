package com.github.david32768.jynxstructure;

import java.io.PrintWriter;

import com.github.david32768.jynxfree.jynx.MainOption;
import com.github.david32768.jynxfree.jynx.MainOptionService;

public class MainStructure implements MainOptionService {

    @Override
    public MainOption main() {
        return MainOption.STRUCTURE;
    }


    @Override
    public boolean call(PrintWriter pw, String fname) {
        return Structure.printClassStructure(fname,pw);
    }
        
}
