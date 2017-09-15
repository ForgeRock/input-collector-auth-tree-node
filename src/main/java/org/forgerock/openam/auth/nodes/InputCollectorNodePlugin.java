/*
 * jon.knight@forgerock.com
 *
 * Needed to register the node
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Arrays.asList;
import static org.forgerock.openam.core.realms.Realm.root;

import javax.inject.Inject;

import org.forgerock.openam.auth.node.api.AbstractNodeAmPlugin;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.plugins.PluginException;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

/**
 * Core nodes installed by default with no engine dependencies.
 */
public class InputCollectorNodePlugin extends AbstractNodeAmPlugin {

    private final AnnotatedServiceRegistry serviceRegistry;

    /**
     * DI-enabled constructor.
     * @param serviceRegistry A service registry instance.
     */
    @Inject
    public InputCollectorNodePlugin(AnnotatedServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public String getPluginVersion() {
        return "1.0.0";
    }

    @Override
    public void onStartup() throws PluginException {
        for (Class<? extends Node> nodeClass : getNodes()) {
            pluginTools.registerAuthNode(nodeClass);
        }
    }

    @Override
    protected Iterable<? extends Class<? extends Node>> getNodes() {
        return asList(
                InputCollectorNode.class
        );
    }
}
