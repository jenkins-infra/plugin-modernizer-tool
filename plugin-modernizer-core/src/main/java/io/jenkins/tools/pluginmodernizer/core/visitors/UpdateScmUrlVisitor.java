package io.jenkins.tools.pluginmodernizer.core.visitors;

import org.openrewrite.ExecutionContext;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.List;

public class UpdateScmUrlVisitor extends MavenIsoVisitor<ExecutionContext> {

    @Override
    public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext context) {
        tag = super.visitTag(tag, context);
        if ("scm".equals(tag.getName())) {
            List<Content> contents = new ArrayList<>(tag.getContent());
            for (int i = 0; i < contents.size(); i++) {
                Content child = contents.get(i);
                if (child instanceof Xml.Tag) {
                    Xml.Tag childTag = (Xml.Tag) child;
                    System.out.println("Processing tag: " + childTag.getName());
                    if ("url".equals(childTag.getName()) && childTag.getValue().isPresent()) {
                        String value = childTag.getValue().get();
                        if (value.startsWith("git://")) {
                            contents.set(i, childTag.withValue(value.replace("git://", "https://")));
                        }
                    }
                    if ("connection".equals(childTag.getName()) && childTag.getValue().isPresent()) {
                        String value = childTag.getValue().get();
                        if (value.startsWith("scm:git:git://")) {
                            contents.set(i, childTag.withValue(value.replace("scm:git:git://", "scm:git:https://")));
                        }
                    }
                }
            }
            return tag.withContent(contents);
        }
        return tag;
    }
}

