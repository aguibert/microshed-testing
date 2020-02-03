/*
 * Copyright (c) 2020 IBM Corporation and others
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
package org.microshed.testing.quarkus;

import java.util.List;
import java.util.stream.Collectors;

import org.microshed.testing.ApplicationEnvironment;
import org.microshed.testing.ManuallyStartedConfiguration;
import org.microshed.testing.testcontainers.config.TestcontainersConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

public class QuarkusConfiguration extends TestcontainersConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(QuarkusConfiguration.class);

    @Override
    public boolean isAvailable() {
        try {
            Class.forName("io.quarkus.test.junit.QuarkusTest");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int getPriority() {
        return ApplicationEnvironment.DEFAULT_PRIORITY - 5;
    }

    @Override
    public boolean configureRestAssured() {
        return false;
    }

    @Override
    public String getApplicationURL() {
        try {
            Class<?> TestHTTPResourceManager = Class.forName("io.quarkus.test.common.http.TestHTTPResourceManager");
            String testUrl = (String) TestHTTPResourceManager.getMethod("getUri").invoke(null);
            return testUrl;
        } catch (Exception e) {
            if (LOG.isDebugEnabled())
                LOG.debug("Unable to determine Quarkus application URL", e);
            return "";
        }
    }

    @Override
    public void applyConfiguration(Class<?> testClass) {
        ManuallyStartedConfiguration.setRuntimeURL(getApplicationURL());
        super.applyConfiguration(testClass);
    }

    @Override
    public void start() {
        super.start();
        autoConfigureDatabases();
        autoConfigureKafka();
        autoConfigureMongoDB();
    }

    private void autoConfigureDatabases() {
        if (System.getProperty("quarkus.datasource.url") != null ||
            System.getProperty("quarkus.datasource.username") != null ||
            System.getProperty("quarkus.datasource.password") != null)
            return; // Do not override explicit configuration
        try {
            Class<?> JdbcContainerClass = Class.forName("org.testcontainers.containers.JdbcDatabaseContainer");
            List<GenericContainer<?>> jdbcContainers = allContainers().stream()
                            .filter(c -> JdbcContainerClass.isAssignableFrom(c.getClass()))
                            .collect(Collectors.toList());
            if (jdbcContainers.size() == 1) {
                GenericContainer<?> db = jdbcContainers.get(0);
                String jdbcUrl = (String) JdbcContainerClass.getMethod("getJdbcUrl").invoke(db);
                System.setProperty("quarkus.datasource.url", jdbcUrl);
                System.setProperty("quarkus.datasource.username", (String) JdbcContainerClass.getMethod("getUsername").invoke(db));
                System.setProperty("quarkus.datasource.password", (String) JdbcContainerClass.getMethod("getPassword").invoke(db));
                if (LOG.isInfoEnabled())
                    LOG.info("Set quarkus.datasource.url to: " + jdbcUrl);
            } else if (jdbcContainers.size() > 1) {
                if (LOG.isInfoEnabled())
                    LOG.info("Located multiple JdbcDatabaseContainer instances. Unable to auto configure quarkus.datasource.* properties");
            } else {
                if (LOG.isDebugEnabled())
                    LOG.debug("No JdbcDatabaseContainer instances found in configuration");
            }
        } catch (ClassNotFoundException | LinkageError ignore) {
            // Testcontainers JDBC not on the classpath
        } catch (Exception e) {
            LOG.debug("Unable to configure Quarkus with JDBC container", e);
        }
    }

    private void autoConfigureKafka() {
        if (System.getProperty("quarkus.kafka.bootstrap-servers") != null)
            return; // Do not override explicit configuration
        try {
            Class<?> KafkaContainerClass = Class.forName("org.testcontainers.containers.KafkaContainer");
            List<GenericContainer<?>> kafkaContainers = allContainers().stream()
                            .filter(c -> KafkaContainerClass.isAssignableFrom(c.getClass()))
                            .collect(Collectors.toList());
            if (kafkaContainers.size() == 1) {
                GenericContainer<?> kafka = kafkaContainers.get(0);
                String bootstrapServers = (String) KafkaContainerClass.getMethod("getBootstrapServers").invoke(kafka);
                System.setProperty("quarkus.kafka.bootstrap-servers", bootstrapServers);
                if (LOG.isInfoEnabled())
                    LOG.info("Set quarkus.kafka.bootstrap-servers=" + bootstrapServers);
            } else if (kafkaContainers.size() > 1) {
                if (LOG.isInfoEnabled())
                    LOG.info("Located multiple KafkaContainer instances. Unable to auto configure 'quarkus.kafka.bootstrap-servers' property");
            } else {
                if (LOG.isDebugEnabled())
                    LOG.debug("No KafkaContainer instances found in configuration");
            }
        } catch (ClassNotFoundException | LinkageError ignore) {
            // Testcontainers Kafka not on the classpath
        } catch (Exception e) {
            LOG.debug("Unable to configure Quarkus with Kafka container", e);
        }
    }

    private void autoConfigureMongoDB() {
        if (System.getProperty("quarkus.mongodb.connection-string") != null ||
            System.getProperty("quarkus.mongodb.hosts") != null)
            return; // Do not override explicit configuration
        try {
            List<GenericContainer<?>> mongoContainers = allContainers().stream()
                            .filter(c -> c.getClass().equals(GenericContainer.class))
                            .filter(c -> c.getDockerImageName().startsWith("mongo:"))
                            .collect(Collectors.toList());
            if (mongoContainers.size() == 1) {
                GenericContainer<?> mongo = mongoContainers.get(0);
                String mongoHost = mongo.getContainerIpAddress() + ':' + mongo.getFirstMappedPort();
                System.setProperty("quarkus.mongodb.hosts", mongoHost);
                if (LOG.isInfoEnabled())
                    LOG.info("Set quarkus.mongodb.hosts=" + mongoHost);
            } else if (mongoContainers.size() > 1) {
                if (LOG.isInfoEnabled())
                    LOG.info("Located multiple MongoDB instances. Unable to auto configure 'quarkus.mongodb.hosts' property");
            } else {
                if (LOG.isDebugEnabled())
                    LOG.debug("No KafkaContainer instances found in configuration");
            }
        } catch (Exception e) {
            LOG.debug("Unable to configure Quarkus with MongoDB container", e);
        }
    }

}
