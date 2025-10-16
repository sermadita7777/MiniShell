package minishell;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import tokenizer.MissingFileException;
import tokenizer.TCommand;
import tokenizer.TLine;
import tokenizer.Tokenizer;

public class MiniShell {
	private static final String prompt = "ms$";
	private File currentDirectory = new File(System.getProperty("user.dir"));
	private final String os; // variable que guardará el sistema operativo

	public MiniShell() {
		this.os = System.getProperty("os.name").toLowerCase();
	}

	public void run() {
		Scanner sc = new Scanner(System.in);

		while (true) {
			System.out.print("\u001B[36m" + this.prompt + "@>\u001B[0m ");

			if (!sc.hasNextLine()) {
				System.out.println();
				break;
			}

			String input = sc.nextLine().trim();

			if (input.isEmpty())
				continue;

			try {
				TLine line = Tokenizer.tokenize(input);

				if (line.getNcommands() > 0) {
					TCommand cmd = line.getCommands().get(0);
					String cmdName = cmd.getArgv().get(0);

					// Cmd exit.
					if (cmdName.equalsIgnoreCase("exit")) {
						System.out.println("Saliendo...");
						break;
						// Cmd cd
					} else if (cmdName.equalsIgnoreCase("cd")) {
						handleCd(cmd);

					} else {
						executeExternalCommand(line);
					}

				}

				System.out.println(line);
			} catch (MissingFileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		sc.close();
	}

	private void handleCd(TCommand cmd) {
		List<String> args = cmd.getArgv();

		if (args.size() < 2) {
			this.currentDirectory = new File(System.getProperty("user.home"));
		} else {
			File targetDir = new File(args.get(1));

			if (!targetDir.isAbsolute()) {
				targetDir = new File(this.currentDirectory, args.get(1));
			}

			if (targetDir.exists() && targetDir.isDirectory()) {
				this.currentDirectory = targetDir;
			} else {
				System.err.println("No existe el directorio: " + args.get(1));
			}
		}

		// Prueba
		System.out.println("Directorio actual: " + this.currentDirectory.getAbsolutePath());
	}

	private void executeExternalCommand(TLine line) {
		try {

			int n = line.getNcommands();

			List<ProcessBuilder> builders = new ArrayList<>();

			// Crear un PB para cada comando
			for (TCommand cmd : line.getCommands()) {

				// Filtar simbolos de redirección??
				List<String> cleanArgs = new ArrayList<>();

				for (String arg : cmd.getArgv()) {
					if (!arg.equals(">") && !arg.equals(">>") && !arg.equals("<") && !arg.equals("2>")
							&& !arg.equals("2>>")) {
						cleanArgs.add(arg);
					}
				}

				List<String> command = new ArrayList<>();
				// HACER CONDICIONES DE SISTEMA OPERATIVO
				if (this.os.contains("win")) {
					command.add("cmd.exe");
					command.add("/c");
				} else {
					command.add("/bin/sh");
					command.add("-c");
					command.add(String.join(" ", cleanArgs));
				}

				command.addAll(cleanArgs);

				ProcessBuilder pb = new ProcessBuilder(command);
				pb.directory(this.currentDirectory);
				pb.redirectErrorStream(false);
				builders.add(pb);

			}

			// Redirección de entrada (>)
			if (line.getRedirectInput() != null) {
				builders.get(0).redirectInput(new File(line.getRedirectInput()));
			}

			ProcessBuilder lst = builders.get(n - 1);

			// Redirección de salida (> o >>)
			if (line.getRedirectOutput() != null) {
				File outFile = new File(line.getRedirectOutput());
				if (line.isAppendOutput()) {
					lst.redirectOutput(ProcessBuilder.Redirect.appendTo(outFile));
				} else {
					lst.redirectOutput(ProcessBuilder.Redirect.to(outFile));
				}
			}

			// Redirección de error (2> o 2>>)
			if (line.getRedirectError() != null) {
				File errFile = new File(line.getRedirectError());
				if (line.isAppendError()) {
					lst.redirectError(ProcessBuilder.Redirect.appendTo(errFile));
				} else {
					lst.redirectError(ProcessBuilder.Redirect.to(errFile));
				}
			}

			// Ejecución con o sin pipes

			List<Process> processes = ProcessBuilder.startPipeline(builders);

			Process lstProcess = processes.get(processes.size() - 1);

			// Identificación de background o foreground
			if (line.isBackground()) {
				long PID = lstProcess.pid();
				System.out.println("Proceso [" + PID + "] ejecutándose en segundo plano...");
			} else {
				// salida estandar
				if (line.getRedirectOutput() == null) {
					try (BufferedReader br = new BufferedReader(new InputStreamReader(lstProcess.getInputStream()))) {
						String lineOut;
						while ((lineOut = br.readLine()) != null) {
							System.out.println(lineOut);
						}
						br.close();
					}
				}

				// salida error
				if (line.getRedirectError() == null) {
					try (BufferedReader br = new BufferedReader(new InputStreamReader(lstProcess.getErrorStream()))) {
						String lineErr;
						while ((lineErr = br.readLine()) != null) {
							System.err.println(lineErr);
						}
						br.close();
					}
				}
			}

			// Esperar que los procesos foreground terminen
			if (!line.isBackground()) {
				for (Process p : processes) {
					p.waitFor();
				}
			}

		} catch (IOException e) {
			// Limpieza visual en mensajes de error
			if (e.getLocalizedMessage().contains("CreateProcess error=2")) {
				System.err.println("Comando no encontrado o no válido,");
			} else {
				System.err.println("Error al ejecutar el comando: " + e.getMessage());
			}
		} catch (InterruptedException e) {
			System.err.println("Ejecución interrumpida");
			Thread.currentThread().interrupt();
		}
	}

	public static void main(String[] args) {
		new MiniShell().run();
	}
}
