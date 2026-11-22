/*
Copyright 2026 BarD Software s.r.o

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.sourceforge.ganttproject;

import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.roles.RoleSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The permissions of the options file which {@link GanttOptions#save()} writes.
 *
 * The file holds the GP Cloud auth token, the GP Cloud user id and the WebDAV server list, and it is
 * created by a plain {@code FileOutputStream}, which leaves it at whatever the umask allows. With the
 * usual umask of 0022 that is 0644, and every other user of the machine reads the token.
 *
 * The second half of these tests is about the platform which cannot do any of this: on a filesystem
 * without POSIX permissions {@code Files.setPosixFilePermissions} throws, and losing the options
 * because a file mode could not be set would be a worse bug than the one being fixed.
 */
public class OptionsFilePermissionsTest {
  private String myOriginalUserHome;

  @BeforeEach
  public void pointUserHomeAtScratchDirectory(@TempDir Path home) {
    myOriginalUserHome = System.getProperty("user.home");
    System.setProperty("user.home", home.toString());
  }

  @AfterEach
  public void restoreUserHome() {
    System.setProperty("user.home", myOriginalUserHome);
  }

  /** The real save() on this machine: the file comes out readable by its owner and nobody else. */
  @Test
  public void savedOptionsFileIsReadableByItsOwnerOnly() throws IOException {
    GanttOptions options = newOptions();

    options.save();

    File file = GanttOptions.getOptionsFile();
    assertTrue(file.exists(), "save() wrote no options file at all to " + file);
    assertEquals("rw-------", modeOf(file),
        "the options file holds the cloud auth token and is open to other users of the machine");
    assertCompleteOptionsFile(file);
  }

  /**
   * The control for the test above: a file created the way save() used to create it, in the same
   * directory and in the same run, is NOT owner-only. Without this the assertion above would also
   * pass on a machine whose umask happens to be 0077, and it would prove nothing.
   */
  @Test
  public void aPlainFileOutputStreamInTheSameDirectoryIsNotOwnerOnly() throws IOException {
    File control = new File(System.getProperty("user.home"), "umask-control");
    try (OutputStream out = new FileOutputStream(control)) {
      out.write(42);
    }

    assertNotEquals("rw-------", modeOf(control),
        "the umask of this run is already 0077, so the test above cannot tell the fix from its "
            + "absence. Run the tests with umask 0022.");
  }

  /**
   * Windows, and every other filesystem without POSIX permissions: the platform call throws
   * {@link UnsupportedOperationException}, and the options still have to reach the disk whole. The
   * throw is put in by hand because a POSIX machine cannot produce it.
   */
  @Test
  public void optionsAreSavedWholeWhereThePlatformCannotSetPermissions() throws IOException {
    GanttOptions options = newOptionsWhosePermissionCallThrows(
        new UnsupportedOperationException("posix:permissions not supported"));

    options.save();

    File file = GanttOptions.getOptionsFile();
    assertTrue(file.exists(), "the options were lost because the file mode could not be set");
    assertCompleteOptionsFile(file);
    assertEquals(modeOfAPlainlyCreatedFile(), modeOf(file),
        "the file mode was changed although the platform call failed");
  }

  /** The same for an I/O failure: a read-only mount, a share which ignores modes. */
  @Test
  public void optionsAreSavedWholeWhenThePermissionCallFails() throws IOException {
    GanttOptions options = newOptionsWhosePermissionCallThrows(new IOException("read-only file system"));

    options.save();

    File file = GanttOptions.getOptionsFile();
    assertTrue(file.exists(), "the options were lost because the file mode could not be set");
    assertCompleteOptionsFile(file);
  }

  private void assertCompleteOptionsFile(File file) throws IOException {
    String xml = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    assertTrue(xml.contains("<ganttproject-options"), "no options document in the file: " + xml);
    assertTrue(xml.trim().endsWith("</ganttproject-options>"),
        "the options document is cut off, so the copy did not finish: " + xml);
  }

  /** The mode as `ls -l` writes it, so that a failure says `rw-r--r--` and not a set of enums. */
  private String modeOf(File file) throws IOException {
    return PosixFilePermissions.toString(Files.getPosixFilePermissions(file.toPath()));
  }

  /** What a file in this directory looks like when nobody narrows it -- measured, not assumed. */
  private String modeOfAPlainlyCreatedFile() throws IOException {
    File control = new File(System.getProperty("user.home"), "plain-control");
    try (OutputStream out = new FileOutputStream(control)) {
      out.write(42);
    }
    return modeOf(control);
  }

  private GanttOptions newOptions() {
    return newOptions(null);
  }

  private GanttOptions newOptionsWhosePermissionCallThrows(Exception thrown) {
    return newOptions(thrown);
  }

  /**
   * A GanttOptions with the two collaborators which doSave reads. With {@code thrown} given, the
   * platform call which narrows the file throws it instead of running -- that is the only way to
   * reach the non-POSIX branch from a POSIX machine.
   */
  private GanttOptions newOptions(Exception thrown) {
    RoleManager roleManager = createNiceMock(RoleManager.class);
    expect(roleManager.getRoleSets()).andStubReturn(new RoleSet[0]);
    DocumentManager documentManager = createNiceMock(DocumentManager.class);
    expect(documentManager.getRecentDocuments()).andStubReturn(Collections.emptyList());
    replay(roleManager, documentManager);

    GanttOptions options = thrown == null
        ? new GanttOptions(roleManager, documentManager, false)
        : new GanttOptions(roleManager, documentManager, false) {
          @Override
          void setOwnerOnlyPermissions(File file) throws IOException {
            if (thrown instanceof IOException) {
              throw (IOException) thrown;
            }
            throw (RuntimeException) thrown;
          }
        };
    // Creates the UI configuration which doSave reads; the real application sets it from outside.
    options.getUIConfiguration();
    return options;
  }
}
