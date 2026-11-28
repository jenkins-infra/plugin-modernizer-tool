package io.jenkins.plugins.testplugin;

import hudson.Extension;
import hudson.model.RootAction;

@Extension
public class TestAction implements RootAction {

    @Override
    public String getIconFileName() {
        return "document.png";
    }

    @Override
    public String getDisplayName() {
        return "Test Plugin";
    }

    @Override
    public String getUrlName() {
        return "test-plugin";
    }
}
