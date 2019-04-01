/*
 * jon.knight@forgerock.com
 *
 * Sets user profile attributes 
 *
 */

/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2017 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import com.google.inject.assistedinject.Assisted;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;

import javax.inject.Inject;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import static org.forgerock.openam.auth.node.api.Action.send;
//import org.forgerock.guava.common.base.Strings;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        default String variable() { return "variable"; }

        @Attribute(order = 200)
        default String prompt() { return "Prompt"; }

        @Attribute(order = 300)
        default Boolean isPassword() {return false;}

        @Attribute(order = 400)
        default Boolean useTransient() {return true;}
    }

    private static final String BUNDLE = "org/forgerock/openam/auth/nodes/InputCollectorNode";
    private final Logger logger = LoggerFactory.getLogger("amAuth");

    private final InputCollectorNode.Config config;

    /**
     * Constructs a new SetSessionPropertiesNode instance.
     * @param config Node configuration.
     */
    @Inject
    public InputCollectorNode(@Assisted InputCollectorNode.Config config) {
        this.config = config;
    }


    @Override
    public Action process(TreeContext context) {

        String prompt = config.prompt();
        if ((prompt.indexOf("{{") == 0) && (prompt.indexOf("}}") == (prompt.length()-2))) {
            prompt = context.sharedState.get(prompt.substring(2,prompt.length()-2)).asString();
            logger.debug("[InputCollectorNode]: Found existing shared state attribute " + prompt);
        }

        final String promptName = prompt;

        if (config.isPassword()) {
            return context.getCallback(PasswordCallback.class)
                    .map(PasswordCallback::getPassword)
                    .map(String::new)
                    .filter(password -> !Strings.isNullOrEmpty(password))
                    .map(password -> {
                        if (config.useTransient()) {
                            logger.debug("[InputCollectorNode]: Storing user password input in transient shared state " + config.variable());
                            return goToNext().replaceTransientState(context.transientState.copy().put(config.variable(), password)).build();
                        }
                        else {
                            logger.debug("[InputCollectorNode]: Storing user password input in shared state " + config.variable());
                            return goToNext().replaceSharedState(context.sharedState.copy().put(config.variable(), password)).build();
                        }
                    })
                    .orElseGet(() -> {
                        logger.debug("[InputCollectorNode]: Sending password callback: " + promptName);
                        return send(new PasswordCallback(promptName,false)).build();
                    });
        } else {

            return context.getCallback(NameCallback.class)
                    .map(NameCallback::getName)
                    .map(name -> {
                        logger.debug("[InputCollectorNode]: Storing user input in shared state " + config.variable());
                        return goToNext().replaceSharedState(context.sharedState.copy().put(config.variable(), name)).build();
                    })
                    .orElseGet(() -> {
                        logger.debug("[InputCollectorNode]: Sending name callback: " + promptName);
                        return send(new NameCallback(promptName)).build();
                    });
        }

    }
}