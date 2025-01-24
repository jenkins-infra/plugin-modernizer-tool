package io.jenkins.tools.pluginmodernizer.core.recipes;

import io.jenkins.tools.pluginmodernizer.core.extractor.MetadataExecutionContext;
import io.jenkins.tools.pluginmodernizer.core.extractor.MetadataFinalizerVisitor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;

/**
 * Metadata finalizer. Executed after FetchMetadata to finalize the metadata and store it.
 */
public class MetadataFinalizer extends Recipe {

    @Override
    public String getDisplayName() {
        return "Metadata finalizer";
    }

    @Override
    public String getDescription() {
        return "Metadata finalizer";
    }

    /**
     * Metadata context.
     */
    private final MetadataExecutionContext metadataContext;

    public MetadataFinalizer(MetadataExecutionContext metadataContext) {
        this.metadataContext = metadataContext;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext ctx) {
                return new MetadataFinalizerVisitor().visit(tree, metadataContext);
            }
        };
    }
}
