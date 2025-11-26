package com.spinque.ciff.app;

import picocli.CommandLine;

@CommandLine.Command(name="ciff-cli",
		description = "Tools for working with CIFF indices",
		mixinStandardHelpOptions = true,
		subcommands = {
				ImportCiffFile.class,
				ExportCiffFile.class,
				QueryCiff.class,
		}
)
public class Launcher {

	public static void main(String... args) {
		CommandLine cl = new CommandLine(new Launcher());
		cl.setCaseInsensitiveEnumValuesAllowed(true);
		int exitCode = 	cl.execute(args);
		System.exit(exitCode);
	}
}
