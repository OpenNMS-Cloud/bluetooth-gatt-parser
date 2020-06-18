package org.sputnikdev.bluetooth.gattparser;

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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sputnikdev.bluetooth.gattparser.spec.BluetoothGattSpecificationReader;
import org.sputnikdev.bluetooth.gattparser.spec.Characteristic;
import org.sputnikdev.bluetooth.gattparser.spec.Field;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.sputnikdev.bluetooth.gattparser.spec.MockUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BluetoothGattParserTest {

    private static final String CHARACTERISTIC_UUID = "2AA7";

    @Mock
    private BluetoothGattSpecificationReader specificationReader;

    @Mock
    private CharacteristicParser defaultParser;

    @Mock
    private Characteristic characteristic;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private GattRequest gattRequest;

    private byte[] data = new byte[]{0x0};

    @InjectMocks
    @Spy
    private BluetoothGattParser parser;

    @BeforeEach
    public void setUp() {
        byte[] data = new byte[]{0x0};

        @SuppressWarnings("unchecked")
        LinkedHashMap<String, FieldHolder> holders = mock(LinkedHashMap.class);

        lenient().when(specificationReader.getCharacteristicByUUID(CHARACTERISTIC_UUID)).thenReturn(characteristic);
        lenient().when(characteristic.isValidForRead()).thenReturn(true);
        lenient().when(characteristic.isValidForWrite()).thenReturn(true);

        lenient().when(defaultParser.parse(characteristic, data)).thenReturn(holders);

        lenient().when(gattRequest.getCharacteristicUUID()).thenReturn(CHARACTERISTIC_UUID);
    }

    @Test
    public void testParse() {
        GattResponse response = parser.parse(CHARACTERISTIC_UUID, data);
        assertThat(response, notNullValue());

        verify(defaultParser, times(1)).parse(characteristic, data);
        verify(specificationReader, times(2)).getCharacteristicByUUID(CHARACTERISTIC_UUID);
    }

    public void testParseNoValid() {
        when(characteristic.isValidForRead()).thenReturn(false);

        Exception ex = assertThrows(CharacteristicFormatException.class, () -> parser.parse(CHARACTERISTIC_UUID, data));
    }

    @Test
    public void testParseCustomParser() {
        CharacteristicParser customParser = mock(CharacteristicParser.class);
        parser.registerParser(CHARACTERISTIC_UUID, customParser);

        GattResponse response = parser.parse(CHARACTERISTIC_UUID, data);
        assertThat(response, notNullValue());

        verify(defaultParser, times(0)).parse(characteristic, data);
        verify(specificationReader, times(2)).getCharacteristicByUUID(CHARACTERISTIC_UUID);
        verify(customParser, times(1)).parse(characteristic, data);
    }

    @Test
    public void testSerialize() {
        doReturn(true).when(parser).validate(gattRequest);
        parser.serialize(gattRequest, true);

        verify(parser, times(1)).validate(gattRequest);
        verify(defaultParser, times(1)).serialize(gattRequest.getAllFieldHolders());
    }

    public void testSerializeRequestNotValid() {
        doReturn(false).when(parser).validate(gattRequest);
        Exception ex = assertThrows(IllegalArgumentException.class, () -> parser.serialize(gattRequest, true));

        verify(parser, times(1)).validate(gattRequest);
    }

    public void testSerializeRequestNotValidForWrite() {
        when(characteristic.isValidForWrite()).thenReturn(false);
        doReturn(true).when(parser).validate(gattRequest);

        Exception ex = assertThrows(CharacteristicFormatException.class, () -> parser.serialize(gattRequest, true));

        verify(parser, times(1)).validate(gattRequest);
    }

    @Test
    public void testSerializeCustomParser() {
        CharacteristicParser customParser = mock(CharacteristicParser.class);
        parser.registerParser(CHARACTERISTIC_UUID, customParser);

        parser.serialize(gattRequest);

        verify(defaultParser, times(0)).serialize(gattRequest.getAllFieldHolders());
        verify(specificationReader, times(1)).getCharacteristicByUUID(CHARACTERISTIC_UUID);
        verify(customParser, times(1)).serialize(gattRequest.getAllFieldHolders());
    }

    @Test
    public void testGetCharacteristic() {
        assertThat(characteristic, is(parser.getCharacteristic(CHARACTERISTIC_UUID)));
        verify(specificationReader, times(1)).getCharacteristicByUUID(CHARACTERISTIC_UUID);
    }

    @Test
    public void prepareGattRequest() {

        List<Field> fields = new ArrayList<>();
        Field opControl = MockUtils.mockControlField("Op Code", false);
        fields.add(opControl);
        when(specificationReader.getFields(characteristic)).thenReturn(fields);

        GattRequest request = parser.prepare(CHARACTERISTIC_UUID);
        assertThat(CHARACTERISTIC_UUID, is(request.getCharacteristicUUID()));

        verify(specificationReader, times(1)).getCharacteristicByUUID(CHARACTERISTIC_UUID);
        verify(specificationReader, times(1)).getFields(characteristic);
    }

    @Test
    public void testValidateOpControl() throws Exception {
        // optional op control field
        List<Field> fields = new ArrayList<>();
        Field opControl = MockUtils.mockControlField("Op Code", false);
        fields.add(opControl);
        GattRequest gattRequest = new GattRequest(CHARACTERISTIC_UUID, fields);
        assertThat(parser.validate(gattRequest), is(true));
        gattRequest.setField("Op Code", 1);
        assertThat(parser.validate(gattRequest), is(true));

        // mandatory op control field
        fields = new ArrayList<>();
        opControl = MockUtils.mockControlField("Op Code", true);
        fields.add(opControl);
        gattRequest = new GattRequest(CHARACTERISTIC_UUID, fields);
        assertThat(parser.validate(gattRequest), is(false));
        gattRequest.setField("Op Code", 1);
        assertThat(parser.validate(gattRequest), is(true));

        // dependants

        fields = new ArrayList<>();
        opControl = MockUtils.mockControlField("Op Code", true, "C1", "C2", "C3");
        fields.add(opControl);
        gattRequest = new GattRequest(CHARACTERISTIC_UUID, fields);
        assertThat(parser.validate(gattRequest), is(false));
        gattRequest.setField("Op Code", 1);
        assertThat(parser.validate(gattRequest), is(false));

        fields = new ArrayList<>();
        opControl = MockUtils.mockControlField("Op Code", true, "C1", "C2", "C3");
        fields.add(opControl);
        fields.add(MockUtils.mockField("Field1", "C1"));
        gattRequest = new GattRequest(CHARACTERISTIC_UUID, fields);
        gattRequest.setField("Op Code", 1);
        assertThat(parser.validate(gattRequest), is(false));
        gattRequest.setField("Field1", 1);
        assertThat(parser.validate(gattRequest), is(true));
        gattRequest.setField("Op Code", 2);
        assertThat(parser.validate(gattRequest), is(false));

        fields.add(MockUtils.mockField("Field2", "C2"));
        gattRequest = new GattRequest(CHARACTERISTIC_UUID, fields);
        gattRequest.setField("Op Code", 2);
        assertThat(parser.validate(gattRequest), is(false));
        gattRequest.setField("Field2", 2);
        assertThat(parser.validate(gattRequest), is(true));
    }

    @Test
    public void testValidateMandatoryFields() throws Exception {
        List<Field> fields = new ArrayList<>();
        fields.add(MockUtils.mockField("Field1", "Mandatory"));
        fields.add(MockUtils.mockField("Field2", "C1", "C2"));
        GattRequest gattRequest = new GattRequest(CHARACTERISTIC_UUID, fields);
        assertThat(parser.validate(gattRequest), is(false));
        gattRequest.setField("Field1", 1);
        assertThat(parser.validate(gattRequest), is(true));

        gattRequest.setField("Field2", 2);
        assertThat(parser.validate(gattRequest), is(true));
    }

    @Test
    public void testParseSimple() {
        byte[] data = new byte[] {0x54, 0x3d, 0x32, 0x37, 0x2e, 0x36, 0x20, 0x48, 0x3d, 0x39, 0x32, 0xe, 0x36, 0x00};
        assertThat(parser.parse(data, 16), is("[54, 3d, 32, 37, 2e, 36, 20, 48, 3d, 39, 32, 0e, 36, 00]"));
    }

    @Test
    public void testSerializeSimple() {
        byte[] data = new byte[] {(byte) 0xfe, 0x3d, 0x32, 0x37, 0x2e, 0x36, 0x20, 0x48, 0x3d, 0x39, 0x32, 0xe, 0x36, 0x00};
        assertThat(parser.serialize("[fe, 3d, 32, 37, 2e, 36, 20, 48, 3d, 39, 32, e, 36, 0]", 16), is(data));
    }

}
