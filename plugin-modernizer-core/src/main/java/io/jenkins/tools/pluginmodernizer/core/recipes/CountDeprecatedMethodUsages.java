package io.jenkins.tools.pluginmodernizer.core.recipes;

import java.util.concurrent.atomic.AtomicInteger;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.search.FindDeprecatedMethods;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CountDeprecatedMethodUsages extends Recipe {

    private static final Logger LOG = LoggerFactory.getLogger(CountDeprecatedMethodUsages.class);

    private final AtomicInteger totalDeprecatedCount = new AtomicInteger();

    public int getTotalDeprecatedCount() {
        return totalDeprecatedCount.get();
    }

    @Override
    public String getDisplayName() {
        return "Count the usages of deprecated methods";
    }

    @Override
    public String getDescription() {
        return "Get the total number of deprecated method calls found.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        // FindDeprecatedMethods recipe with full wildcard pattern
        Recipe findDeprecated = new FindDeprecatedMethods("*..* *(..)", false);

        return new TreeVisitor<>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext ctx) {
                // Apply the FindDeprecatedMethods visitor
                Tree result = findDeprecated.getVisitor().visit(tree, ctx);

                AtomicInteger perFileCount = new AtomicInteger();

                new TreeVisitor<Tree, ExecutionContext>() {
                    @Override
                    public Tree visit(Tree innerTree, ExecutionContext innerCtx) {
                        if (innerTree instanceof J
                                && (innerTree)
                                        .getMarkers()
                                        .findFirst(SearchResult.class)
                                        .isPresent()) {
                            perFileCount.incrementAndGet();
                        }
                        return super.visit(innerTree, innerCtx);
                    }
                }.visit(result, ctx);

                totalDeprecatedCount.addAndGet(perFileCount.get());
                return result;
            }
        };
    }
}
