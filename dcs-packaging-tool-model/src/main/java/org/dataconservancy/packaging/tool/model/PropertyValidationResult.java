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

import org.dataconservancy.packaging.tool.model.dprofile.PropertyValueHint;

/**
 * Class that holds the validation result from validating a property of an ontology.
 * Class contains a boolean representing the validation result, and an enum that tells the system what hint to display to the user.
 * Classes receiving this class should implement strings to display for the {@link org.dataconservancy.packaging.tool.model.ValidationType} enum.
 */
public class PropertyValidationResult {

    private PropertyValueHint validationType;
    private boolean valid;

    public PropertyValidationResult(boolean valid, PropertyValueHint type) {
        this.valid = valid;
        this.validationType = type;
    }

    public PropertyValueHint getValidationType() {
        return validationType;
    }

    public boolean isValid() {
        return valid;
    }
}
