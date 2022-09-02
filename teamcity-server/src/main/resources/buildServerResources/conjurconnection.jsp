<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include-internal.jsp" %>
<jsp:useBean id="keys" class="com.cyberark.common.ConjurJspKey"/>
<jsp:useBean id="project" type="jetbrains.buildServer.serverSide.SProject" scope="request"/>
<bs:linkScript>
    /js/bs/testConnection.js
</bs:linkScript>
<style type="text/css">
    .auth-container {
        display: none;
    }
</style>

<script>
    BS.OAuthConnectionDialog.submitTestConnection = function () {
        var that = this;
        BS.PasswordFormSaver.save(this, '<c:url value="/conjur-test-connection.html"/>', OO.extend(BS.ErrorsAwareListener, {
            onFailedTestConnectionError: function (elem) {
                var text = "";
                if (elem.firstChild) {
                    text = elem.firstChild.nodeValue;
                }
                BS.TestConnectionDialog.show(false, text, $('testConnectionButton'));
            },
            onCompleteSave: function (form, responseXML) {
                var err = BS.XMLResponse.processErrors(responseXML, this, form.propertiesErrorsHandler);
                BS.ErrorsAwareListener.onCompleteSave(form, responseXML, err);
                if (!err) {
                    this.onSuccessfulSave(responseXML);
                }
            },
            onSuccessfulSave: function (responseXML) {
                that.enable();
                var additionalInfo = "";
                var testConnectionResultNodes = responseXML.documentElement.getElementsByTagName("testConnectionResult");
                if (testConnectionResultNodes && testConnectionResultNodes.length > 0) {
                    var testConnectionResult = testConnectionResultNodes.item(0);
                    if (testConnectionResult.firstChild) {
                        additionalInfo = testConnectionResult.firstChild.nodeValue;
                    }
                }
                BS.TestConnectionDialog.show(true, additionalInfo, $('testConnectionButton'));
            }
        }));
        return false;
    };
    var afterClose = BS.OAuthConnectionDialog.afterClose;
    BS.OAuthConnectionDialog.afterClose = function () {
        $j('#OAuthConnectionDialog .testConnectionButton').remove();
        afterClose()
    }
</script>

<tr>
    <td><label for="displayName">Display name:</label><l:star/></td>
    <td>
        <props:textProperty name="displayName" className="longField"/>
        <span class="smallNote">Provide some name to distinguish this connection from others.</span>
        <span class="error" id="error_displayName"></span>
    </td>
</tr>

<tr>
    <td><label for="${keys.applianceUrl}">Conjur Appliance URL:</label><l:star/></td>
    <td>
        <props:textProperty name="${keys.applianceUrl}"
                            className="longField textProperty_max-width js_max-width"/>
        <span class="error" id="error_${keys.applianceUrl}"/>
        <span class="smallNote">e.g. https://conjur-follower.company.local</span>
    </td>
</tr>

<tr>
    <td><label for="${keys.account}">Conjur Account:</label><l:star/></td>
    <td>
        <props:textProperty name="${keys.account}"
                            className="longField textProperty_max-width js_max-width"/>
        <span class="error" id="error_${keys.account}"/>
        <span class="smallNote">e.g. companyName</span>
    </td>
</tr>

<tr>
    <td><label for="${keys.authnLogin}">Conjur Authn Login:</label><l:star/></td>
    <td>
        <props:textProperty name="${keys.authnLogin}"
                            className="longField textProperty_max-width js_max-width"/>
        <span class="error" id="error_${keys.authnLogin}"/>
        <span class="smallNote">e.g. host/teamcity/projectName</span>
    </td>
</tr>

<tr>
    <td><label for="${keys.apiKey}">Conjur API Key:</label><l:star/></td>
    <td>
        <props:passwordProperty name="${keys.apiKey}"
                            className="longField textProperty_max-width js_max-width"/>
        <span class="error" id="error_${keys.apiKey}"/>
    </td>
</tr>

<tr>
    <td><label for="${keys.certFile}">Conjur Certificate:</label></td>
    <td>
        <props:multilineProperty expanded="true" name="${keys.certFile}" className="longField textProperty_max-width js_max-width"
                             rows="4" cols="45" linkTitle="Conjur Certificate"/>
        <span class="error" id="error_${keys.certFile}"/>
        <span class="smallNote">The public certificate chain used to establish TLS connection to the Conjur API</span>
    </td>
</tr>

<tr>
    <td><label for="${keys.failOnError}">Fail in case of error</label></td>
    <td>
        <props:checkboxProperty name="${keys.failOnError}"/>
        <span class="error" id="error_${keys.failOnError}"/>
        <span class="smallNote">Whether to fail builds in case of parameter resolving error</span>
    </td>
</tr>

<tr>
    <td><label for="${keys.verboseLogging}">Enable verbose logging</label></td>
    <td>
        <props:checkboxProperty name="${keys.verboseLogging}"/>
        <span class="error" id="error_${keys.verboseLogging}"/>
        <span class="smallNote">Whether to enable verbose logging</span>
    </td>
</tr>

<forms:submit id="testConnectionButton" type="button" label="Test Connection" className="testConnectionButton"
              onclick="return BS.OAuthConnectionDialog.submitTestConnection();"/>
<bs:dialog dialogId="testConnectionDialog" title="Test Connection" closeCommand="BS.TestConnectionDialog.close();"
           closeAttrs="showdiscardchangesmessage='false'">
    <div id="testConnectionStatus"></div>
    <div id="testConnectionDetails" class="mono"></div>
</bs:dialog>
<script>
    $j('#OAuthConnectionDialog .popupSaveButtonsBlock .testConnectionButton').remove();
    $j("#testConnectionButton").appendTo($j('#OAuthConnectionDialog .popupSaveButtonsBlock')[0])
    BS.Vault = {
        onAuthChange: function (element) {
            $j('.auth-container').hide();
            let value = $j(element).val();
            $j('.auth-' + value).show();
            BS.VisibilityHandlers.updateVisibility('mainContent');
        }
    }
    $j(document).ready(function () {
        BS.Vault.onAuthChange($j('input[name="prop:${keys.authnLogin}"]:checked'));
    })
</script>