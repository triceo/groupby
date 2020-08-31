/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.examples.cloudbalancing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.security.AnyTypePermission;

final class SolutionIO<Solution_> {

    protected XStream xStream;

    public SolutionIO(Class... xStreamAnnotatedClasses) {
        xStream = new XStream();
        xStream.setMode(XStream.ID_REFERENCES);
        xStream.processAnnotations(xStreamAnnotatedClasses);
        XStream.setupDefaultSecurity(xStream);
        // Presume the XML file comes from a trusted source so it works out of the box. See class javadoc.
        xStream.addPermission(new AnyTypePermission());
    }

    public XStream getXStream() {
        return xStream;
    }

    public String getInputFileExtension() {
        return "xml";
    }

    public Solution_ read(File inputSolutionFile) {
        try (InputStream inputSolutionStream = Files.newInputStream(inputSolutionFile.toPath())) {
            return read(inputSolutionStream);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed reading inputSolutionFile (" + inputSolutionFile + ").", e);
        }
    }

    public Solution_ read(InputStream inputSolutionStream) {
        // xStream.fromXml(InputStream) does not use UTF-8
        try (Reader reader = new InputStreamReader(inputSolutionStream, StandardCharsets.UTF_8)) {
            return (Solution_) xStream.fromXML(reader);
        } catch (XStreamException | IOException e) {
            throw new IllegalArgumentException("Failed reading inputSolutionStream.", e);
        }
    }

    public void write(Solution_ solution, File outputSolutionFile) {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputSolutionFile), StandardCharsets.UTF_8)) {
            xStream.toXML(solution, writer);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed writing outputSolutionFile (" + outputSolutionFile + ").", e);
        }
    }
}
