package org.optaplanner.examples.cloudbalancing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

public final class DrlBasedKieBase implements KieSessionSupplier {

    private final KieContainer cache;

    public DrlBasedKieBase(final String drlResourceName) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(drlResourceName)))) {
            String rule = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            cache = getKieContainer(null, rule);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String getPom(ReleaseId releaseId) {
        String pom =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                        "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
                        "  <modelVersion>4.0.0</modelVersion>\n" +
                        "\n" +
                        "  <groupId>" + releaseId.getGroupId() + "</groupId>\n" +
                        "  <artifactId>" + releaseId.getArtifactId() + "</artifactId>\n" +
                        "  <version>" + releaseId.getVersion() + "</version>\n" +
                        "</project>";
        return pom;
    }

    @Override
    public KieSession get() {
        return cache.newKieSession();
    }

    protected KieContainer getKieContainer(KieModuleModel model, String... stringRules) {
        return getKieContainer(model, toKieFiles(stringRules));
    }

    protected KieContainer getKieContainer(KieModuleModel model, KieFile... stringRules) {
        KieServices ks = KieServices.get();
        ReleaseId releaseId = ks.newReleaseId("org.kie", "kjar-test-" + UUID.randomUUID(), "1.0");

        KieBuilder kieBuilder = createKieBuilder(ks, model, releaseId, stringRules);
        return ks.newKieContainer(releaseId);
    }

    protected KieBuilder createKieBuilder(KieServices ks, KieModuleModel model, ReleaseId releaseId, KieFile... stringRules) {
        return createKieBuilder(ks, model, releaseId, true, stringRules);
    }

    protected KieBuilder createKieBuilder(KieServices ks, KieModuleModel model, ReleaseId releaseId, boolean failIfBuildError, KieFile... stringRules) {
        ks.getRepository().removeKieModule(releaseId);

        KieFileSystem kfs = ks.newKieFileSystem();
        if (model != null) {
            kfs.writeKModuleXML(model.toXML());
        }
        kfs.writePomXML(getPom(releaseId));
        for (int i = 0; i < stringRules.length; i++) {
            kfs.write(stringRules[i].path, stringRules[i].content);
        }

        // Interesting: If you switch to Exec Model here, the rules no longer compile.
        KieBuilder kieBuilder = ks.newKieBuilder(kfs).buildAll();

        if (failIfBuildError) {
            List<Message> messages = kieBuilder.getResults().getMessages();
            if (!messages.isEmpty()) {
                throw new IllegalStateException(messages.toString());
            }
        }

        return kieBuilder;
    }

    public KieFile[] toKieFiles(String[] stringRules) {
        KieFile[] kieFiles = new KieFile[stringRules.length];
        for (int i = 0; i < stringRules.length; i++) {
            kieFiles[i] = new KieFile(i, stringRules[i]);
        }
        return kieFiles;
    }

    private static class KieFile {

        public final String path;
        public final String content;

        public KieFile(int index, String content) {
            this(String.format("src/main/resources/r%d.drl", index), content);
        }

        public KieFile(String path, String content) {
            this.path = path;
            this.content = content;
        }
    }
}
