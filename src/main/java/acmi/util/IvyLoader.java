/*
 * Copyright (c) 2018 acmi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package acmi.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.resolver.ChainResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.IBiblioResolver;
import org.apache.ivy.util.DefaultMessageLogger;
import org.apache.ivy.util.Message;

import lombok.SneakyThrows;

class IvyLoader {

    private static final String CACHE_DIR = new File(System.getProperty("ivy.cache.dir", "./lib")).getAbsolutePath();
    private static final int LOG_LEVEL = Integer.parseInt(System.getProperty("ivy.log.level", String.valueOf(Message.MSG_WARN)));

    static {
        Message.setDefaultLogger(new DefaultMessageLogger(LOG_LEVEL));

        System.setProperty("ivy.cache.resolution", CACHE_DIR);
        System.setProperty("ivy.cache.dir", CACHE_DIR);
        System.setProperty("ivy.cache.repository", CACHE_DIR);
        System.setProperty("ivy.cache.ttl.default", "eternal");
        System.setProperty("ivy.checksums", "");
    }

    private final Ivy ivy;

    IvyLoader(List<String> repos) {
        DependencyResolver resolver = buildResolver(repos);

        IvySettings settings = new IvySettings();
        settings.addResolver(resolver);
        settings.setDefaultResolver(resolver.getName());

        ivy = Ivy.newInstance(settings);
    }

    private static DependencyResolver buildResolver(List<String> repos) {
        ChainResolver resolver = new ChainResolver();
        resolver.setName("resolver");

        for (int i = 0; i < repos.size(); i++) {
            IBiblioResolver repo = new IBiblioResolver();
            repo.setName("repo_" + i);
            repo.setM2compatible(true);
            repo.setRoot(repos.get(i));
            resolver.add(repo);
        }

        IBiblioResolver central = new IBiblioResolver();
        central.setName("central");
        central.setM2compatible(true);
        resolver.add(central);

        return resolver;
    }

    @SneakyThrows({ParseException.class, IOException.class})
    List<URI> resolve(String group, String name, String version) {
        DefaultModuleDescriptor moduleDescriptor = DefaultModuleDescriptor.newDefaultInstance(
                ModuleRevisionId.newInstance(
                        group,
                        name + "-module",
                        version
                ));

        DefaultDependencyDescriptor dependencyDescriptor = new DefaultDependencyDescriptor(
                moduleDescriptor,
                ModuleRevisionId.newInstance(group, name, version),
                true,
                false,
                true
        );
        dependencyDescriptor.addDependencyConfiguration("default", "*");

        moduleDescriptor.addDependency(dependencyDescriptor);


        ResolveOptions resolveOptions = new ResolveOptions();
        resolveOptions.setValidate(false);

        ResolveReport r = ivy.resolve(moduleDescriptor, resolveOptions);
        if (r.hasError()) {
            //noinspection unchecked
            throw new ResolveException(((List<String>) r.getAllProblemMessages())
                    .stream()
                    .map(ResolveException.Entry::parse)
                    .collect(Collectors.toList()));
        }
        return Arrays.stream(r.getAllArtifactsReports())
                .map(ArtifactDownloadReport::getLocalFile)
                .map(File::toURI)
                .collect(Collectors.toList());
    }
}
