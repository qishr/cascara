package io.github.qishr.cascara.command.casc;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

import io.github.qishr.cascara.command.casc.CascCommand.Get;
import io.github.qishr.cascara.common.content.ResourceContent;
import io.github.qishr.cascara.common.diagnostic.exception.LocalizableIOException;
import io.github.qishr.cascara.common.io.IOUtils;
import io.github.qishr.cascara.common.io.provider.ResResourceProvider;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.common.util.CommandLine;
import io.github.qishr.cascara.common.util.CommandLine.Command;
import io.github.qishr.cascara.common.util.CommandLine.Option;
import io.github.qishr.cascara.common.util.CommandLine.Parameters;
import io.github.qishr.cascara.common.util.JarManifest;
import io.github.qishr.cascara.common.util.JreUtils;
import io.github.qishr.cascara.common.util.UriScheme;

@Command(name = "casc",
        mixinStandardHelpOptions = true,
        versionProvider = CascCommand.VersionProvider.class,
        description = "`casc` command.",
        subcommands = {io.github.qishr.cascara.command.casc.CascCommand.Info.class, Get.class})
public class CascCommand implements Callable<Integer> {

    // Read properties at class initialization time
    private static final String COMMAND_VERSION_PROP = System.getProperty("command.version");
    private static final String CASCARA_VERSION_PROP = System.getProperty("cascara.version");

    // @Parameters(index = "0", description = "Verb.")
    // private String verb;

    @Option(names = {"-H"}, description = "Specify Cascara home directory")
    private String homePath;

    // @Option(names = {"-i", "--info"}, description = "Display Cascara Information")
    // private boolean info;

    @Command(name = "info", description = "Display Cascara Information",
        mixinStandardHelpOptions = true, version = "1.0")
    public static class Info implements Runnable {

        public Info() {}

        @Override
        public void run() {
            CascCommand.displayInfo();
        }
    }

    @Command(name = "get", description = "Retrieve a resource by its URI",
        mixinStandardHelpOptions = true, version = "1.0")
    public static class Get implements Callable<Integer> {
        // @Option(names = {"-x", "--times"}, description = "...")
        @Parameters(index = "0", description = "URI to retrieve")
        URI uri;

        public Get() {}

        @Override
        public Integer call() {
            try {
                ResourceContent res = IOUtils.getResource(uri);
                System.out.println(res.content());
            } catch (LocalizableIOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return 0;
        }
    }

    public static class VersionProvider implements CommandLine.IVersionProvider {
        // Public no-arg constructor required by Picocli factory / GraalVM reflection
        public VersionProvider() {}

        @Override
        public String[] getVersion() {
            return new String[] { "Cascara " + CascCommand.getCascaraVersion() };
        }
    }

    public CascCommand() {}

    public static void main(String... args) {
        int exitCode = execute(args);
        System.exit(exitCode);
    }

    public static int execute(String... args) {
        CommandLine cmd = new CommandLine(new CascCommand());
        cmd.setExecutionStrategy(parseResult -> {
            // 1. Run top-level global initialization
            CascCommand root = (CascCommand) parseResult.commandSpec().root().userObject();
            root.initGlobalState();

            // 2. Delegate to Picocli's standard execution strategy for subcommands
            return new CommandLine.RunLast().execute(parseResult);
        });
        return cmd.execute(args);
    }

    private void initGlobalState() {
        if (homePath != null) {
            Cascara.setHomePath(Path.of(homePath));
        }

        // Force SPL to register classes directly, avoiding GraalVM issues with moudleopath scanning
        SPL.getRoot().registerClass(io.github.qishr.cascara.common.content.type.ContentTypeStore.class);
        SPL.getRoot().registerClass(io.github.qishr.cascara.schema.util.SchemaResolverImpl.class);
        SPL.getRoot().registerClass(io.github.qishr.cascara.schema.util.CascaraSchemaResourceProvider.class);

        // The following call is neccessary when running in Graalvm.
        // Normally it happens automatically.
        IOUtils.setResourceProvider(
            UriScheme.RES,
            new ResResourceProvider(CascCommand.class)
        );
    }

    @Override
    public Integer call() throws LocalizableIOException {
        return 0;
    }

    public static String getCascaraVersion() {
        if (CASCARA_VERSION_PROP == null) {
            return Cascara.getVersion().toString();
        } else {
            return CASCARA_VERSION_PROP;
        }
    }

    public static String getCommandVersion() {
        if (COMMAND_VERSION_PROP == null) {
            JarManifest manifest;
            try {
                manifest = JarManifest.parse(JreUtils.getResourceAsString(CascCommand.class, "/META-INF/MANIFEST.MF"));
            } catch (IOException e) {
                return "0.0.0";
            }
            return manifest.getString("Implementation-Version", "0.0.0");
        } else {
            return COMMAND_VERSION_PROP;
        }
    }

    private static void displayInfo() {
        output("Command version: " + getCommandVersion());
        output("Cascara build version: " + getCascaraVersion());

        if (Cascara.getHomeEnvVar() != null) {
            output("Cascara home env var: " + Cascara.getHomeEnvVar());
        }

        output("Home: " + Cascara.getHomePath());
        output("Content types: " + Cascara.getContentTypesPath());
        output("Schemas: " + Cascara.getSchemasPath());

        output("Active installation: " + Cascara.getActiveVersionString());

        List<String> installedVersions = Cascara.getInstalledVersions();
        if (installedVersions.isEmpty()) {
            output("No installed versions");
        } else {
            output("Installed versions:");
            for (String v : installedVersions) {
                output("  " + v);
            }
        }
    }

    private static void output(Object o) {
        String text;
        if (o instanceof String s) {
            text = s;
        } else {
            text = Objects.toString(o);
        }
        System.out.println(text);
    }
}
