package fr.ses10doigts.toolkitbridge.service.agent.process.store;

import fr.ses10doigts.toolkitbridge.service.agent.process.model.ExternalProcessHistoryEntry;
import fr.ses10doigts.toolkitbridge.service.agent.process.model.ExternalProcessSnapshot;
import fr.ses10doigts.toolkitbridge.service.agent.process.model.ExternalProcessUpdateRequest;

import java.util.List;
import java.util.Optional;

public interface ExternalProcessStore {

    Optional<ExternalProcessSnapshot> findById(String processId);

    ExternalProcessSnapshot save(ExternalProcessUpdateRequest request);

    List<ExternalProcessHistoryEntry> loadHistory(String processId);
}
