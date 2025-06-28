module com.github.david32768.JynxStructure {
    requires com.github.david32768.JynxFree;
    exports com.github.david32768.jynxstructure;
    provides com.github.david32768.jynxfree.jynx.MainOptionService 
            with com.github.david32768.jynxstructure.MainStructure;
}
