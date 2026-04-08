(function ($) {
    "use strict";

    $(function () {
        var dateTarget = $(".js-marcel-today");
        if (dateTarget.length > 0) {
            var today = new Date();
            dateTarget.text(today.toLocaleDateString());
        }
    });
})(jQuery);
