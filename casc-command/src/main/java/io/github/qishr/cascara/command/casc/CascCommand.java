// License & Terms
//
// This file is part of **Cascara CT**.
//
// **Cascara CT** is free software: you can redistribute
// it and/or modify them without restriction under the terms of
// the MIT License.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// MIT License for more details.

package io.github.qishr.cascara.command.casc;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

import io.github.qishr.cascara.common.diagnostic.LocalizableIOException;
import io.github.qishr.cascara.common.io.IOUtils;
import io.github.qishr.cascara.common.io.provider.ResResourceProvider;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.common.util.CommandLine;
import io.github.qishr.cascara.common.util.CommandLine.Command;
import io.github.qishr.cascara.common.util.CommandLine.Option;
import io.github.qishr.cascara.common.util.JarManifest;
import io.github.qishr.cascara.common.util.JreUtils;
import io.github.qishr.cascara.common.util.UriScheme;

@Command(name = "casc",
        mixinStandardHelpOptions = true,
        versionProvider = CascCommand.VersionProvider.class,
        description = "`casc` command.")
public class CascCommand implements Callable<Integer> {

    // Read properties at class initialization time
    private static final String COMMAND_VERSION_PROP = System.getProperty("command.version");
    private static final String CASCARA_VERSION_PROP = System.getProperty("cascara.version");

    // @Parameters(index = "0", description = "The CT script to execute.")
    // private String inFile;

    @Option(names = {"-H"}, description = "Specify Cascara home directory")
    private String homePath;

    @Option(names = {"-i", "--info"}, description = "Display Cascara Information")
    private boolean info;

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
        return new CommandLine(new CascCommand()).execute(args);
    }

    @Override
    public Integer call() throws LocalizableIOException {
        if (homePath != null) {
            Cascara.setHomePath(Path.of(homePath));
        }

        // The following call is neccessary when running in Graalvm.
        // Normally it happens automatically.
        IOUtils.setResourceProvider(UriScheme.RES, new ResResourceProvider(CascCommand.class));

        if (info) {
            displayInfo();
        }

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

    private void displayInfo() {
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

    private void output(Object o) {
        String text;
        if (o instanceof String s) {
            text = s;
        } else {
            text = Objects.toString(o);
        }
        System.out.println(text);
    }
}
