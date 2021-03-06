/*
 *
 *  * Copyright 2015 Johns Hopkins University
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.dataconservancy.packaging.tool.model.ser;

/**
 * Identifiers for streams serialized in a package or package state.
 */
public enum StreamId {

    /**
     * Identifier for the stream containing the name of the package
     */
    PACKAGE_NAME,

    /**
     * Identifier for the stream containing the internal package model
     */
    PACKAGE_TREE,

    /**
     * Identifier for the stream containing package metadata fields and values
     */
    PACKAGE_METADATA,

    /**
     * Identifier for the stream containing developer version information (version number, build number, etc.) for the
     * Package Tool GUI
     */
    APPLICATION_VERSION,

    /**
     * Identifier for the stream containing the URIs of Domain Profiles in use for the package
     */
    DOMAIN_PROFILE_LIST,

    /**
     * Identifier for the stream containing the serialization of domain objects in the package
     */
    DOMAIN_OBJECTS,

    /**
     * Identifier for the stream containing the user specified properties map for the nodes in the package
     */
    USER_SPECIFIED_PROPERTIES

}
