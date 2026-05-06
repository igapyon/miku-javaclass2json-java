package jp.igapyon.mikujavaclass2json.mavenplugin;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import jp.igapyon.mikujavaclass2json.coreapi.ClassIndexGenerator;
import jp.igapyon.mikujavaclass2json.coreapi.ClassIndexOptions;
import jp.igapyon.mikujavaclass2json.coreapi.ClassIndexResult;

@Mojo(name = "index", threadSafe = true)
public class MikuJavaclass2jsonMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project.build.outputDirectory}", property = "miku-javaclass2json.classesDirectory")
    private File classesDirectory;

    @Parameter(defaultValue = "${project.basedir}/.java-class-index", property = "miku-javaclass2json.outputDirectory")
    private File outputDirectory;

    @Parameter(defaultValue = "false", property = "miku-javaclass2json.skip")
    private boolean skip;

    @Parameter(property = "miku-javaclass2json.excludePackages")
    private String[] excludePackages;

    @Parameter(property = "miku-javaclass2json.excludeCallPackages")
    private String[] excludeCallPackages;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("miku-javaclass2json skipped.");
            return;
        }
        try {
            ClassIndexOptions options = new ClassIndexOptions();
            options.setInput(classesDirectory.toPath());
            options.setOutputDirectory(outputDirectory.toPath());
            options.setExcludePackages(excludePackages == null ? null : Arrays.asList(excludePackages));
            options.setExcludeCallPackages(excludeCallPackages == null ? null : Arrays.asList(excludeCallPackages));
            ClassIndexResult result = new ClassIndexGenerator().generate(options);
            getLog().info("indexed classes: " + result.getClassCount());
            getLog().info("output: " + outputDirectory);
        } catch (Exception ex) {
            throw new MojoExecutionException("Failed to run miku-javaclass2json.", ex);
        }
    }

    public void setClassesDirectory(File classesDirectory) {
        this.classesDirectory = classesDirectory;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public void setExcludePackages(String[] excludePackages) {
        this.excludePackages = excludePackages;
    }

    public void setExcludeCallPackages(String[] excludeCallPackages) {
        this.excludeCallPackages = excludeCallPackages;
    }
}
