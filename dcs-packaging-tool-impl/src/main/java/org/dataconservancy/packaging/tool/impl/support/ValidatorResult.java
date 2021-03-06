/*
 * Copyright 2016 Johns Hopkins University
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

package org.dataconservancy.packaging.tool.impl.support;

/**
 * A support class for validators to facilitate returning a message as well as the boolean result of a validation test.
 * The message is most useful for describing the reason for a validation failure
 */
public class ValidatorResult {

    String message;

    boolean result;

    public ValidatorResult() {

    }

    public ValidatorResult(boolean result) {
        this.result = result;
    }

    public ValidatorResult(boolean result, String message) {
        this.message = message;
        this.result = result;
    }

    public boolean getResult(){
        return result;
    };

    public void setResult(boolean result){
        this.result = result;
    };

    public String getMessage(){
        return message;
    };

    public void setMessage(String message){
        this.message = message;
    };
}

