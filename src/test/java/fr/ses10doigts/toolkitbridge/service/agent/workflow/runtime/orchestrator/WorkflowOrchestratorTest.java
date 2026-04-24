package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.orchestrator;

import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowExecutionContext;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowRun;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowRunStatus;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStep;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStepDecision;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStepResult;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowOrchestratorTest {

    private final WorkflowOrchestrator orchestrator = new WorkflowOrchestrator();

    @Test
    void executesStepOnceAndReturnsResultForSupportedDecision() {
        WorkflowExecutionContext context = buildContext();
        AtomicInteger executionCount = new AtomicInteger(0);
        WorkflowStepResult expected = new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "ok",
                Map.of("k", "v")
        );

        WorkflowStep step = executionContext -> {
            executionCount.incrementAndGet();
            return expected;
        };

        WorkflowStepResult actual = orchestrator.executeSingleStep(context, step);

        assertThat(actual).isEqualTo(expected);
        assertThat(executionCount.get()).isEqualTo(1);
    }

    @Test
    void returnsResultForWaitHumanDecision() {
        WorkflowExecutionContext context = buildContext();
        WorkflowStepResult expected = new WorkflowStepResult(
                WorkflowStepDecision.WAIT_HUMAN,
                "waiting human input",
                Map.of("questionId", "q-1")
        );

        WorkflowStepResult actual = orchestrator.executeSingleStep(context, executionContext -> expected);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void returnsResultForStopFailureDecision() {
        WorkflowExecutionContext context = buildContext();
        WorkflowStepResult expected = new WorkflowStepResult(
                WorkflowStepDecision.STOP_FAILURE,
                "failed",
                Map.of("reason", "technical error")
        );

        WorkflowStepResult actual = orchestrator.executeSingleStep(context, executionContext -> expected);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void rejectsNullContext() {
        WorkflowStep step = context -> new WorkflowStepResult(WorkflowStepDecision.CONTINUE, null, Map.of());

        assertThatThrownBy(() -> orchestrator.executeSingleStep(null, step))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("context");
    }

    @Test
    void rejectsNullStep() {
        WorkflowExecutionContext context = buildContext();

        assertThatThrownBy(() -> orchestrator.executeSingleStep(context, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("step");
    }

    @Test
    void rejectsNullStepResult() {
        WorkflowExecutionContext context = buildContext();

        assertThatThrownBy(() -> orchestrator.executeSingleStep(context, executionContext -> null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("result");
    }

    @Test
    void rejectsUnsupportedDecision() {
        WorkflowExecutionContext context = buildContext();
        WorkflowStep step = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.RETRY_CORRECTION,
                "not yet",
                Map.of()
        );

        assertThatThrownBy(() -> orchestrator.executeSingleStep(context, step))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unsupported workflow decision")
                .hasMessageContaining("RETRY_CORRECTION");
    }

    @Test
    void rejectsUnsupportedDecisionFinish() {
        WorkflowExecutionContext context = buildContext();
        WorkflowStep step = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.FINISH,
                "done",
                Map.of()
        );

        assertThatThrownBy(() -> orchestrator.executeSingleStep(context, step))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unsupported workflow decision")
                .hasMessageContaining("FINISH");
    }

    @Test
    void executesSecondStepWhenFirstStepContinuesAndReturnsSecondResult() {
        WorkflowExecutionContext context = buildContext();
        AtomicInteger secondStepExecutionCount = new AtomicInteger(0);

        WorkflowStep firstStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "first ok",
                Map.of("step", 1)
        );
        WorkflowStepResult secondResult = new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "second ok",
                Map.of("step", 2)
        );
        WorkflowStep secondStep = executionContext -> {
            secondStepExecutionCount.incrementAndGet();
            return secondResult;
        };

        WorkflowStepResult actual = orchestrator.executeTwoSteps(context, firstStep, secondStep);

        assertThat(actual).isEqualTo(secondResult);
        assertThat(secondStepExecutionCount.get()).isEqualTo(1);
    }

    @Test
    void returnsFirstResultAndSkipsSecondStepWhenFirstStepWaitsHuman() {
        WorkflowExecutionContext context = buildContext();
        AtomicInteger secondStepExecutionCount = new AtomicInteger(0);

        WorkflowStepResult firstResult = new WorkflowStepResult(
                WorkflowStepDecision.WAIT_HUMAN,
                "waiting",
                Map.of("step", 1)
        );
        WorkflowStep firstStep = executionContext -> firstResult;
        WorkflowStep secondStep = executionContext -> {
            secondStepExecutionCount.incrementAndGet();
            return new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "second", Map.of());
        };

        WorkflowStepResult actual = orchestrator.executeTwoSteps(context, firstStep, secondStep);

        assertThat(actual).isEqualTo(firstResult);
        assertThat(secondStepExecutionCount.get()).isEqualTo(0);
    }

    @Test
    void returnsFirstResultAndSkipsSecondStepWhenFirstStepStopsFailure() {
        WorkflowExecutionContext context = buildContext();
        AtomicInteger secondStepExecutionCount = new AtomicInteger(0);

        WorkflowStepResult firstResult = new WorkflowStepResult(
                WorkflowStepDecision.STOP_FAILURE,
                "failed",
                Map.of("step", 1)
        );
        WorkflowStep firstStep = executionContext -> firstResult;
        WorkflowStep secondStep = executionContext -> {
            secondStepExecutionCount.incrementAndGet();
            return new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "second", Map.of());
        };

        WorkflowStepResult actual = orchestrator.executeTwoSteps(context, firstStep, secondStep);

        assertThat(actual).isEqualTo(firstResult);
        assertThat(secondStepExecutionCount.get()).isEqualTo(0);
    }

    @Test
    void returnsSecondResultWhenSecondStepWaitsHuman() {
        WorkflowExecutionContext context = buildContext();

        WorkflowStep firstStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "first ok",
                Map.of("step", 1)
        );
        WorkflowStepResult secondResult = new WorkflowStepResult(
                WorkflowStepDecision.WAIT_HUMAN,
                "need human",
                Map.of("step", 2)
        );
        WorkflowStep secondStep = executionContext -> secondResult;

        WorkflowStepResult actual = orchestrator.executeTwoSteps(context, firstStep, secondStep);

        assertThat(actual).isEqualTo(secondResult);
    }

    @Test
    void rejectsNullContextForExecuteTwoSteps() {
        WorkflowStep firstStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "first",
                Map.of()
        );
        WorkflowStep secondStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "second",
                Map.of()
        );

        assertThatThrownBy(() -> orchestrator.executeTwoSteps(null, firstStep, secondStep))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("context");
    }

    @Test
    void rejectsNullFirstStepForExecuteTwoSteps() {
        WorkflowExecutionContext context = buildContext();
        WorkflowStep secondStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "second",
                Map.of()
        );

        assertThatThrownBy(() -> orchestrator.executeTwoSteps(context, null, secondStep))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("firstStep");
    }

    @Test
    void rejectsNullSecondStepForExecuteTwoSteps() {
        WorkflowExecutionContext context = buildContext();
        WorkflowStep firstStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "first",
                Map.of()
        );

        assertThatThrownBy(() -> orchestrator.executeTwoSteps(context, firstStep, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("secondStep");
    }

    @Test
    void executesThirdStepWhenFirstTwoStepsContinueAndReturnsThirdResult() {
        WorkflowExecutionContext context = buildContext();
        AtomicInteger thirdStepExecutionCount = new AtomicInteger(0);

        WorkflowStep firstStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "first ok",
                Map.of("step", 1)
        );
        WorkflowStep secondStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "second ok",
                Map.of("step", 2)
        );
        WorkflowStepResult thirdResult = new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "third ok",
                Map.of("step", 3)
        );
        WorkflowStep thirdStep = executionContext -> {
            thirdStepExecutionCount.incrementAndGet();
            return thirdResult;
        };

        WorkflowStepResult actual = orchestrator.executeThreeSteps(context, firstStep, secondStep, thirdStep);

        assertThat(actual).isEqualTo(thirdResult);
        assertThat(thirdStepExecutionCount.get()).isEqualTo(1);
    }

    @Test
    void returnsFirstResultAndSkipsSecondAndThirdStepsWhenFirstStepBlocks() {
        WorkflowExecutionContext context = buildContext();
        AtomicInteger secondStepExecutionCount = new AtomicInteger(0);
        AtomicInteger thirdStepExecutionCount = new AtomicInteger(0);

        WorkflowStepResult firstResult = new WorkflowStepResult(
                WorkflowStepDecision.WAIT_HUMAN,
                "waiting",
                Map.of("step", 1)
        );
        WorkflowStep firstStep = executionContext -> firstResult;
        WorkflowStep secondStep = executionContext -> {
            secondStepExecutionCount.incrementAndGet();
            return new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "second", Map.of());
        };
        WorkflowStep thirdStep = executionContext -> {
            thirdStepExecutionCount.incrementAndGet();
            return new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "third", Map.of());
        };

        WorkflowStepResult actual = orchestrator.executeThreeSteps(context, firstStep, secondStep, thirdStep);

        assertThat(actual).isEqualTo(firstResult);
        assertThat(secondStepExecutionCount.get()).isEqualTo(0);
        assertThat(thirdStepExecutionCount.get()).isEqualTo(0);
    }

    @Test
    void returnsSecondResultAndSkipsThirdStepWhenSecondStepBlocks() {
        WorkflowExecutionContext context = buildContext();
        AtomicInteger thirdStepExecutionCount = new AtomicInteger(0);

        WorkflowStep firstStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "first ok",
                Map.of("step", 1)
        );
        WorkflowStepResult secondResult = new WorkflowStepResult(
                WorkflowStepDecision.STOP_FAILURE,
                "second failed",
                Map.of("step", 2)
        );
        WorkflowStep secondStep = executionContext -> secondResult;
        WorkflowStep thirdStep = executionContext -> {
            thirdStepExecutionCount.incrementAndGet();
            return new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "third", Map.of());
        };

        WorkflowStepResult actual = orchestrator.executeThreeSteps(context, firstStep, secondStep, thirdStep);

        assertThat(actual).isEqualTo(secondResult);
        assertThat(thirdStepExecutionCount.get()).isEqualTo(0);
    }

    @Test
    void returnsThirdResultWhenThirdStepWaitsHuman() {
        WorkflowExecutionContext context = buildContext();

        WorkflowStep firstStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "first ok",
                Map.of("step", 1)
        );
        WorkflowStep secondStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "second ok",
                Map.of("step", 2)
        );
        WorkflowStepResult thirdResult = new WorkflowStepResult(
                WorkflowStepDecision.WAIT_HUMAN,
                "third needs human",
                Map.of("step", 3)
        );
        WorkflowStep thirdStep = executionContext -> thirdResult;

        WorkflowStepResult actual = orchestrator.executeThreeSteps(context, firstStep, secondStep, thirdStep);

        assertThat(actual).isEqualTo(thirdResult);
    }

    @Test
    void rejectsNullContextForExecuteThreeSteps() {
        WorkflowStep firstStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "first",
                Map.of()
        );
        WorkflowStep secondStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "second",
                Map.of()
        );
        WorkflowStep thirdStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "third",
                Map.of()
        );

        assertThatThrownBy(() -> orchestrator.executeThreeSteps(null, firstStep, secondStep, thirdStep))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("context");
    }

    @Test
    void rejectsNullFirstStepForExecuteThreeSteps() {
        WorkflowExecutionContext context = buildContext();
        WorkflowStep secondStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "second",
                Map.of()
        );
        WorkflowStep thirdStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "third",
                Map.of()
        );

        assertThatThrownBy(() -> orchestrator.executeThreeSteps(context, null, secondStep, thirdStep))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("firstStep");
    }

    @Test
    void rejectsNullSecondStepForExecuteThreeSteps() {
        WorkflowExecutionContext context = buildContext();
        WorkflowStep firstStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "first",
                Map.of()
        );
        WorkflowStep thirdStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "third",
                Map.of()
        );

        assertThatThrownBy(() -> orchestrator.executeThreeSteps(context, firstStep, null, thirdStep))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("secondStep");
    }

    @Test
    void rejectsNullThirdStepForExecuteThreeSteps() {
        WorkflowExecutionContext context = buildContext();
        WorkflowStep firstStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "first",
                Map.of()
        );
        WorkflowStep secondStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "second",
                Map.of()
        );

        assertThatThrownBy(() -> orchestrator.executeThreeSteps(context, firstStep, secondStep, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("thirdStep");
    }

    @Test
    void executesCorrectionWhenReviewRequiresCorrection() {
        WorkflowExecutionContext context = buildContext();
        AtomicInteger correctionExecutionCount = new AtomicInteger(0);

        WorkflowStep analysisStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "analysis ok",
                Map.of()
        );
        WorkflowStep reviewStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "review needs correction",
                Map.of("requiresCorrection", true)
        );
        WorkflowStepResult correctionResult = new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "correction done",
                Map.of("step", "correction")
        );
        WorkflowStep correctionStep = executionContext -> {
            correctionExecutionCount.incrementAndGet();
            return correctionResult;
        };

        WorkflowStepResult actual = orchestrator.executeAnalysisReviewWithOptionalCorrection(
                context,
                analysisStep,
                reviewStep,
                correctionStep
        );

        assertThat(actual.decision()).isEqualTo(WorkflowStepDecision.CONTINUE);
        assertThat(actual.message()).isEqualTo("correction done");
        assertThat(actual.data())
                .containsEntry("correctionTriggered", true)
                .containsEntry("finalDecision", "CONTINUE");
        assertThat(correctionExecutionCount.get()).isEqualTo(1);
    }

    @Test
    void skipsCorrectionWhenReviewDoesNotRequireCorrection() {
        WorkflowExecutionContext context = buildContext();
        AtomicInteger correctionExecutionCount = new AtomicInteger(0);

        WorkflowStep analysisStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "analysis ok",
                Map.of()
        );
        WorkflowStep reviewStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "review ok",
                Map.of("requiresCorrection", false)
        );
        WorkflowStep correctionStep = executionContext -> {
            correctionExecutionCount.incrementAndGet();
            return new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "correction", Map.of());
        };

        WorkflowStepResult actual = orchestrator.executeAnalysisReviewWithOptionalCorrection(
                context,
                analysisStep,
                reviewStep,
                correctionStep
        );

        assertThat(actual.decision()).isEqualTo(WorkflowStepDecision.CONTINUE);
        assertThat(actual.message()).isEqualTo("Review completed - no correction required");
        assertThat(actual.data())
                .containsEntry("requiresCorrection", false)
                .containsEntry("correctionTriggered", false)
                .containsEntry("finalDecision", "CONTINUE");
        assertThat(correctionExecutionCount.get()).isEqualTo(0);
    }

    @Test
    void returnsWaitHumanAndSkipsCorrectionWhenReviewWaitsHuman() {
        WorkflowExecutionContext context = buildContext();
        AtomicInteger correctionExecutionCount = new AtomicInteger(0);

        WorkflowStep analysisStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "analysis ok",
                Map.of()
        );
        WorkflowStepResult waitHumanResult = new WorkflowStepResult(
                WorkflowStepDecision.WAIT_HUMAN,
                "need human validation",
                Map.of()
        );
        WorkflowStep reviewStep = executionContext -> waitHumanResult;
        WorkflowStep correctionStep = executionContext -> {
            correctionExecutionCount.incrementAndGet();
            return new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "correction", Map.of());
        };

        WorkflowStepResult actual = orchestrator.executeAnalysisReviewWithOptionalCorrection(
                context,
                analysisStep,
                reviewStep,
                correctionStep
        );

        assertThat(actual.decision()).isEqualTo(WorkflowStepDecision.WAIT_HUMAN);
        assertThat(actual.message()).isEqualTo("need human validation");
        assertThat(actual.data())
                .containsEntry("correctionTriggered", false)
                .containsEntry("finalDecision", "WAIT_HUMAN");
        assertThat(correctionExecutionCount.get()).isEqualTo(0);
    }

    @Test
    void returnsStopFailureAndSkipsCorrectionWhenReviewFails() {
        WorkflowExecutionContext context = buildContext();
        AtomicInteger correctionExecutionCount = new AtomicInteger(0);

        WorkflowStep analysisStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "analysis ok",
                Map.of()
        );
        WorkflowStepResult stopFailureResult = new WorkflowStepResult(
                WorkflowStepDecision.STOP_FAILURE,
                "review failed",
                Map.of()
        );
        WorkflowStep reviewStep = executionContext -> stopFailureResult;
        WorkflowStep correctionStep = executionContext -> {
            correctionExecutionCount.incrementAndGet();
            return new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "correction", Map.of());
        };

        WorkflowStepResult actual = orchestrator.executeAnalysisReviewWithOptionalCorrection(
                context,
                analysisStep,
                reviewStep,
                correctionStep
        );

        assertThat(actual.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(actual.message()).isEqualTo("review failed");
        assertThat(actual.data())
                .containsEntry("correctionTriggered", false)
                .containsEntry("finalDecision", "STOP_FAILURE");
        assertThat(correctionExecutionCount.get()).isEqualTo(0);
    }

    @Test
    void failsExplicitlyWhenReviewContinueResultDoesNotProvideRequiresCorrectionFlag() {
        WorkflowExecutionContext context = buildContext();

        WorkflowStep analysisStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "analysis ok",
                Map.of()
        );
        WorkflowStep reviewStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "review continue without contract",
                Map.of("anotherKey", "value")
        );
        WorkflowStep correctionStep = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "correction",
                Map.of()
        );

        assertThatThrownBy(() -> orchestrator.executeAnalysisReviewWithOptionalCorrection(
                context,
                analysisStep,
                reviewStep,
                correctionStep
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requiresCorrection");
    }

    private WorkflowExecutionContext buildContext() {
        WorkflowRun workflowRun = new WorkflowRun(
                "run-1",
                "codex-implementation",
                "phase-3/step-2",
                WorkflowRunStatus.RUNNING,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        return new WorkflowExecutionContext(workflowRun, null, Map.of());
    }
}
