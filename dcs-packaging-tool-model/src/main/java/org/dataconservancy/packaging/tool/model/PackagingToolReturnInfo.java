/*
 * Copyright 2013 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataconservancy.packaging.tool.model;

/**
 * Error messages to be used for runtime exceptions for the command line Package Generation Tool
 * Return codes are specified here - return codes for errors must be non-zero.
 *
 */
public enum PackagingToolReturnInfo {

    CMD_LINE_FILE_NOT_FOUND_EXCEPTION (2, "Commandline App: File not found."),
    CMD_LINE_PARAM_BUILD_EXCEPTION(3, "Commandline App: Attempt to parse the provided parameter file failed. "),
    CMD_LINE_INPUT_ERROR(4, "Commandline App: Error reading from input."),
    CMD_LINE_PACKAGE_NOT_GENERATED(5, "Commandline App: Error creating package, null package returned."),
    CMD_LINE_CONTENT_ROOT_NOT_SPECIFIED(6, "Commandline App: Content root directory was not specified"),
    CMD_LINE_DOMAIN_PROFILE_NOT_SPECIFIED(7, "Commandline App: Domain profile was not specified" ),
    CMD_LINE_BAD_DOMAIN_PROFILE_EXTENSION(8, "Commandline App: Domain profile must be a turtle file with name ending in .ttl, or an RDF/XML file " +
                                "with name ending in .xml"),
    CMD_LINE_CONTENT_ROOT_CANT_OPEN(9, "Commandline App: Content root directory could not be opened"),
    CMD_LINE_DOMAIN_PROFILE_CANT_OPEN(10,"Commandline App: Domain profile file could not be opened"),
    CMD_LINE_PACKAGE_METADATA_CANT_OPEN(10,"Commandline App: Package metadata file could not be opened"),
    CMD_LINE_CANT_ASSIGN_NODE_TYPES(11, "Unable to assign node types to tree"),
    CMD_LINE_CANT_TRANSFORM_TO_RDF(12, "Error transforming the tree's internal package model to RDF"),
    CMD_LINE_PACKAGE_LOCATION_CANT_OPEN(13,"Commandline App: Package location could not be opened"),
    CMD_LINE_STAGING_LOCATION_CANT_OPEN(14,"Commandline App: Package staging location could not be opened"),
    CMD_LINE_PACKAGE_LOCATION_NOT_SPECIFIED(15, "Commandline App: Packaeg location was not specified"),

    /* general exceptions used across the board */
    PKG_FILE_NOT_FOUND_EXCEPTION (100,"File not found"),
    PKG_IO_EXCEPTION (101, "IO Exception"),
    PKG_OBJECT_INSTANTIATION_EXP (102, "Could not instantiate object needed to create the package."),
    PKG_REQUIRED_PARAMS_MISSING (103, "One or more parameters required for generating the package is missing."),
    PKG_DIR_CREATION_EXP (104, "Required directory could not be created "),


    PKG_NO_SUCH_CHECKSUM_ALGORITHM_EXCEPTION (201, "No such checksum algorithm"),

    /* exceptions relating to Package Description */
    PKG_DESC_JSON_PROCESSING_ERROR (301, "JSON Processing Error"),
    PKG_DESC_RDF_TRANSFORM_EXCEPTION (302, "RDF Transform Exception"),
    PKG_DESC_IO_EXCEPTION (303, "Package serialization IO Exception"),
    PKG_DESC_UNSUPPORTED_ENCODING_EXCEPTION (304, "Unsupported Encoding Exception"),
    PKG_VALIDATION_FAIL (305, "PackageDescription validation failed"),

    /* exceptions relating to package assembler */
    PKG_ASSEMBLER_URI_GENERATION_EXP (401, "Failed to generate file URI."),
    PKG_ASSEMBLER_PARAMS_NOT_INITIALIZED_EXP (402, "This assembler has not been initialized with required package " +
            "generation parameters."),
    PKG_ASSEMBLER_ARCHIVE_EXP (404, "Exception occurred during package serialization. "),
    PKG_ASSEMBLER_COMPRESSION_EXP (405, "Exception occurred when compressing the package's serialization. "),
    PKG_ASSEMBLER_INVALID_PARAMS (406, "One or more initial parameters for the package assembler was invalid "),
    PKG_ASSEMBLER_STRAY_FILE (407, "One or more of the files provided to the package assembler do not reside under the specified" +
            " content root location"),
    PKG_ASSEMBLER_INVALID_FILENAME (408, "One or more of the files provided to the package assembler has an invalid file name."),

    /* exception relating to Package Generator */
    PKG_UNEXPECTED_PACKAGING_FORMAT ( 501, "Package format provided was not as expected."),
    PKG_GEN_MISSING_COMPONENTS (502, "One or more required components for generating packages is missing. ");


    private int code;
    private String message;
    PackagingToolReturnInfo (int returnCode, String messageString) {
        code = returnCode;
        message = messageString;
    }

    public int returnCode(){
        return code;
    }

    public String stringMessage() {
        return message;
    }

}
