import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import javax.tools.ToolProvider;

/** Offline build: run with `java tools/Build.java build|test|run`. Requires JDK 17+. */
public class Build {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path CLASSES = ROOT.resolve("build/classes");
    private static final Path JAR = ROOT.resolve("dist/KingdomKing.jar");

    public static void main(String[] args) throws Exception {
        String command = args.length == 0 ? "build" : args[0];
        if (!List.of("build", "test", "run").contains(command)) {
            throw new IllegalArgumentException("Usage: java tools/Build.java [build|test|run] [game arguments]");
        }
        if (!Files.isDirectory(ROOT.resolve("src/game/rain"))) {
            throw new IllegalStateException("Run from the KingdomKing repository directory, or use build.sh/run.sh.");
        }
        build();
        if (command.equals("test")) {
            Path tests = ROOT.resolve("build/test-classes");
            clean(tests);
            compile(ROOT.resolve("tests"), tests, List.of("-classpath", JAR.toString()));
            runJava(List.of("-Djava.awt.headless=true", "-cp", tests + java.io.File.pathSeparator + JAR,
                    "game.rain.RegressionTests"));
            // A second JVM renders from the JAR alone, with no loose resource directory.
            runJava(List.of("-Djava.awt.headless=true", "-jar", JAR.toString(), "--screenshot",
                    ROOT.resolve("build/test-output/spawn.png").toString()));
        } else if (command.equals("run")) {
            List<String> gameArgs = new ArrayList<>(List.of("-jar", JAR.toString()));
            gameArgs.addAll(List.of(args).subList(1, args.length));
            runJava(gameArgs);
        }
    }

    private static void build() throws IOException {
        clean(CLASSES);
        compile(ROOT.resolve("src"), CLASSES, List.of());
        Files.createDirectories(JAR.getParent());
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "game.rain.Game");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(JAR), manifest)) {
            addFiles(jar, CLASSES, ".class");
            addFiles(jar, ROOT.resolve("res"), ".png");
            JarEntry license = new JarEntry("META-INF/LICENSE");
            jar.putNextEntry(license);
            Files.copy(ROOT.resolve("LICENSE"), jar);
            jar.closeEntry();
        }
        System.out.println("Built " + JAR);
    }

    private static void compile(Path source, Path output, List<String> extra) throws IOException {
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("A full JDK 17+ is required to build KingdomKing.");
        Files.createDirectories(output);
        List<String> options = new ArrayList<>(List.of("--release", "17", "-encoding", "UTF-8", "-Xlint:all", "-Werror",
                "-d", output.toString()));
        options.addAll(extra);
        try (var paths = Files.walk(source)) {
            paths.filter(p -> p.toString().endsWith(".java")).sorted().map(Path::toString).forEach(options::add);
        }
        if (compiler.run(null, null, null, options.toArray(String[]::new)) != 0) {
            throw new IllegalStateException("Compilation failed.");
        }
    }

    private static void addFiles(JarOutputStream jar, Path directory, String suffix) throws IOException {
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(suffix)).sorted().toList()) {
                JarEntry entry = new JarEntry(directory.relativize(path).toString().replace('\\', '/'));
                entry.setTime(0);
                jar.putNextEntry(entry);
                Files.copy(path, jar);
                jar.closeEntry();
            }
        }
    }

    private static void clean(Path directory) throws IOException {
        if (!Files.exists(directory)) return;
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
        }
    }

    private static void runJava(List<String> args) throws IOException, InterruptedException {
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", windows ? "java.exe" : "java").toString());
        command.addAll(args);
        int status = new ProcessBuilder(command).inheritIO().start().waitFor();
        if (status != 0) throw new IllegalStateException("Java process exited with status " + status);
    }
}
