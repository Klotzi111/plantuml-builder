/*-
 * Plantuml builder
 *
 * Copyright (C) 2017 Focus IT
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
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
package ch.ifocusit.plantuml;

import ch.ifocusit.plantuml.classdiagram.model.Association;
import ch.ifocusit.plantuml.classdiagram.model.Package;
import ch.ifocusit.plantuml.classdiagram.model.attribute.Attribute;
import ch.ifocusit.plantuml.classdiagram.model.clazz.Clazz;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.text.MessageFormat;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ch.ifocusit.plantuml.classdiagram.model.Association.DIRECTION;
import static org.apache.commons.lang3.StringUtils.SPACE;

/**
 * Plantuml diagram helper for java classes.
 *
 * @author Julien Boz
 */
public class PlantUmlBuilder {
    private static final String STARTUML = "@startuml";
    private static final String ENDUML = "@enduml";
    public static final String SEMICOLON = ":";
    public static final String BRACE_OPEN = "{";
    public static final String BRACE_CLOSE = "}";
    public static final String STEREOTYPE_OPEN = "<<";
    public static final String STEREOTYPE_CLOSE = ">>";
    public static final String TAB = SPACE + SPACE;
    public static final String NEWLINE = System.getProperty("line.separator");
    public static final String QUOTE = "\"";
    public static final String HASHTAG = "#";
    public static final String PACKAGE_TMPL = "package {0} <<{1}>>";

    private final StringBuilder content = new StringBuilder();

    public PlantUmlBuilder start() {
        content.append(STARTUML).append(NEWLINE).append(NEWLINE);
        return this;
    }

    public PlantUmlBuilder end() {
        content.append(NEWLINE).append(ENDUML);
        return this;
    }

    public String build() {
        return content.toString();
    }

    private String escape(String value) {
        return QUOTE + value + QUOTE;
    }

    private String color(String color) {
        return HASHTAG + color;
    }

    private PlantUmlBuilder writeClazzDefinition(Clazz clazz) {
        content.append(clazz.getType()).append(SPACE).append(escape(clazz.getName()));
        return this;
    }

    private PlantUmlBuilder append(String s) {
        if (s != null) {
            content.append(s);
        }
        return this;
    }

    //*********************************************************************************
    // HEADER/FOOTER
    //*********************************************************************************

    public PlantUmlBuilder appendPart(String part) {
        if (part != null) {
            content.append(NEWLINE).append(NEWLINE).append(part).append(NEWLINE).append(NEWLINE);
        }
        return this;
    }


    //*********************************************************************************
    // PACKAGE
    //*********************************************************************************

    public PlantUmlBuilder addPackage(Package aPackage, Clazz... classes) {
        Validate.notNull(aPackage, "no package defined !");
        aPackage.validate();
        Validate.notEmpty(classes, String.format("Package '%s' must not be empty !", aPackage.getName()));

        content.append(MessageFormat.format(PACKAGE_TMPL, aPackage.getName(), aPackage.getType()));
        aPackage.getColor().ifPresent(color -> content.append(SPACE).append(color(color)));

        if (classes.length > 0) {
            content.append(SPACE).append(BRACE_OPEN).append(NEWLINE);
            Stream.of(classes).forEach(clazz -> {
                clazz.validate();
                append(TAB).writeClazzDefinition(clazz).append(NEWLINE);
            });
            content.append(BRACE_CLOSE);
        }
        content.append(NEWLINE).append(NEWLINE);

        return this;
    }


    //*********************************************************************************
    // TYPE
    //*********************************************************************************

    public PlantUmlBuilder addType(Clazz clazz) {
        Validate.notNull(clazz, "No class defined !");
        clazz.validate();

        writeClazzDefinition(clazz);
        // stereotype
        clazz.getStereotypes().ifPresent(stereotypes -> content
                .append(SPACE).append(STEREOTYPE_OPEN)
                .append(stereotypes.stream().collect(Collectors.joining(", ")))
                .append(STEREOTYPE_CLOSE));
        // class link
        clazz.getLink().ifPresent(link -> content.append(SPACE).append(link.toString()));
        // class color
        clazz.getBackgroundColor().ifPresent(color -> content.append(SPACE).append(color(color)));
        if (!clazz.getAttributes().isEmpty()) {
            content.append(SPACE).append(BRACE_OPEN).append(NEWLINE);
            for (Attribute attribute : clazz.getAttributes()) {
                // name
                content.append(TAB).append(attribute.getName());
                // type
                attribute.getType().ifPresent(type -> content.append(SPACE).append(SEMICOLON).append(SPACE).append(type));
                // field link
                attribute.getLink().ifPresent(link -> content.append(SPACE).append(link.toString()));
                content.append(NEWLINE);
            }
            content.append(BRACE_CLOSE);
        }
        content.append(NEWLINE).append(NEWLINE);
        return this;
    }

    //*********************************************************************************
    // TYPE ASSOCIATION
    //*********************************************************************************

    public PlantUmlBuilder addAssociation(String aName, String bName) {
        return addAssociation(aName, bName, DIRECTION, null);
    }

    public PlantUmlBuilder addAssociation(String aName, String bName, Association assoc) {
        return addAssociation(aName, bName, assoc, null);
    }

    public PlantUmlBuilder addAssociation(String aName, String bName, String label) {
        return addAssociation(aName, bName, DIRECTION, label);
    }

    public PlantUmlBuilder addAssociation(String aName, String bName, Association assoc, String label) {
        return addAssociation(aName, bName, assoc, label, null, null);
    }

    public PlantUmlBuilder addAssociation(String aName, String bName, Association assoc, String label, String aCardinality, String bCardinality) {
        Validate.notBlank(aName, "Class a name is mandatory");
        Validate.notBlank(bName, "Class b name is mandatory");
        Validate.notNull(assoc, "Association type is mandatory");

        content.append(escape(aName));
        if (StringUtils.isNotBlank(aCardinality)) {
            content.append(SPACE).append(QUOTE).append(aCardinality).append(QUOTE);
        }
        content.append(SPACE).append(assoc).append(SPACE);
        if (StringUtils.isNotBlank(bCardinality)) {
            content.append(QUOTE).append(bCardinality).append(QUOTE).append(SPACE);
        }
        content.append(escape(bName));
        if (StringUtils.isNotBlank(label)) {
            content.append(SPACE).append(SEMICOLON).append(SPACE).append(label);

        }
        content.append(NEWLINE);
        return this;
    }
}
