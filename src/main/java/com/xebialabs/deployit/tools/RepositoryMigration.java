/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
*/

package com.xebialabs.deployit.tools;

import java.io.File;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Properties;
import org.apache.jackrabbit.core.RepositoryCopier;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import com.google.common.io.Files;

public class RepositoryMigration {

    final File deployitHomeDirectory;
    final File repositoryConfigurationFile;
    final String repositoryName;
    private final File newTargetRepositoryDirectory;
    final boolean updateConfiguration;

    public RepositoryMigration(final RepositoryMigrationOptions options) {
        this.deployitHomeDirectory = new File(options.getDeployitHome());
        this.repositoryConfigurationFile = new File(options.getJackrabbitConfigFile());
        this.repositoryName = options.getRepositoryName();
        this.updateConfiguration = options.isUpdateDeployitConfiguration();
        this.newTargetRepositoryDirectory = new File(deployitHomeDirectory, repositoryName);

        System.out.println("\tdeployitHomeDirectory = " + deployitHomeDirectory);
        System.out.println("\trepositoryConfigurationFile = " + repositoryConfigurationFile);
        System.out.println("\trepositoryName = " + repositoryName);
        System.out.println("\tupdateConfiguration = " + updateConfiguration);
        System.out.println("\tnewTargetRepositoryDirectory = " + newTargetRepositoryDirectory);

    }

    public static void main(String[] args) throws Exception {

        final RepositoryMigrationOptions options = RepositoryMigrationOptions.parseCommandLine(args);
        if (options == null) {
            return;
        }
        new RepositoryMigration(options).migrate();
    }

    private void migrate() throws Exception {
        System.out.println("Start the migration");
        final long start = System.currentTimeMillis();

        if (newTargetRepositoryDirectory.exists()) {
            throw new RuntimeException("The new target repository " + newTargetRepositoryDirectory.getAbsolutePath() + " exists. Stop the migration process.");
        }

        newTargetRepositoryDirectory.mkdirs();

        RepositoryConfig source = RepositoryConfig.create(getConfigurationPath(deployitHomeDirectory), getRepositoryPath(deployitHomeDirectory));
        RepositoryConfig target = RepositoryConfig.create(repositoryConfigurationFile, newTargetRepositoryDirectory);

        RepositoryCopier.copy(source, target);
        final long stop = System.currentTimeMillis();

        if (updateConfiguration) {
            System.out.println("Update the configuration");

            long timestamp = System.currentTimeMillis();
            final File jackrabbitBackupFile = new File(getConfigurationPath(deployitHomeDirectory).getParentFile(), "jackrabbit-repository.xml." + timestamp);
            System.out.println(" backup the previous jackrabbit configuration file " + jackrabbitBackupFile);
            Files.copy(getConfigurationPath(deployitHomeDirectory), jackrabbitBackupFile);

            final File deployitConfigurationFile = getDeployitConfigurationFile(deployitHomeDirectory);
            System.out.println(" backup the previous deployit configuration file " + deployitConfigurationFile);
            final File deployitConfBackupFile = new File(deployitConfigurationFile.getParentFile(), "deployit.conf." + timestamp);
            Files.copy(deployitConfigurationFile, deployitConfBackupFile);


            System.out.println(" copy jackrabbit configuration file to conf dir");
            Files.copy(repositoryConfigurationFile, getConfigurationPath(deployitHomeDirectory));

            System.out.println(" update deployit configuration file");
            java.util.Properties deployitProperties = new Properties();
            deployitProperties.load(Files.newReader(deployitConfigurationFile, Charset.defaultCharset()));
            deployitProperties.setProperty("jcr.repository.path", newTargetRepositoryDirectory.getName());
            final Writer bufferedWriter = Files.newWriter(deployitConfigurationFile, Charset.defaultCharset());
            deployitProperties.store(bufferedWriter, "Generated by Deployit Repository Migration Tools");

        }
        System.out.println("Done " + ((stop - start) / 1000) + " seconds");

    }

    private File getConfigurationPath(final File sourceDir) {
        return new File(new File(sourceDir, "conf"), "jackrabbit-repository.xml");
    }

    private File getDeployitConfigurationFile(final File sourceDir) {
        return new File(new File(sourceDir, "conf"), "deployit.conf");
    }


    private File getRepositoryPath(final File sourceDir) {
        return new File(sourceDir, "repository");
    }


}
