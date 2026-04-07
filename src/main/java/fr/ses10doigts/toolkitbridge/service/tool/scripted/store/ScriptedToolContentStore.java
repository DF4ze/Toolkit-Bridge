package fr.ses10doigts.toolkitbridge.service.tool.scripted.store;

public interface ScriptedToolContentStore {

    String save(String toolName, int version, String runtimeType, String content);

    String load(String relativePath);

    void delete(String relativePath);
}
