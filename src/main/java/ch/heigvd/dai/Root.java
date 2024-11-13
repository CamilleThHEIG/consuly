package ch.heigvd.dai;

import picocli.CommandLine;

@CommandLine.Command(
        description = "The UDP application-you need everyone to agree on the same meeting point, or it's like herding cats with no Wi-Fi.",
        version = "1.0.0",
        subcommands = {
                Client.class,
                Server.class,
        },
        scope = CommandLine.ScopeType.INHERIT,
        mixinStandardHelpOptions = true)
public class Root {}