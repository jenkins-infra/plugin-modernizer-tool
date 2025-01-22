package io.jenkins.tools.pluginmodernizer.core.visitors;

import java.util.ArrayList;
import java.util.List;
import org.openrewrite.ExecutionContext;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateScmUrlVisitor extends MavenIsoVisitor<ExecutionContext> {

    private static final Logger Log = LoggerFactory.getLogger(UpdateScmUrlVisitor.class);

    @Override
    public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
        tag = super.visitTag(tag, ctx);

        if (!"scm".equals(tag.getName())) {
            return tag;
        }

        boolean changed = false;
        List<Content> contents = tag.getContent() != null ? new ArrayList<>(tag.getContent()) : new ArrayList<>();

        for (int i = 0; i < contents.size(); i++) {
            if (!(contents.get(i) instanceof Xml.Tag)) {
                continue;
            }

            Xml.Tag childTag = (Xml.Tag) contents.get(i);
            String updatedValue = null;

            if (childTag.getValue().isPresent()) {
                String value = childTag.getValue().get();
                if ("connection".equals(childTag.getName()) && value.startsWith("scm:git:git://")) {
                    Log.info("Updating SCM connection from 'scm:git:git:' to 'scm:git:https:'");
                    updatedValue = value.replace("scm:git:git://", "scm:git:https://");
                }
                if ("connection".equals(childTag.getName()) && value.startsWith("scm:git:ssh://")) {
                    Log.info("Updating SCM connection from 'scm:git:ssh:' to 'scm:git:https:'");
                    updatedValue = value.replace("scm:git:ssh://", "scm:git:https://");
                }
            }

            if (updatedValue != null) {
                contents.set(i, childTag.withValue(updatedValue));
                changed = true;
            }
        }

        return changed ? tag.withContent(contents) : tag;
    }
}
