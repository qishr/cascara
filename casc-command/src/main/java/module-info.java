module cascara.command.casc {
    requires transitive cascara.common;
    requires cascara.common.io;
    requires cascara.schema;
    requires info.picocli;

    exports io.github.qishr.cascara.command.casc to cascara.common;

    opens io.github.qishr.cascara.command.casc to cascara.common;
}
