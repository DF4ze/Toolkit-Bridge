package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.project;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowProjectPathPolicyTest {

    private final WorkflowProjectPathPolicy policy = new WorkflowProjectPathPolicy();

    @Test
    void windowsRootIsDangerous() {
        assertThat(policy.isDangerous(Path.of("C:\\"), PathOsFamily.WINDOWS, Path.of("C:\\Users\\clement"))).isTrue();
        assertThat(policy.isDangerous(Path.of("D:\\"), PathOsFamily.WINDOWS, Path.of("C:\\Users\\clement"))).isTrue();
    }

    @Test
    void windowsSystemDirectoriesAreDangerous() {
        assertThat(policy.isDangerous(Path.of("C:\\Windows"), PathOsFamily.WINDOWS, Path.of("C:\\Users\\clement"))).isTrue();
        assertThat(policy.isDangerous(Path.of("C:\\Windows\\System32"), PathOsFamily.WINDOWS, Path.of("C:\\Users\\clement"))).isTrue();
        assertThat(policy.isDangerous(Path.of("C:\\Program Files"), PathOsFamily.WINDOWS, Path.of("C:\\Users\\clement"))).isTrue();
        assertThat(policy.isDangerous(Path.of("C:\\Program Files (x86)\\Foo"), PathOsFamily.WINDOWS, Path.of("C:\\Users\\clement"))).isTrue();
        assertThat(policy.isDangerous(Path.of("C:\\Users"), PathOsFamily.WINDOWS, Path.of("C:\\Users\\clement"))).isTrue();

        assertThat(policy.isDangerous(Path.of("D:\\Windows"), PathOsFamily.WINDOWS, Path.of("C:\\Users\\clement"))).isTrue();
        assertThat(policy.isDangerous(Path.of("D:\\Windows\\System32"), PathOsFamily.WINDOWS, Path.of("C:\\Users\\clement"))).isTrue();
        assertThat(policy.isDangerous(Path.of("D:\\Program Files"), PathOsFamily.WINDOWS, Path.of("C:\\Users\\clement"))).isTrue();
        assertThat(policy.isDangerous(Path.of("D:\\Program Files (x86)\\Foo"), PathOsFamily.WINDOWS, Path.of("C:\\Users\\clement"))).isTrue();
        assertThat(policy.isDangerous(Path.of("D:\\Users"), PathOsFamily.WINDOWS, Path.of("C:\\Users\\clement"))).isTrue();
    }

    @Test
    void windowsHomeDirectIsDangerousButSubfolderIsAllowed() {
        Path home = Path.of("C:\\Users\\clement");
        assertThat(policy.isDangerous(home, PathOsFamily.WINDOWS, home)).isTrue();
        assertThat(policy.isDangerous(Path.of("C:\\Users\\clement\\Documents\\Spring\\Toolkit-Bridge"), PathOsFamily.WINDOWS, home)).isFalse();
    }

    @Test
    void unixRootAndSystemDirectoriesAreDangerous() {
        Path home = Path.of("/home/clement");
        assertThat(policy.isDangerous(Path.of("/"), PathOsFamily.UNIX, home)).isTrue();
        assertThat(policy.isDangerous(Path.of("/etc"), PathOsFamily.UNIX, home)).isTrue();
        assertThat(policy.isDangerous(Path.of("/etc/ssl"), PathOsFamily.UNIX, home)).isTrue();
        assertThat(policy.isDangerous(Path.of("/usr"), PathOsFamily.UNIX, home)).isTrue();
        assertThat(policy.isDangerous(Path.of("/var/log"), PathOsFamily.UNIX, home)).isTrue();
    }

    @Test
    void unixHomeDirectIsDangerousButSubfolderIsAllowed() {
        Path home = Path.of("/home/clement");
        assertThat(policy.isDangerous(home, PathOsFamily.UNIX, home)).isTrue();
        assertThat(policy.isDangerous(Path.of("/home/clement/projects/Toolkit-Bridge"), PathOsFamily.UNIX, home)).isFalse();
    }
}
