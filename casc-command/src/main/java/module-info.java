module cascara.command.casc {
    requires transitive cascara.common;
    requires cascara.common.io;

    exports io.github.qishr.cascara.command.casc to cascara.common;

    opens io.github.qishr.cascara.command.casc to cascara.common;
}
