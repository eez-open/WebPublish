$(function () {
    "use strict";

    var HIDDEN_INLINE_STYLE = "skriveno_tekst";
    var HIDDEN_INLINE_LINK_STYLE = "skriveno_link";

    var HIDDEN_BLOCK_STYLE = "skriveno";
    var HIDDEN_BLOCK_LINK_STYLE = "skriveno_paragraf";

    $.fn.outerHTML = function (arg) {
        var ret;

        // If no items in the collection, return
        if (!this.length) {
            return typeof arg == "undefined" ? this : null;
        }
        // Getter overload (no argument passed)
        if (!arg) {
            return this[0].outerHTML || 
                (ret = this.wrap('<div>').parent().html(), this.unwrap(), ret);
        }
        // Setter overload
        $.each(this, function (i, el) {
            var fnRet, 
                pass = el,
                inOrOut = el.outerHTML ? "outerHTML" : "innerHTML";

            if (!el.outerHTML)
                el = $(el).wrap('<div>').parent()[0];

            if (jQuery.isFunction(arg)) { 
                if ((fnRet = arg.call(pass, i, el[inOrOut])) !== false)
                    el[inOrOut] = fnRet;
            }
            else
                el[inOrOut] = arg;

            if (!el.outerHTML)
                $(el).children().unwrap();
        });

        return this;
    };

    function getInlineHiddenContent(el) {
        var result = "";

        $.each($(el).nextAll(), function () {
            if ($(this).is('.' + HIDDEN_INLINE_STYLE) || $(this).children().is('.' + HIDDEN_INLINE_STYLE)) {
                result += $(this).outerHTML();
            }
            if (result !== "" && $(this).is('.' + HIDDEN_INLINE_LINK_STYLE)) {
                return false;
            }
        });

        return result;
    }

    function getParagraphHiddenContent(el) {
        var result = "";

        $.each($(el).parent().nextAll(), function () {
            if ($(this).is('.' + HIDDEN_BLOCK_STYLE)) {
                result += $(this).outerHTML();
            } else if (result !== "") {
                return false;
            }
        });

        return result;
    }

    function tooltip(el, contents) {
        $(el).balloon({
            contents: contents,
            classname: "balloon",
            position: 'top',
            delay: 400,
            minLifetime: 200,
            showDuration: 0,
            hideDuration: 0,
            css: {
                opacity: "1",
                maxWidth: "400px",
                boxShadow: "2px 2px 8px #aaa",
                border: "solid 1px #ccc",
                backgroundColor: "#f1f1f1"
            }
        });
    }

    $("." + HIDDEN_INLINE_LINK_STYLE).each(function () {
        tooltip(this, getInlineHiddenContent(this));
    });

    $("." + HIDDEN_BLOCK_LINK_STYLE).each(function () {
        tooltip(this, getParagraphHiddenContent(this));
    });
});