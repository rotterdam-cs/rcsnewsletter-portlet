/**
 * Custom Email Validator
 * using custom regular expression
 */

$.validator.addMethod(
        "custom-email",
        function(value, element) {
            var re = /^[_A-Za-z0-9-]+(\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]([_A-Za-z0-9-]+)+(\.[A-Za-z0-9-]+)*(\.[A-Za-z]{2,})$/;
            return this.optional(element) || re.test(value);
        },
        $.validator.messages.email
);
$.validator.addMethod(
        "custom-list",
        function(value, element) {
            {
                if (element.value === "0") {return false;}
                else return true;
             }
        },
        $.validator.messages.required
);