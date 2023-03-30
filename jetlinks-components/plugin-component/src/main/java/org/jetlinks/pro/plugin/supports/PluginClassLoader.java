package org.jetlinks.pro.plugin.supports;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

public class PluginClassLoader extends URLClassLoader {

    public PluginClassLoader(String location, ClassLoader parent) throws Exception {
        super(new URL[]{new URL(location)}, parent);
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

}