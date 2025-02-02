/*
 * Copyright (c) 2017-2019 AxonIQ B.V. and/or licensed to AxonIQ B.V.
 * under one or more contributor license agreements.
 *
 *  Licensed under the AxonIQ Open Source License Agreement v1.0;
 *  you may not use this file except in compliance with the license.
 *
 */

package io.axoniq.cli;

import io.axoniq.cli.json.Application;
import org.apache.commons.cli.CommandLine;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;

/**
 * @author Marc Gathier
 */
public class RegisterApplication extends AxonIQCliCommand {

    private static final int MIN_LENGTH = 16;

    public static void run(String[] args) throws IOException {
        // check args
        CommandLine commandLine = processCommandLine(args[0], args, CommandOptions.APPLICATION,
                CommandOptions.ROLES, CommandOptions.DESCRIPTION, CommandOptions.TOKEN, CommandOptions.SET_TOKEN);
        String url = createUrl(commandLine, "/v1/applications");

        if( commandLine.hasOption(CommandOptions.SET_TOKEN.getOpt())) {
            if( commandLine.getOptionValue(CommandOptions.SET_TOKEN.getOpt()).length() < MIN_LENGTH) {
                System.err.printf("Token must be at least %d characters", MIN_LENGTH);
                return;
            }
        }

        Application application = new Application(commandLine.getOptionValue(CommandOptions.APPLICATION.getOpt()),
                                                  commandLine.getOptionValue(CommandOptions.DESCRIPTION.getOpt()),
                                                  commandLine.getOptionValue(CommandOptions.SET_TOKEN.getOpt()),
                                                  commandLine.getOptionValues(CommandOptions.ROLES.getOpt()));

        // get http client
        try (CloseableHttpClient httpclient = createClient(commandLine)) {
            String response = postJSON(httpclient, url, application, 200, getToken(commandLine));
            System.out.println("AccessToken is: "+  response);
            System.out.println("Please note this token as this will only be provided once!");
        }
    }
}
