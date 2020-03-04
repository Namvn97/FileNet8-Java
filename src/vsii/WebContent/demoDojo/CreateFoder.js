define([
        "dojo/_base/declare",
        "ecm/widget/layout/_LaunchBarPane",
        "dojo/text!./templates/CreateFoder.html",
        "ecm/model/Request",
        "dojo/_base/lang",
    ],
    function (declare,
              _LaunchBarPane,
              template,
              Request,
              lang) {
        /**
         * @name demoDojo.CreateFoder
         * @class
         * @augments ecm.widget.layout._LaunchBarPane
         */
        return declare("demoDojo.CreateFoder", [
            _LaunchBarPane
        ], {
            /** @lends demoDojo.CreateFoder.prototype */

            templateString: template,

            // Set to true if widget template contains DOJO widgets.
            widgetsInTemplate: false,

            postCreate: function () {
                this.logEntry("postCreate");
                this.inherited(arguments);

                /**
                 * Add custom logic (if any) that should be necessary after the feature pane is created. For example,
                 * you might need to connect events to trigger the pane to update based on specific user actions.
                 */

                this.logExit("postCreate");
            },

            /**
             * Optional method that sets additional parameters when the user clicks on the launch button associated with
             * this feature.
             */
            setParams: function (params) {
                this.logEntry("setParams", params);

                if (params) {

                    if (!this.isLoaded && this.selected) {
                        this.loadContent();
                    }
                }

                this.logExit("setParams");
            },

            /**
             * Loads the content of the pane. This is a required method to insert a pane into the LaunchBarContainer.
             */
            loadContent: function () {
                require({paths: {"jquery": "../plugin/Demo/getResource/jQuery/jquery"}}, ["jquery"], function (jquery) {

                });

                $("#createBtn").click(function () {
                    var nameFolder = $("#nameFolder").val();
                    var nameSubFolder = $("#nameSubFolder").val();
                    var sizeListFile = $("#chooseFile")[0].files.length;
                    var contentFile = "";
                    var typeDoc = "";
                    var nameFile = "";
                    for (var index = 0; index < sizeListFile; index++) {
                        var f = $("#chooseFile")[0].files[index];
                        var reader = new FileReader();
                        reader.onload = (function (theFile) {
                            return function (e) {
                                index--;
                                var binaryData = e.target.result;
                                var file = $("#chooseFile")[0].files[index];
                                if (index != 0) {
                                    contentFile += window.btoa(binaryData) + "endFile";
                                    typeDoc += file.type + "endType";
                                    nameFile += file.name + "endName";
                                } else {
                                    contentFile += window.btoa(binaryData);
                                    typeDoc += file.type;
                                    nameFile += file.name;
                                    Request.invokePluginService("Demo", "CreateFolderService", {
                                        requestParams: {
                                            stringAction: "createFolder",
                                            nameFolder: nameFolder,
                                            nameSubFolder: nameSubFolder,
                                            typeDocument: typeDoc,
                                            nameFile: nameFile,
                                            content: contentFile
                                        },
                                        requestCompleteCallback: lang.hitch(this, function (response) {
                                            $("#textResponse").text(response.messageResponse);
                                        })
                                    });
                                }
                            };
                        })(f);
                        reader.readAsBinaryString(f);
                    }
                });

                $("#copyBtn").click(function () {
                    var nameFolderCopy = $("#copyFolder").val();
                    var newNameFolder = $("#newNameFolder").val();
                    Request.invokePluginService("Demo", "CreateFolderService", {
                        requestParams: {
                            stringAction: "copyFolder",
                            nameFolderCopy: nameFolderCopy,
                            newNameFolder: newNameFolder
                        },
                        requestCompleteCallback: lang.hitch(this, function (response) {
                            $("#textResponse").text(response.messageResponse);
                        })
                    });
                });

                $("#deleteBtn").click(function () {
                    var nameFolderDelete = $("#deleteFolder").val();
                    Request.invokePluginService("Demo", "CreateFolderService", {
                        requestParams: {
                            stringAction: "deleteFolder",
                            nameFolderDelete: nameFolderDelete,
                        },
                        requestCompleteCallback: lang.hitch(this, function (response) {
                            $("#textResponse").text(response.messageResponse);
                        })
                    });
                });

                this.logEntry("loadContent");

                if (!this.isLoaded) {
                    /**
                     * Add custom load logic here. The LaunchBarContainer widget will call this method when the user
                     * clicks on the launch button associated with this feature.
                     */
                    this.isLoaded = true;
                    this.needReset = false;
                }

                this.logExit("loadContent");
            },

            /**
             * Resets the content of this pane.
             */
            reset: function () {
                this.logEntry("reset");

                /**
                 * This is an option method that allows you to force the LaunchBarContainer to reset when the user
                 * clicks on the launch button associated with this feature.
                 */
                this.needReset = false;

                this.logExit("reset");
            },

        });
    });
