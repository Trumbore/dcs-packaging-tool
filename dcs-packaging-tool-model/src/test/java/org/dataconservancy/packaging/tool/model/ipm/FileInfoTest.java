package org.dataconservancy.packaging.tool.model.ipm;

/*
 * Copyright 2015 Johns Hopkins University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class FileInfoTest {

    @ClassRule
    public static TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void equalsTest() {
        EqualsVerifier
            .forClass(FileInfo.class)
            .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
            .verify();
    }

    /**
     * Simply tests that both SHA1 and MD5 checksums are generated when a file info object is created.
     * Since other code is responsible for generating the checksums the validity of the checksums are not tested here.
     * @throws IOException
     */
    @Test
    public void testChecksumsGeneratedForFile() throws IOException {
        File tmpFile = tmpFolder.newFile("testFile");
        FileInfo info = new FileInfo(tmpFile.toPath());

        assertNotNull(info.getChecksum(FileInfo.Algorithm.SHA1));
        assertNotNull(info.getChecksum(FileInfo.Algorithm.MD5));
    }

    /**
     * Tests that checksums are not generated for a directory and the method to get checksums returns correctly.
     * @throws IOException
     */
    @Test
    public void testChecksumsNotGeneratedForDirectory() throws IOException {
        File tmpFile = tmpFolder.newFolder("testFolder");
        FileInfo info = new FileInfo(tmpFile.toPath());

        assertNull(info.getChecksum(FileInfo.Algorithm.SHA1));
        assertNull(info.getChecksum(FileInfo.Algorithm.MD5));
    }

    /**
     * Tests that formats are correctly generated for a file.
     * Since the code that actually generates a file is located elsewhere, the validity of the formats detected are not tested here.
     * @throws IOException
     */
    @Test
    public void testFormatsGeneratedForFile() throws IOException {
        File textFile = tmpFolder.newFile("textFile.txt");

        FileInfo info = new FileInfo(textFile.toPath());

        assertNotNull(info.getFormats());
        assertFalse(info.getFormats().isEmpty());
    }

    /**
     * Tests that formats are not generated for a directory, and that the getFormats method returns correctly.
     * @throws IOException
     */
    @Test
    public void testFormatsNotGeneratedForDirectory() throws IOException {
        File tempFolder = tmpFolder.newFolder("tempFolder");

        FileInfo info = new FileInfo(tempFolder.toPath());
        assertNull(info.getFormats());
    }
}
