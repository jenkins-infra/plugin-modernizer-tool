package io.jenkins.tools.pluginmodernizer.cli.nativeimage;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.CommitConfig;
import org.eclipse.jgit.lib.CoreConfig;
import org.eclipse.jgit.lib.GcConfig;
import org.eclipse.jgit.lib.GpgConfig;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.ObjectChecker;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.SignatureVerifier;
import org.eclipse.jgit.lib.SubmoduleConfig;
import org.eclipse.jgit.transport.HttpConfig;

/**
 * JGit reads its configuration enums and translation fields by reflection.
 * Same list as the Quarkiverse JGit extension, which is not used because it brings the JSch transport.
 */
@RegisterForReflection(
        targets = {
            BranchConfig.BranchRebaseMode.class,
            CommitConfig.CleanupMode.class,
            CoreConfig.AutoCRLF.class,
            CoreConfig.CheckStat.class,
            CoreConfig.EOL.class,
            CoreConfig.EolStreamType.class,
            CoreConfig.HideDotFiles.class,
            CoreConfig.LogRefUpdates.class,
            CoreConfig.SymLinks.class,
            CoreConfig.TrustStat.class,
            DiffAlgorithm.SupportedAlgorithm.class,
            DirCache.DirCacheVersion.class,
            GcConfig.PackRefsMode.class,
            GpgConfig.GpgFormat.class,
            HttpConfig.HttpRedirectMode.class,
            IndexDiff.StageState.class,
            JGitText.class,
            ObjectChecker.ErrorType.class,
            MergeCommand.ConflictStyle.class,
            MergeCommand.FastForwardMode.Merge.class,
            MergeCommand.FastForwardMode.class,
            Ref.Storage.class,
            RefUpdate.Result.class,
            SignatureVerifier.TrustLevel.class,
            SubmoduleConfig.FetchRecurseSubmodulesMode.class
        })
public class JGitReflectionConfiguration {}
