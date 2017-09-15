/*
 * jon.knight@forgerock.com
 *
 * Sets user profile attributes 
 *
 */

/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guava.common.collect.ImmutableList;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import java.util.*;

import static org.forgerock.openam.auth.node.api.Action.send;

/**
 * A node which collects a username from the user via a name callback.
 *
 * <p>Places the result in the shared state as 'username'.</p>
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
            configClass = InputCollectorNode.Config.class)
public class InputCollectorNode extends SingleOutcomeNode {

    public interface Config {
        @Attribute(order = 100)
        Map<String, String> properties();
    }

    private static final String BUNDLE = "org/forgerock/openam/auth/nodes/InputCollectorNode";
    private final static String DEBUG_FILE = "InputCollectorNode";
    protected Debug debug = Debug.getInstance(DEBUG_FILE);

    private final InputCollectorNode.Config config;

    /**
     * Constructs a new SetSessionPropertiesNode instance.
     * @param config Node configuration.
     */
    @Inject
    public InputCollectorNode(@Assisted InputCollectorNode.Config config) {
        this.config = config;
    }


    private String findCallbackValue(TreeContext context, String name) {
        Iterator<? extends Callback> iterator = context.getAllCallbacks().iterator();
        while (iterator.hasNext()) {
            NameCallback ncb = (NameCallback) iterator.next();
            if (name.equals(ncb.getPrompt())) return ncb.getName();
        }
        return "";
    }


    @Override
    public Action process(TreeContext context) {
        JsonValue newSharedState = context.sharedState.copy();
        Set<String> configKeys = config.properties().keySet();
        if (context.hasCallbacks()) {
            for (String key: configKeys) {
                String result = findCallbackValue(context, config.properties().get(key));
                newSharedState.put(key, result);
            }
            return goToNext().replaceSharedState(newSharedState).build();
        } else {
            List<Callback> callbacks = new ArrayList<Callback>(1);
            for (String key : configKeys) {
                NameCallback nameCallback = new NameCallback(config.properties().get(key));
                callbacks.add(nameCallback);
            }
            return send(ImmutableList.copyOf(callbacks)).build();

        }
    }
}
