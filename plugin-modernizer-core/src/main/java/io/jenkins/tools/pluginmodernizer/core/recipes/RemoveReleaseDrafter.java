package io.jenkins.tools.pluginmodernizer.core.recipes;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import java.util.concurrent.atomic.AtomicBoolean;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Remove release drafter if CD is in place (overlap of workflow)
 */
@SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "No user input")
public class RemoveReleaseDrafter extends ScanningRecipe<AtomicBoolean> {

    /**
     * LOGGER.
     */
    private static final Logger LOG = LoggerFactory.getLogger(RemoveReleaseDrafter.class);

    @Override
    public String getDisplayName() {
        return "Remove release drafter if CD is in place";
    }

    @Override
    public String getDescription() {
        return "Remove the release drafter file if CD is in place.";
    }

    @Override
    public AtomicBoolean getInitialValue(ExecutionContext ctx) {
        return new AtomicBoolean(false);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean shouldRemove) {
        return new TreeVisitor<>() {

            @Override
            public Tree visit(Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) tree;
                if (ArchetypeCommonFile.WORKFLOW_CD.same(sourceFile.getSourcePath())) {
                    LOG.info("Project is using CD. Need to remove release drafter.");
                    shouldRemove.set(true);
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AtomicBoolean shouldRemove) {
        return new TreeVisitor<>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext ctx) {
                LOG.info("Checking if release drafter should be removed");
                if (shouldRemove.get() && tree instanceof SourceFile sourceFile) {
                    if (ArchetypeCommonFile.RELEASE_DRAFTER.same(sourceFile.getSourcePath())) {
                        LOG.info("Deleting release drafter file: {}", sourceFile.getSourcePath());
                        return null;
                    }
                    if (ArchetypeCommonFile.RELEASE_DRAFTER_WORKFLOW.same(sourceFile.getSourcePath())) {
                        LOG.info("Deleting release drafter workflow file: {}", sourceFile.getSourcePath());
                        return null;
                    }
                }
                return tree;
            }
        };
    }
}
