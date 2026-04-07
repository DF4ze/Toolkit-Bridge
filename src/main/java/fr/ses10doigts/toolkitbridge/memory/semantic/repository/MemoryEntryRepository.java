package fr.ses10doigts.toolkitbridge.memory.semantic.repository;

import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryScope;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryStatus;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemoryEntryRepository extends JpaRepository<MemoryEntry, Long> {

    List<MemoryEntry> findByAgentIdAndScope(String agentId, MemoryScope scope);

    List<MemoryEntry> findByAgentIdAndType(String agentId, MemoryType type);

    List<MemoryEntry> findByAgentIdAndStatus(String agentId, MemoryStatus status);

    @Query("select m from MemoryEntry m " +
            "where m.agentId = :agentId " +
            "and lower(m.content) like lower(concat('%', :query, '%'))")
    List<MemoryEntry> searchByAgentIdAndContent(@Param("agentId") String agentId,
                                                @Param("query") String query);

    @Query("select distinct m from MemoryEntry m left join m.tags t " +
            "where m.agentId = :agentId " +
            "and m.status = :status " +
            "and (:query is null or :query = '' " +
            "or lower(m.content) like lower(concat('%', :query, '%')) " +
            "or lower(t) like lower(concat('%', :query, '%')))")
    List<MemoryEntry> searchCandidates(@Param("agentId") String agentId,
                                       @Param("status") MemoryStatus status,
                                       @Param("query") String query,
                                       Pageable pageable);
}
