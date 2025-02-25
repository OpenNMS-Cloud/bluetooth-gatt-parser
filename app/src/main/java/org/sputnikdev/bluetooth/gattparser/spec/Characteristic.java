package org.sputnikdev.bluetooth.gattparser.spec;

/*-
 * #%L
 * org.sputnikdev:bluetooth-gatt-parser
 * %%
 * Copyright (C) 2017 Sputnik Dev
 * %%
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
 * #L%
 */

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * @author Vlad Kolotov
 */
@XStreamAlias("Characteristic")
public class Characteristic {
    @XStreamAsAttribute
    private String name;
    @XStreamAsAttribute
    private String uuid;
    @XStreamAsAttribute
    private String type;
    @XStreamAlias("InformativeText")
    private InformativeText informativeText;
    @XStreamAlias("Value")
    private Value value;

    private boolean validForRead;
    private boolean validForWrite;

    public String getName() {
        return name;
    }

    public String getUuid() {
        return uuid;
    }

    public String getType() {
        return type;
    }

    public InformativeText getInformativeText() {
        return informativeText;
    }

    public Value getValue() {
        return value;
    }

    public boolean isValidForRead() {
        return validForRead;
    }

    void setValidForRead(boolean validForRead) {
        this.validForRead = validForRead;
    }

    public boolean isValidForWrite() {
        return validForWrite;
    }

    void setValidForWrite(boolean validForWrite) {
        this.validForWrite = validForWrite;
    }
}
