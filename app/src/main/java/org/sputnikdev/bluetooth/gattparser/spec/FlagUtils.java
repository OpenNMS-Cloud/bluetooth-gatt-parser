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

import org.sputnikdev.bluetooth.gattparser.BluetoothGattParserFactory;
import org.sputnikdev.bluetooth.gattparser.num.RealNumberFormatter;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Vlad Kolotov
 */
public final class FlagUtils implements IFlagUtils {

    public FlagUtils() {
    }

    @Override
    public Set<String> getReadFlags(List<Field> fields, byte[] data, RealNumberFormatter formatter) {
        Set<String> flags = new HashSet<>();
        int index = 0;
        for (Field field : fields) {
            if (field.isFlagField()) {
                int[] values = field.parseReadFlags(data, index, formatter);
                int bitIndex = 0;
                for (Bit bit : field.getBitField().getBits()) {
                    String requires = bit.getFlag((byte) values[bitIndex++]);
                    if (requires != null) {
                        List<String> flgs = Arrays.asList(requires.split(","));
                        if (!flgs.isEmpty()) {
                            flags.addAll(flgs);
                        }
                    }
                }
                break;
            }
            if (field.getReference() != null) {
                // if flags field goes after a reference field, then it is not possible to parse the such characteristic
                // simply because we don't know if this reference field if optional or not
                break;
            }
            if (field.getFormat() == null) {
                // This is a strange field without format!
                throw new IllegalStateException("A filed is missing its format: " + field.getName());
            }
            index += field.getFormat().getSize();
        }
        return flags;
    }
    /*
    public static String getRequires(Field field, BigInteger key) {
        return getEnumeration(field, key).map(Enumeration::getRequires).orElse(null);
    }

    public static Optional<Enumeration> getEnumeration(Field field, BigInteger key) {
        if (key == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(field.getEnumerations()).map(Enumerations::getEnumerations)
                .map(Collection::stream).orElse(Stream.empty())
                .filter(e -> key.equals(e.getKey())).findAny();
    }

    public static List<Enumeration> getEnumerations(Field field, String value) {
        if (value == null) {
            return Collections.emptyList();
        }

        return Optional.ofNullable(field.getEnumerations()).map(Enumerations::getEnumerations)
                .map(Collection::stream).orElse(Stream.empty())
                .filter(e -> value.equals(e.getValue()))
                .collect(Collectors.toList());
    }

    public static boolean isFlagsField(Field field) {
        return "flags".equalsIgnoreCase(field.getName()) && field.getBitField() != null;
    }

    public static boolean isOpCodesField(Field field) {
        String name = field.getName();
        return ("op code".equalsIgnoreCase(name) || "op codes".equalsIgnoreCase(name))
                && field.getEnumerations() != null && !field.getEnumerations().getEnumerations().isEmpty();
    }

    static Set<String> getAllFlags(Field flagsField) {
        Set<String> result = new HashSet<>();
        if (flagsField != null && flagsField.getBitField() != null) {
            for (Bit bit : flagsField.getBitField().getBits()) {
                for (Enumeration enumeration : bit.getEnumerations().getEnumerations()) {
                    if (enumeration.getRequires() != null) {
                        result.add(enumeration.getRequires());
                    }
                }
            }
        }
        return result;
    }

    static Set<String> getAllOpCodes(Field field) {
        Set<String> result = new HashSet<>();
        if (field.getEnumerations() == null || field.getEnumerations().getEnumerations() == null) {
            return Collections.emptySet();
        }
        for (Enumeration enumeration : field.getEnumerations().getEnumerations()) {
            result.add(enumeration.getRequires());
        }
        return result;
    }

    static int[] parseReadFlags(Field flagsField, byte[] raw, int index, RealNumberFormatter formatter) {
        BitSet bitSet = BitSet.valueOf(raw).get(index, index + flagsField.getFormat().getSize());
        List<Bit> bits = flagsField.getBitField().getBits();
        int[] flags = new int[bits.size()];
        int offset = 0;
        for (int i = 0; i < bits.size(); i++) {
            int size = bits.get(i).getSize();
            flags[i] = formatter.deserializeInteger(bitSet.get(offset, offset + size), size, false);
            offset += size;
        }
        return flags;
    }
    */


    Field getFlags(List<Field> fields) {
        for (Field field : fields) {
            if (field.isFlagField()) {
                return field;
            }
        }
        return null;
    }

    Field getOpCodes(List<Field> fields) {
        for (Field field : fields) {
            if (field.isOpCodesField()) {
                return field;
            }
        }
        return null;
    }


}
