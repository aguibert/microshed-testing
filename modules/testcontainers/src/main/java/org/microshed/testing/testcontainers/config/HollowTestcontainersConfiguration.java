/*
 * Copyright (c) 2019 IBM Corporation and others
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.microshed.testing.testcontainers.config;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.microshed.testing.ApplicationEnvironment;
import org.microshed.testing.ManuallyStartedConfiguration;
import org.microshed.testing.testcontainers.ApplicationContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

public class HollowTestcontainersConfiguration extends TestcontainersConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(HollowTestcontainersConfiguration.class);

    public static boolean available() {
        String host = resolveProperty(ManuallyStartedConfiguration.MICROSHED_HOSTNAME);
        String httpPort = resolveProperty(ManuallyStartedConfiguration.MICROSHED_HTTP_PORT);
        String httpsPort = resolveProperty(ManuallyStartedConfiguration.MICROSHED_HTTPS_PORT);
        return !host.isEmpty() && (!httpPort.isEmpty() || !httpsPort.isEmpty());
    }

    private static String resolveProperty(String key) {
        String value = System.getProperty(key, System.getenv(key));
        return value == null ? "" : value;
    }

    @Override
    public boolean isAvailable() {
        return available();
    }

    @Override
    public int getPriority() {
        return ApplicationEnvironment.DEFAULT_PRIORITY - 20;
    }

    @Override
    public void applyConfiguration(Class<?> testClass) {
        super.applyConfiguration(testClass);

        // Translate any Docker network hosts that may have been configured in environment variables
        Set<String> networkAliases = allContainers().stream()
                        .filter(c -> !(c instanceof ApplicationContainer))
                        .flatMap(c -> c.getNetworkAliases().stream())
                        .collect(Collectors.toSet());
        allContainers().stream()
                        .filter(c -> c instanceof ApplicationContainer)
                        .map(c -> (ApplicationContainer) c)
                        .forEach(mpApp -> sanitizeEnvVar(mpApp, networkAliases));

        // Expose any external resources (such as DBs) on fixed exposed ports
        try {
            Method addFixedPort = GenericContainer.class.getDeclaredMethod("addFixedExposedPort", int.class, int.class);
            addFixedPort.setAccessible(true);
            Map<Integer, String> fixedExposedPorts = new HashMap<>();
            for (GenericContainer<?> c : allContainers()) {
                for (Integer p : c.getExposedPorts()) {
                    if (fixedExposedPorts.containsKey(p)) {
                        throw new ExtensionConfigurationException("Cannot expose port " + p + " for " + c.getDockerImageName() +
                                                                  " because another container (" + fixedExposedPorts.get(p) +
                                                                  ") is already using it.");
                    }
                    LOG.info("Exposing fixed port " + p + " for container " + c.getDockerImageName());
                    fixedExposedPorts.put(p, c.getDockerImageName());
                    addFixedPort.invoke(c, p, p);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Apply configuration to a running server
        URL appURL;
        String runtimeURL = getApplicationURL();
        try {
            appURL = new URL(runtimeURL);
        } catch (MalformedURLException e) {
            throw new ExtensionConfigurationException("The application URL '" + runtimeURL + "' was not a valid URL.", e);
        }
        allContainers().stream()
                        .filter(c -> c instanceof ApplicationContainer)
                        .map(c -> (ApplicationContainer) c)
                        .forEach(c -> c.setRunningURL(appURL));
    }

    @Override
    public String getApplicationURL() {
        return ManuallyStartedConfiguration.getRuntimeURL();
    }

    /**
     * Attempt to translate any environment variables such as:
     * FOO_HOSTNAME=http://foo:8080
     * to accomodate for the fixed exposed port such as:
     * FOO_HOSTNAME=http://localhost:8080
     */
    private void sanitizeEnvVar(ApplicationContainer mpApp, Set<String> networkAliases) {
        mpApp.getEnvMap().forEach((k, v) -> {
            URL url = null;
            try {
                url = new URL(v);
            } catch (MalformedURLException e1) {
                try {
                    url = new URL("http://" + v);
                } catch (MalformedURLException e2) {
                    return;
                }
            }
            for (String networkAlias : networkAliases) {
                if (url.getHost().equals(networkAlias)) {
                    String newValue = v.replaceFirst(networkAlias, "localhost");
                    LOG.info("Translating env var key=" + k + " from " + v + " to " + newValue);
                    mpApp.withEnv(k, newValue);
                }
            }
        });
    }
}
