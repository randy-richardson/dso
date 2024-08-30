/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is
 *      Terracotta, Inc., a Software AG company
 */
package org.terracotta.kit;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;

public class EhcacheAllIT {

    @Test
    public void testEhcacheAllManifest() throws IOException, ClassNotFoundException {
        String version = System.getProperty("project.version");
        Path path = Paths.get("target", "terracotta-" + version, "terracotta-" + version, "apis", "ehcache-terracotta-client-all.jar");
        System.out.println(path.toAbsolutePath());

        try (URLClassLoader urlClassLoader = URLClassLoader.newInstance(new URL[] {path.toUri().toURL()}, null)) {
            Assert.assertNotNull(urlClassLoader.findResource("Version.info"));
            urlClassLoader.loadClass("net.sf.ehcache.Cache");
            urlClassLoader.loadClass("org.slf4j.Logger");
        }
    }
}
