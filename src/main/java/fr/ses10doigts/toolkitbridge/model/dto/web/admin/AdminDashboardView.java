package fr.ses10doigts.toolkitbridge.model.dto.web.admin;

import java.util.List;

public record AdminDashboardView(
        List<SummaryCard> summaryCards,
        List<ShortcutLink> shortcutLinks,
        List<SystemItem> systemItems
) {

    public record SummaryCard(
            String title,
            String value,
            String detail,
            String toneClass,
            String href
    ) {
    }

    public record ShortcutLink(
            String title,
            String description,
            String href
    ) {
    }

    public record SystemItem(
            String label,
            String value
    ) {
    }
}
