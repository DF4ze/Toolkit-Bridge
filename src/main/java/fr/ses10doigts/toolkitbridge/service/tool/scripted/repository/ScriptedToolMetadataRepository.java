package fr.ses10doigts.toolkitbridge.service.tool.scripted.repository;

import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolActivationStatus;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScriptedToolMetadataRepository extends JpaRepository<ScriptedToolMetadata, Long> {

    @Query("select metadata from ScriptedToolMetadata metadata where metadata.name = :name")
    Optional<ScriptedToolMetadata> findByName(@Param("name") String name);

    List<ScriptedToolMetadata> findByActivationStatus(ScriptedToolActivationStatus activationStatus);
}
