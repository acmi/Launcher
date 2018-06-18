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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.swing.JOptionPane;

import javafx.application.Application;
import lombok.SneakyThrows;

class Launcher {

    private static final String MSG_ERR_LOAD_PROPERTIES = "Couldn't configure application";
    private static final String MSG_ERR_DOWNLOAD = "Couldn't download application";
    private static final String MSG_ERR_START = "Couldn't start application";

    private static final int MAIN_MODIFIERS = Modifier.PUBLIC | Modifier.STATIC;

    private List<URI> artifacts;

    private String[] repositoryUrls;
    private String applicationGroup;
    private String applicationName;
    private String applicationVersion;
    private String applicationMain;
    private String[] applicationArgs;

    private void loadConfig() {
        repositoryUrls = System.getProperty("repository.url", "").split("#");
        applicationGroup = System.getProperty("application.group");
        applicationName = System.getProperty("application.name");
        applicationVersion = System.getProperty("application.version", "latest.release");
        applicationMain = System.getProperty("application.main");
        String args = System.getProperty("application.args", "").trim();
        applicationArgs = args.isEmpty() ? new String[0] : args.split("\\s+");
    }

    private void download() {
        artifacts = new IvyLoader(Arrays.asList(repositoryUrls))
                .resolve(applicationGroup, applicationName, applicationVersion);
    }

    @SneakyThrows(ReflectiveOperationException.class)
    private void start() {
        ClassLoader classLoader = new URLClassLoader(artifacts.stream().map(Launcher::toUrl).toArray(URL[]::new));
        if (applicationMain == null) {
            findMain();
        }
        Class<?> clazz = Class.forName(applicationMain, true, classLoader);
        if (Application.class.isAssignableFrom(clazz)) {
            Application.launch(clazz.asSubclass(Application.class), applicationArgs);
            return;
        } else {
            Optional<Method> main = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(it -> (it.getModifiers() & MAIN_MODIFIERS) == MAIN_MODIFIERS && Objects.equals(it.getName(), "main"))
                    .findAny();
            if (main.isPresent()) {
                main.get().invoke(null, (Object) applicationArgs);
                return;
            }
        }
        throw new IllegalStateException("Main method not found");
    }

    @SneakyThrows(MalformedURLException.class)
    private static URL toUrl(URI uri) {
        return uri.toURL();
    }

    private void findMain() {
        // TODO
    }

    private static void doOrExitWithMessage(String msg, Action action) {
        try {
            action.run();
        } catch (Exception e) {
            System.err.println(msg);
            e.printStackTrace(System.err);

            JOptionPane.showMessageDialog(null, msg + (e.getMessage() != null ? ": " + e.getMessage() : ""), "Error", JOptionPane.ERROR_MESSAGE);

            System.exit(1);
        }
    }

    private interface Action {
        void run() throws Exception;
    }

    public static void main(String[] args) {
        Launcher launcher = new Launcher();
        doOrExitWithMessage(MSG_ERR_LOAD_PROPERTIES, launcher::loadConfig);
        doOrExitWithMessage(MSG_ERR_DOWNLOAD, launcher::download);
        doOrExitWithMessage(MSG_ERR_START, launcher::start);
    }
}
