package io.jenkins.tools.pluginmodernizer.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class JdkFetcherTest {

    private static final int MODE_FILE_644 = 0100644;
    private static final int MODE_FILE_755 = 0100755;

    @TempDir
    private Path tempDir;

    @Test
    public void extractTarGzPreservesExecutableModeOnJspawnhelper() throws Exception {
        assumePosix();

        Path tarGz = tempDir.resolve("jdk.tar.gz");
        writeJdkTarGz(tarGz, MODE_FILE_755, MODE_FILE_644);
        assertArchiveEntryHasExecuteBit(tarGz, "jspawnhelper");

        Path extractionDir = tempDir.resolve("extracted");
        Files.createDirectories(extractionDir);

        invokeExtractTarGz(tarGz, extractionDir);

        Path jspawnhelper = extractionDir.resolve("lib").resolve("jspawnhelper");
        Path modules = extractionDir.resolve("lib").resolve("modules");

        assertTrue(Files.isRegularFile(jspawnhelper), "jspawnhelper should be extracted");
        assertTrue(Files.isRegularFile(modules), "modules should be extracted");

        Set<PosixFilePermission> helperPerms = Files.getPosixFilePermissions(jspawnhelper);
        assertTrue(
                helperPerms.contains(PosixFilePermission.OWNER_EXECUTE),
                "extractTarGz must preserve the tar entry +x bit on lib/jspawnhelper; after the old extract path this file was 644");
        assertTrue(helperPerms.contains(PosixFilePermission.OWNER_READ));
        assertTrue(helperPerms.contains(PosixFilePermission.GROUP_EXECUTE));
        assertTrue(helperPerms.contains(PosixFilePermission.OTHERS_EXECUTE));

        Set<PosixFilePermission> modulesPerms = Files.getPosixFilePermissions(modules);
        assertTrue(modulesPerms.contains(PosixFilePermission.OWNER_READ));
        assertFalse(
                modulesPerms.contains(PosixFilePermission.OWNER_EXECUTE),
                "non-executable archive entries must stay non-executable after extract");
    }

    @Test
    public void setJavaBinariesPermissionsMakesJspawnhelperExecutable() throws Exception {
        assumePosix();

        Path jdkPath = tempDir.resolve("jdk");
        Path javaBin = jdkPath.resolve("bin").resolve("java");
        Path jspawnhelper = jdkPath.resolve("lib").resolve("jspawnhelper");
        Files.createDirectories(javaBin.getParent());
        Files.createDirectories(jspawnhelper.getParent());
        Files.writeString(javaBin, "java");
        Files.writeString(jspawnhelper, "helper");

        Set<PosixFilePermission> readOnly =
                Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ);
        Files.setPosixFilePermissions(javaBin, readOnly);
        Files.setPosixFilePermissions(jspawnhelper, readOnly);
        assertFalse(Files.getPosixFilePermissions(jspawnhelper).contains(PosixFilePermission.OWNER_EXECUTE));

        invokeSetJavaBinariesPermissions(jdkPath);

        assertTrue(
                Files.getPosixFilePermissions(javaBin).contains(PosixFilePermission.OWNER_EXECUTE),
                "bin/java should be executable");
        assertTrue(
                Files.getPosixFilePermissions(jspawnhelper).contains(PosixFilePermission.OWNER_EXECUTE),
                "lib/jspawnhelper must be chmod'd even when extract dropped the tar mode");
        assertEquals("rwxr-xr-x", toRwx(Files.getPosixFilePermissions(jspawnhelper)));
    }

    private static void assumePosix() {
        assumeTrue(
                FileSystems.getDefault().supportedFileAttributeViews().contains("posix"),
                "POSIX file permissions are required");
    }

    private static void assertArchiveEntryHasExecuteBit(Path tarGz, String entrySuffix) throws IOException {
        try (InputStream in = Files.newInputStream(tarGz);
                GZIPInputStream gzip = new GZIPInputStream(in);
                TarArchiveInputStream tar = new TarArchiveInputStream(gzip)) {
            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                if (entry.getName().endsWith(entrySuffix)) {
                    assertTrue(
                            (entry.getMode() & 0100) != 0,
                            "Adoptium tar stores " + entrySuffix + " with +x; getMode() was " + entry.getMode());
                    return;
                }
            }
        }
        throw new AssertionError("Archive did not contain " + entrySuffix);
    }

    private static void writeJdkTarGz(Path tarGz, int jspawnhelperMode, int modulesMode) throws IOException {
        try (OutputStream fos = Files.newOutputStream(tarGz);
                GZIPOutputStream gzip = new GZIPOutputStream(fos);
                TarArchiveOutputStream tar = new TarArchiveOutputStream(gzip)) {
            putFile(tar, "jdk-17/lib/jspawnhelper", "spawn-helper", jspawnhelperMode);
            putFile(tar, "jdk-17/lib/modules", "modules-data", modulesMode);
        }
    }

    private static void putFile(TarArchiveOutputStream tar, String name, String content, int mode) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setSize(bytes.length);
        entry.setMode(mode);
        tar.putArchiveEntry(entry);
        tar.write(bytes);
        tar.closeArchiveEntry();
    }

    private void invokeExtractTarGz(Path tarGz, Path extractionDir) throws Exception {
        invokePrivate("extractTarGz", tarGz, extractionDir);
    }

    private void invokeSetJavaBinariesPermissions(Path jdkPath) throws Exception {
        invokePrivate("setJavaBinariesPermissions", jdkPath);
    }

    private void invokePrivate(String methodName, Object... args) throws Exception {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = Path.class;
        }
        Method method = JdkFetcher.class.getDeclaredMethod(methodName, types);
        method.setAccessible(true);
        JdkFetcher fetcher = new JdkFetcher(tempDir);
        try {
            method.invoke(fetcher, args);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception ex) {
                throw ex;
            }
            throw e;
        }
    }

    private static String toRwx(Set<PosixFilePermission> permissions) {
        return PosixFilePermissions.toString(permissions);
    }
}
