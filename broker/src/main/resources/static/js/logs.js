/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 - 2018 Norwegian Defence Research Establishment / NTNU
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

var Logs = (function($) {

    var url = "log?logLevel=DEBUG&logID=0";
    var logLevel = "DEBUG";
    var logID = 0;
    var logLength = 250;

    var updateUrl = function() {
        url = "log?logLevel=" + logLevel + "&logID=" + logID + "&length=" + logLength
    };

    var updateLogView = function(data) {
        var HTML = '<pre class="log-view">';
        $.each(data.lines, function (i, line) {
           HTML += line + '\n'
        });
        HTML += '</pre>';
        return HTML
    };

    return {
        init: function() {
            /*
            * Get log levels from the backend.
            * Create buttons
            * Initialize buttons with filters
            * */
            Main.ajax({
                url: 'log/levels',
                type: 'GET',
                success: function(data) {
                    $.okseDebug.logPrint("[Debug][Logs] Adding log level buttons");
                    var buttons = "";
                    $.each(data, function(i, level) {
                        if (level == 'DEBUG') {
                            buttons += '<button type="button" class="btn btn-info active" id="button-' + level + '">' + level + '</button>'
                        } else {
                            buttons += '<button type="button" class="btn btn-info" id="button-' + level + '">' + level + '</button>'
                        }
                    });
                    $("#log-level").html(buttons);
                    $.each(data, function(i, level) {
                        $("#button-" + level).on("click", function(e){
                            e.preventDefault();
                            $('#button-' + level).addClass('active').siblings().removeClass('active');
                            logLevel = level;
                            updateUrl();
                            Main.setIntervalForTab({
                                url: Logs.url(),
                                type: 'GET',
                                success: Logs.refresh
                            })
                            /*
                            Main.ajax({
                                url: Logs.url(),
                                type: 'GET',
                                success: Logs.refresh

                            });
                            */
                        })
                    })
                }
            });

            /*
             * Get log files from the backend.
             * Create buttons
             * Initialize buttons with filters
             * */
            Main.ajax({
                url: 'log/files',
                type: 'GET',
                success: function(data) {
                    $.okseDebug.logPrint("[Debug][Logs] Adding log files buttons");
                    var files = "";
                    $.each(data, function(i) {
                        if (i == 0) {
                            files += '<option value="' + i + '" selected>' + data[i] + '</option>'
                        } else {
                            files += '<option value="' + i + '">' + data[i] + '</option>'
                        }
                    });
                    $("#log-file").html(files);

                    $('#log-file').on('change', function() {
                        logID = $(this).val();
                        updateUrl();
                        Main.setIntervalForTab({
                            url: Logs.url(),
                            type: 'GET',
                            success: Logs.refresh
                        });
                    });
                }
            });

            $("#log-length").keyup(function(){
                logLength = $(this).val();
                updateUrl();
                Main.setIntervalForTab({
                    url: Logs.url(),
                    type: 'GET',
                    success: Logs.refresh
                })
            });

            /*
             * Add a listener to clear interval
             * */
            $("#logs-button-refresh").on("click", function(e) {
                e.preventDefault();
                if (!$(this).hasClass("active")) {
                    $(this).addClass("active");
                    $(this).text("Stop refresh");
                    Main.setIntervalForTab({
                        url: Logs.url(),
                        type: 'GET',
                        success: Logs.refresh
                    });
                } else {
                    $(this).removeClass("active");
                    $(this).text("Start refresh");
                    Main.clearIntervalForTab()
                }
            })
        },
        refresh: function(data) {
            $('#log-name').html(data.name);
            $('#log-data').html(updateLogView(data))
        },
        url: function() {
            return url
        }
    }

})(jQuery);
