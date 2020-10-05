package com.pingidentity.sync.pipe.plugin;

import com.unboundid.directory.sdk.sync.api.SyncPipePlugin;
import com.unboundid.directory.sdk.sync.config.SyncPipePluginConfig;
import com.unboundid.directory.sdk.sync.types.PostStepResult;
import com.unboundid.directory.sdk.sync.types.SyncOperation;
import com.unboundid.directory.sdk.sync.types.SyncServerContext;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.StringArgument;

import java.util.ArrayList;
import java.util.List;

/**
 * This extension is a simple example to demonstrate applying a custom hash to a select set
 * of attributes configured as an argument
 */
public class CustomHash extends SyncPipePlugin {

    public static final String ARG_NAME_ATTRIBUTE = "attribute-to-hash";
    private List<String> attributesToHash;
    private SyncServerContext serverContext;

    /**
     * This method returns the name that is displayed when administrators interact with
     * this extension such as when the extension is installed or displayed such as in monitoring
     * information
     */
    @Override
    public String getExtensionName() {
        return "Custom Hash Sync Pipe Plugin";
    }

    /**
     * This method returns a description that is displayed when administrators interact with
     * this extension such as when the extension is installed
     */
    @Override
    public String[] getExtensionDescription() {
        return new String[]{"An example extension applying a custom hash on a select set of attributes."};
    }

    /**
     * This method allows to declare arguments the extension may use and their constraints
     * @param parser the argument parser
     * @throws ArgumentException in case the definition for the arguments is in any way invalid
     */
    @Override
    public void defineConfigArguments(ArgumentParser parser) throws ArgumentException {
        parser.addArgument(new StringArgument(null, ARG_NAME_ATTRIBUTE,true,0,"{attribute}","The attribute name to apply the custom hashing to"));
    }

    /**
     * This method applies the configuration such that a change in any arguments is reflected on the
     * instance of the extension at run time
     * @param config the configuration object for the instance of the extension
     * @param parser the argument parser
     * @param adminActionsRequired a list of messages in case administrative action is required for the configuration to be fully applied
     * @param messages a list of messages resulting from applying the configuration to the instance of the extension
     * @return ResultCode.SUCCESS if the configuration was applied successfully to the instance of the extension
     */
    @Override
    public ResultCode applyConfiguration(SyncPipePluginConfig config, ArgumentParser parser, List<String> adminActionsRequired, List<String> messages) {
        attributesToHash = parser.getStringArgument(ARG_NAME_ATTRIBUTE).getValues();
        return ResultCode.SUCCESS;
    }

    /**
     * This method initializes the instance of the extension with what it needs to operate
     * @param serverContext the server context
     * @param config the configuration object associated with the instance of the extension
     * @param parser the argument parser
     * @throws LDAPException if the initialization encountered a problem
     */
    @Override
    public void initializeSyncPipePlugin(SyncServerContext serverContext, SyncPipePluginConfig config, ArgumentParser parser) throws LDAPException {
        this.serverContext = serverContext;
        List<String> messages = new ArrayList<>(3);
        List<String> actions = new ArrayList<>(3);
        ResultCode configurationResult = applyConfiguration(config, parser, actions, messages);
        if (!ResultCode.SUCCESS.equals(configurationResult)) {
            throw new LDAPException(ResultCode.OTHER, String.join(", ",messages));
        }
    }

    /**
     * This method will execute after all the mappings configured have been applied and effect the
     * custom hashing function on the attributes configured
     * @param sourceEntry the original entry at the sourrce, as is, before transformations
     * @param equivalentDestinationEntry the resulting entry once all configured transformations were applied
     * @param operation the sync operation being processed
     * @return CONTINUE
     */
    @Override
    public PostStepResult postMapping(Entry sourceEntry, Entry equivalentDestinationEntry, SyncOperation operation) {
        for (String attributeName: attributesToHash){
            Attribute originalAttribute = equivalentDestinationEntry.getAttribute(attributeName);
            if ( originalAttribute != null ) {
                equivalentDestinationEntry.removeAttribute(originalAttribute.getName());
                Attribute hashedAttribute = hashTheAttribute(originalAttribute);
                equivalentDestinationEntry.addAttribute(hashedAttribute);
            }
        }
        return PostStepResult.CONTINUE;
    }

    /**
     * This method parses the attribute to apply the custom hashing function
     * Note that because this is simply an example, there are several caveats in its current form:
     *   - this will be destructive for attribute options
     *   - it only handles String attributes, all other types will be cast to String and thus hashed incorrectly
     * @param originalAttribute
     * @return the resulting attribute after each value, if any, was hashed
     */
    private Attribute hashTheAttribute(final Attribute originalAttribute){
        if ( originalAttribute == null ) {
            return null;
        }
        String[] originalValues = originalAttribute.getValues();
        List<String> hashedValues = new ArrayList<>(originalValues.length);
        for (String originalValue: originalValues){
            hashedValues.add(hashTheValue(originalValue));
        }
        Attribute hashedAttribute = new Attribute(originalAttribute.getName(),hashedValues);

        return hashedAttribute;
    }

    /**
     * This method hashes the value
     *   beware that this will not work on anything but String attribute values in its current form
     * @param originalValue the original value to hash
     * @return the hashed value
     */
    private String hashTheValue(String originalValue){
        return originalValue;
    }

    @Override
    public void toString(StringBuilder stringBuilder) {
    }
}
