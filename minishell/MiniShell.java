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

/**
 * @author Sergio Juanas Santamaría
 * @version 10 
 * @date 22/10/2025
 * 
 * Repositorio del proyecto:
 * https://github.com/sermadita7777/MiniShell
 * 
 */

public class MiniShell {

	/** Prompt mostrado al usuario */
	
	private static final String prompt = "ms$";

	/** Directorio actual de trabajo -> modificable con el comando 'cd'	*/

	private File currentDirectory = new File(System.getProperty("user.dir"));

	/** Nombre del sistema operativo -> permite compatibilidad multiplataforma (Linux/Windows) */

	private final String os;

	public MiniShell() {
		this.os = System.getProperty("os.name").toLowerCase();
	}

	/**
	 * Búcle principal.
	 * 
	 * Por cada iteración ejecuta la lectura de comandos y muestra el prompt.
	 * - Ejecuta los comandos "tokenizando" la entrada con la clase Tokenizer.
	 * - Ejecuta comandos internos o delega al método executeExternalCommand().
	 */

	public void run() {
		Scanner sc = new Scanner(System.in);

		while (true) {
			System.out.print("\u001B[36m" + prompt + "@>\u001B[0m ");

			String input = sc.nextLine().trim();

			// Si la línea está vacía (el usuario solo pulsó Enter), vuelve a mostrarse el prompt.
			if (input.isEmpty()) {
				continue;
			}
			
			try {

				// Convierte la entrada del usuario en comandos y argumentos.
				
				TLine line = Tokenizer.tokenize(input);

				if (line.getNcommands() > 0) {

					// Se crea un objeto TCommand en el primer comando para ver si se trata de uno interno.
					
					TCommand cmd = line.getCommands().get(0);

					// Se guarda el primer argumento (nombre) del comando.
					String cmdName = cmd.getArgv().get(0);
					
					/**
					 * COMANDO INTERNO: exit Usado para finalizar el bucle y la aplicación.
					 */

					if (cmdName.equalsIgnoreCase("exit")) {
						System.out.println("Saliendo...");
						break;

						/**
						 * COMANDO INTERNO: cd Usado para el cambio de directorios mediante rutas
						 * absolutas o relativas.
						 */

					} else if (cmdName.equalsIgnoreCase("cd")) {
						handleCd(cmd);

						/**
						 * COMANDOS EXTERNOS 
						 * Este método se encargará de crear procesos, unir pipes y aplicar las redirecciones.
						 */
						
					} else {
						executeExternalCommand(line);
					}

				}

			} catch (MissingFileException e) {
				System.err.println("Debes introducir un archivo para aplicar la redirección: "+e.getMessage());
			}
		}

		sc.close();
	}

	/**
	 * Cambia el directorio actual de trabajo.
	 * 
	 * Funciones/Comportamientos principales: 
	 * - Si no se pasa argumento ( solo "cd" ), se va al directorio home del usuario.
	 * - Si se pasa una ruta relativa, se interpreta respecto al directorio actual.
	 * - Se normaliza la ruta con getCanonicalFile() para resolver '..' o '.' .
	 * 		Subida de nivel o mostrar el directorio actual.
	 * - Si la ruta no existe o no es un directorio, se informa al usuario.
	 * 
	 * @param cmd
	 */
	
	private void handleCd(TCommand cmd) {
		
		//Obtenemos los argumentos del comando.
		List<String> args = cmd.getArgv();

		if (args.size() < 2) {
			this.currentDirectory = new File(System.getProperty("user.home"));
		} else {
			
			// Se toma el primer argumento como ruta objetivo.
			String pathArg = args.get(1);
			
			// El programa crea un archivo con esa ruta tal cual (bien relativa o absoluta)
			
			File targetDir = new File(pathArg);

			// Si la ruta no es absoluta, se interpreta como relativa en el directorio actual.
			
			if (!targetDir.isAbsolute()) {
				targetDir = new File(this.currentDirectory, pathArg);
			}

			try {				
				targetDir = targetDir.getCanonicalFile();
			} catch (IOException e) {
				System.err.println("Error al acceder al directorio: " + e.getMessage());
			}

			// Actualizar el directorio actual o mostrar error.
			
			if (targetDir.exists() && targetDir.isDirectory()) {
				this.currentDirectory = targetDir;
			} else {
				System.err.println("No existe el directorio: " + pathArg);
			}
		}

		System.out.println("Directorio actual: " + this.currentDirectory.getAbsolutePath());
	}
	
	/**
	 * Método para ejecutar comandos externos.
	 * 
	 * Funciones/Comportamientos principales: 
	 * - Recibe un TLine que puede contener una secuencia de comandos (pipe)
	 * y redirecciones.
	 * - Crea un ProcessBuilder para cada comando en la línea.
	 * - Aplica redirecciones de entrada/salida/error.
	 * - Ejecuta la pipeline usando ProcessBuilder.startPipeLine() 
	 * 		Permite conectarlas automaticamente.
	 * - Si la línea termina con '&' (background), muestra únicamente su PID.
	 * - Si está en foreground, muestra la salida/error del último proceso y espera.
	 * 
	 * @param line
	 */

