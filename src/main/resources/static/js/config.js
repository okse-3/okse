/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Norwegian Defence Research Establishment / NTNU
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 * Created by Fredrik Tørnvall (freboto) and Håkon Ødegård Løvdal (hakloev) on 02/03/15.
 */


var Config = (function($) {

    var createPanel = function() {

    }

    var bindButtons = function() {
        $('#chunked-encoding').on('click', function(e) {
           Main.ajax('config/encoding/', null, function() {
               console.log("[Debug] Chunked encoding set to: " + $('#chunked-encoding').is(":checked"))
           }, "POST");
        });

        $("#add-predefined-mapping").on('click', function(e) {
            console.log('[Debug] Add predefined mapping between ' + $('#from-topic').val() + ' --> ' + $('#to-topic').val())

        })
    }

    return {
        error: function(xhr, statusText, thrownError) {
            console.log("[Error] in Ajax for config with status: " + xhr.statusText)
        },
        refresh: function(response) {
            console.log(JSON.stringify(response))
        },
        init: function() {
            bindButtons()
        }
    }


})(jQuery)