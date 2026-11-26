package io.jenkins.tools.pluginmodernizer.cli.nativeimage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

/**
 * Register the precompiled jte templates for reflection. jte loads them by name and renders them through
 * reflection, and a template is added with each recipe.
 */
public class JteTemplatesFeature implements Feature {

    private static final String PACKAGE_PATH = "gg/jte/generated/precompiled/";

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        for (Path classPathEntry : access.getApplicationClassPath()) {
            for (String path : entries(classPathEntry)) {
                if (path.startsWith(PACKAGE_PATH) && path.endsWith(".class")) {
                    String className =
                            path.substring(0, path.length() - ".class".length()).replace('/', '.');
                    Class<?> template = access.findClassByName(className);
                    if (template != null) {
                        RuntimeReflection.register(template);
                        RuntimeReflection.register(template.getDeclaredMethods());
                        RuntimeReflection.register(template.getFields());
                    }
                }
            }
        }
    }

    private static List<String> entries(Path classPathEntry) {
        try {
            if (Files.isDirectory(classPathEntry)) {
                try (Stream<Path> files = Files.walk(classPathEntry)) {
                    return files.map(file ->
                                    classPathEntry.relativize(file).toString().replace('\\', '/'))
                            .toList();
                }
            }
            if (Files.isRegularFile(classPathEntry) && classPathEntry.toString().endsWith(".jar")) {
                try (JarFile jar = new JarFile(classPathEntry.toFile())) {
                    return jar.stream().map(ZipEntry::getName).toList();
                }
            }
            return Collections.emptyList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