	private void executeExternalCommand(TLine line) {
		try {

			int n = line.getNcommands();
			
			List<ProcessBuilder> builders = new ArrayList<>();

			for (TCommand cmd : line.getCommands()) {

				/**
				 * TLine ofrece de base la representación de las redirecciones,
				 * por lo que se "limpian" los argumentos filtrando los símbolos
				 *  de redireccón y añadiendo los "limpios" a una nueva lista.
				 */
				
				List<String> cleanArgs = new ArrayList<>();

				for (String arg : cmd.getArgv()) {
					if (!arg.equals(">") && !arg.equals(">>") && !arg.equals("<") && !arg.equals("2>")
							&& !arg.equals("2>>")) {
						cleanArgs.add(arg);
					}
				}
				
				/**
				 * VERIFICACIÓN SISTEMA OPERATIVO:
				 * Se crea una nueva lista que recogerá la "formación" de los comandos 
				 * en base al S.O. correspondiente. Una vez rellenada se le pasará 
				 * a un nuevo ProcessBuilder. 
				 */

				List<String> command = new ArrayList<>();

				if (this.os.contains("win")) {

					// Configuración específica para windows, 
					// pues al contrario de linux no permite poner el comando por si solo.
					
					command.add("cmd.exe");
					command.add("/c");
					
					command.add(String.join(" ", cleanArgs));
				} else {
					
					/**
					 * En linux la línea de comandos está formada por:
					 * "/bin/sh -c <cadena de comandos>"
					 * No hace falta ponerlo pues se interpreta automático.
					 */			
				
					command.addAll(cleanArgs);
				}

				ProcessBuilder pb = new ProcessBuilder(command);
				
				pb.directory(this.currentDirectory);
				pb.redirectErrorStream(false);				
				builders.add(pb);

			}

			/**
			 * CONFIGURACIÓN DE REDIRECCIONES: 
			 */
			
			//Redirección de entrada (<): se aplica solo al primer comando.
			if (line.getRedirectInput() != null) {
				
				File inFile = new File(line.getRedirectInput());
				
				if(!inFile.exists() || !inFile.canRead()){
					System.err.println("Error: archivo de entrada no encontrado o no legible: "+inFile);
					return;
				}
				builders.get(0).redirectInput(inFile);
			}

			//Por otro lado las redirecciones de salida y error serán aplicadas al último.
			ProcessBuilder lst = builders.get(n - 1);

			// Redirección de salida (> o >>)
			if (line.getRedirectOutput() != null) {
				File outFile = new File(line.getRedirectOutput());
					
				if(outFile.exists() && !outFile.canWrite()) {
					System.err.println("Error: no se puede escribir en el archivo de salida: "+outFile);
					return;
				}					
				
				if (line.isAppendOutput()) {
					lst.redirectOutput(ProcessBuilder.Redirect.appendTo(outFile));
				} else {
					lst.redirectOutput(ProcessBuilder.Redirect.to(outFile));
				}
			}

			// Redirección de error (2> o 2>>)
			if (line.getRedirectError() != null) {
				File errFile = new File(line.getRedirectError());
				
				if (errFile.exists() && !errFile.canWrite()) {
					System.err.println("Error: no se puede escribir en el archivo de error: "+errFile);
					return;
				} 
				
				if (line.isAppendError()) {
					lst.redirectError(ProcessBuilder.Redirect.appendTo(errFile));
				} else {
					lst.redirectError(ProcessBuilder.Redirect.to(errFile));
				}
			}

			/**
			 * EJECUCIÓN CON PIPES:
			 * Conexión automática entre procesos (stdout -> stin).
			 */

			List<Process> processes = ProcessBuilder.startPipeline(builders);

			//Se obtiene el último proceso para recoger su PID o stdout/err
			Process lstProcess = processes.get(processes.size() - 1);

		
			/**
			 * BACKGROUND VS FOREGROUND:
			 * Si la línea termina con '&' se ejecuta en segund plano.
			 * - No se espera a que termine el proceso, solo se muestra el PID
			 */
			
			if (line.isBackground()) {
				long PID = lstProcess.pid();
				System.out.println("Proceso [" + PID + "] ejecutándose en segundo plano...");
			} else {
				
				//Foreground -> se muestra la salida y espera a que terminen.
				
				//Mostrar la salida estándar (stdout) del último proceso si no es redirección.
				if (line.getRedirectOutput() == null) {
					try (BufferedReader br = new BufferedReader(new InputStreamReader(lstProcess.getInputStream()))) {
						String lineOut;
						while ((lineOut = br.readLine()) != null) {
							System.out.println(lineOut);
						}
					}
				}

				// Mostrar la salida de errores (stderr) de todos los procesos si no se redirecciona.
				// Útil para identificar fallos a mitad de pipe.
				if (line.getRedirectError() == null) {
					for(Process p: processes) {
						try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
							String lineErr;
							while ((lineErr = br.readLine()) != null) {
								System.err.println(lineErr);
							}
						}	
					}
				}
			}

			/**
			 * ESPERAR FINALIZACIÓN:
			 * En caso de que el proceso no sea background, se espera a que 
			 * todos los procesos terminen.
			 */
			
			if (!line.isBackground()) {
				for (Process p : processes) {
					p.waitFor();
				}
			}

		} catch (IOException e) {
			
			// Limpieza visual en mensajes de error
			
			String msg=e.getMessage();
			
			if(msg.contains("No such file") || msg.contains("cannot find the file")) {
				System.err.println("Archivo no encontrado: verifica la ruta.");
				
			} else if (msg.contains("Access is denied") || msg.contains("Permission denied")) {
				System.err.println("Permiso denegado: no se puede acceder al archivo o directorio.");
				  
			} else {
				System.err.println("Error inesperado: "+ msg);
			}
		} catch (InterruptedException e) {
			System.err.println("Ejecución interrumpida");
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Método main:
	 * 
	 * Crea una instancia de la minishell y ejecuta el bucle run().
	 */
	
	public static void main(String[] args) {
		new MiniShell().run();
	}
}
