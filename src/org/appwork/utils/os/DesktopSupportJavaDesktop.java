/**
 *
 * ====================================================================================================================================================
 *         "AppWork Utilities" License
 *         The "AppWork Utilities" will be called [The Product] from now on.
 * ====================================================================================================================================================
 *         Copyright (c) 2009-2015, AppWork GmbH <e-mail@appwork.org>
 *         Schwabacher Straße 117
 *         90763 Fürth
 *         Germany
 * === Preamble ===
 *     This license establishes the terms under which the [The Product] Source Code & Binary files may be used, copied, modified, distributed, and/or redistributed.
 *     The intent is that the AppWork GmbH is able to provide their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 *     These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 *
 * === 3rd Party Licences ===
 *     Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 *     to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header.
 *
 * === Definition: Commercial Usage ===
 *     If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's any commercial interest or aspect in what you are doing, we consider this as a commercial usage.
 *     If your use-case is neither strictly private nor strictly educational, it is commercial. If you are unsure whether your use-case is commercial or not, consider it as commercial or contact us.
 * === Dual Licensing ===
 * === Commercial Usage ===
 *     If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 *     Contact AppWork for further details: <e-mail@appwork.org>
 * === Non-Commercial Usage ===
 *     If there is no commercial usage (see definition above), you may use [The Product] under the terms of the
 *     "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 *
 *     If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.utils.os;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;

import sun.security.action.GetPropertyAction;

/**
 * @author daniel
 *
 */
public class DesktopSupportJavaDesktop implements DesktopSupport {
    private Boolean openFileSupported  = null;
    private Boolean browseURLSupported = null;

    @Override
    public void browseURL(final URL url) throws IOException, URISyntaxException {
        if (this.isBrowseURLSupported()) {
            final Desktop desktop = Desktop.getDesktop();
            desktop.browse(url.toURI());
        } else {
            throw new IOException("browseURL not supported");
        }
    }

    public long[] killProcessesByExecutablePath(String path, int exitCode) throws InterruptedException, NotSupportedException {
        throw new NotSupportedException("Operating System not supported");
    }

    @Override
    public boolean isBrowseURLSupported() {
        if (this.browseURLSupported != null) {
            return this.browseURLSupported;
        }
        if (!Desktop.isDesktopSupported()) {
            this.browseURLSupported = false;
            return false;
        }
        final Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            this.browseURLSupported = false;
            return false;
        }
        this.browseURLSupported = true;
        return true;
    }

    @Override
    public boolean isOpenFileSupported() {
        if (this.openFileSupported != null) {
            return this.openFileSupported;
        }
        if (!Desktop.isDesktopSupported()) {
            this.openFileSupported = false;
            return false;
        }
        final Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.OPEN)) {
            this.openFileSupported = false;
            return false;
        }
        this.openFileSupported = true;
        return true;
    }

    @Override
    public void openFile(final File file) throws IOException {
        if (this.isOpenFileSupported()) {
            final Desktop desktop = Desktop.getDesktop();
            final URI uri = file.getCanonicalFile().toURI();
            desktop.open(new File(uri));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.os.DesktopSupport#shutdown()
     */
    @Override
    public boolean shutdown(boolean force) throws InterruptedException {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.os.DesktopSupport#standby()
     */
    @Override
    public boolean standby() throws InterruptedException {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.os.DesktopSupport#hibernate()
     */
    @Override
    public boolean hibernate() throws InterruptedException {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.os.DesktopSupport#getDefaultDownloadDirectory()
     */
    @Override
    public String getDefaultDownloadDirectory() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.os.DesktopSupport#getProcessExecutablePathByPID(int)
     */
    @Override
    public String getProcessExecutablePathByPID(long pid) throws NotSupportedException, InterruptedException {
        throw new NotSupportedException("Operating System not supported");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.os.DesktopSupport#getProcessCommandlineByPID(int)
     */
    @Override
    public String getProcessCommandlineByPID(long pid) throws NotSupportedException, InterruptedException {
        throw new NotSupportedException("Operating System not supported");
    }

    /**
     * @param c0
     * @return
     */
    private boolean isDriveLetter(char c) {
        return ((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z'));
    }

    private final char SLASH = AccessController.doPrivileged(new GetPropertyAction("file.separator")).charAt(0);

    /*
     * (non-Javadoc) from java.io.WinNTFileSystem.prefixLength(String);
     *
     * @see org.appwork.utils.os.DesktopSupport#getPrefixLength(java.lang.String)
     */
    @Override
    public int getPrefixLength(String path) {
        char slash = SLASH;
        int n = path.length();
        if (n == 0) {
            return 0;
        }
        char c0 = path.charAt(0);
        char c1 = (n > 1) ? path.charAt(1) : 0;
        if (c0 == slash) {
            if (c1 == slash) {
                return 2; /* Absolute UNC pathname "\\\\foo" */
            }
            return 1; /* Drive-relative "\\foo" */
        }
        if (isDriveLetter(c0) && (c1 == ':')) {
            if ((n > 2) && (path.charAt(2) == slash)) {
                return 3; /* Absolute local pathname "z:\\foo" */
            }
            return 2; /* Directory-relative "z:foo" */
        }
        return 0; /* Completely relative */
    }
}
