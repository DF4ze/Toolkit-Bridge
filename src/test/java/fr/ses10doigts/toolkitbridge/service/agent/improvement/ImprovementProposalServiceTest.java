package fr.ses10doigts.toolkitbridge.service.agent.improvement;

import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.Artifact;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactContentPointer;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactDraft;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactType;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.service.ArtifactService;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPermissionControlService;
import fr.ses10doigts.toolkitbridge.service.agent.improvement.model.ImprovementObservation;
import fr.ses10doigts.toolkitbridge.service.agent.improvement.model.ImprovementProposal;
import fr.ses10doigts.toolkitbridge.service.agent.improvement.model.ImprovementProposalDraft;
import fr.ses10doigts.toolkitbridge.service.agent.improvement.model.ImprovementProposalMode;
import fr.ses10doigts.toolkitbridge.service.agent.improvement.model.ImprovementRecommendation;
import fr.ses10doigts.toolkitbridge.service.agent.improvement.model.ImprovementTargetType;
import fr.ses10doigts.toolkitbridge.service.agent.process.DefaultExternalProcessCatalog;
import fr.ses10doigts.toolkitbridge.service.agent.process.ExternalProcessService;
import fr.ses10doigts.toolkitbridge.service.agent.process.model.TextTemplateProcess;
import fr.ses10doigts.toolkitbridge.service.auth.CurrentAgentService;
import fr.ses10doigts.toolkitbridge.service.agent.trace.AgentTraceService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImprovementProposalServiceTest {

    @Test
    void createsTraceableProposalArtifactWithoutChangingRuntime() {
        ArtifactService artifactService = mock(ArtifactService.class);
        ExternalProcessService externalProcessService = mock(ExternalProcessService.class);
        AgentTraceService traceService = mock(AgentTraceService.class);

        when(externalProcessService.loadTypedContent(
                ImprovementProposalService.IMPROVEMENT_PROPOSAL_PROCESS_ID,
                TextTemplateProcess.class
        )).thenReturn(new TextTemplateProcess(
                "# Improvement proposal\n\nMode={{mode}}\nTarget={{targetType}}/{{targetReference}}\n{{guardrailNote}}\n"
        ));
        when(externalProcessService.renderTemplate(any(), any())).thenCallRealMethod();
        when(artifactService.create(any(ArtifactDraft.class))).thenReturn(new Artifact(
                "artifact-1",
                ArtifactType.PROPOSAL,
                "task-1",
                "agent-1",
                "Improvement proposal - Slow task prompt",
                Instant.parse("2026-04-07T00:00:00Z"),
                Instant.parse("2026-04-08T00:00:00Z"),
                Map.of("mode", "PROPOSAL_ONLY"),
                new ArtifactContentPointer("workspace", "artifacts/proposal/1.md", "text/markdown", 12)
        ));

        ImprovementProposalService service = new ImprovementProposalService(
                artifactService,
                externalProcessService,
                traceService
        );

        ImprovementProposal proposal = service.propose(new ImprovementProposalDraft(
                "task-1",
                "agent-1",
                "trace-1",
                new ImprovementObservation(
                        "latency",
                        "Slow task prompt",
                        "The task orchestration prompt is repeated and expensive to tune in code.",
                        Map.of("processId", "task-execution-prompt")
                ),
                new ImprovementRecommendation(
                        ImprovementTargetType.PROCESS,
                        "task-execution-prompt",
                        "Externalize prompt refinements via the managed process file.",
                        "This keeps prompt changes auditable and decoupled from Java classes.",
                        "Revise the prompt wording in the external process after review."
                ),
                Map.of("origin", "phase16b")
        ));

        ArgumentCaptor<ArtifactDraft> draftCaptor = ArgumentCaptor.forClass(ArtifactDraft.class);
        verify(artifactService).create(draftCaptor.capture());
        verify(traceService, times(2)).trace(any(), any(), any(), any());

        ArtifactDraft artifactDraft = draftCaptor.getValue();
        assertThat(proposal.mode()).isEqualTo(ImprovementProposalMode.PROPOSAL_ONLY);
        assertThat(artifactDraft.type()).isEqualTo(ArtifactType.PROPOSAL);
        assertThat(artifactDraft.metadata()).containsEntry("mode", "PROPOSAL_ONLY");
        assertThat(artifactDraft.content()).contains("Improvement proposal").contains("PROPOSAL_ONLY");
    }
}
