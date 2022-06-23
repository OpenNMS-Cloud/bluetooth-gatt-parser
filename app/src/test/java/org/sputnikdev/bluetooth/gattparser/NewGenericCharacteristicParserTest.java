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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sputnikdev.bluetooth.gattparser.num.FloatingPointNumberFormatter;
import org.sputnikdev.bluetooth.gattparser.num.RealNumberFormatter;
import org.sputnikdev.bluetooth.gattparser.spec.*;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.collection.IsMapWithSize.aMapWithSize;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NewGenericCharacteristicParserTest {

    private static final String CHARACTERISTIC_UUID = "2A19";

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Field flagField;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Characteristic characteristic;

    @Mock
    private RealNumberFormatter twosComplementNumberFormatter;

    @Mock
    private FloatingPointNumberFormatter ieee754NumberFormatter;

    @Mock
    private FloatingPointNumberFormatter ieee11073NumberFormatter;

    // this must be left as static initializer otherwise some tests will fail
    private BluetoothGattSpecificationReader reader;

    @Mock
    private IGattParserConfiguration configuration;

    private GenericCharacteristicParser charParser;

    @Mock
    private BluetoothGattParser gattParser;

    @Mock
    private IFlagUtils flagUtils;

    @BeforeEach
    public void setUp() {
        // PowerMockito.mockStatic(BluetoothGattParserFactory.class, FlagUtils.class);
        lenient().when(characteristic.getUuid()).thenReturn(CHARACTERISTIC_UUID);

        // this must be left as static initializer otherwise some tests will fail
        reader = mock(BluetoothGattSpecificationReader.class, RETURNS_DEEP_STUBS);

        lenient().when(configuration.getTwosComplementNumberFormatter()).thenReturn(twosComplementNumberFormatter);
        lenient().when(configuration.getIEEE754FloatingPointNumberFormatter()).thenReturn(ieee754NumberFormatter);
        lenient().when(configuration.getIEEE11073FloatingPointNumberFormatter()).thenReturn(ieee11073NumberFormatter);
        lenient().when(configuration.getGattSpecificationReader()).thenReturn(reader);
        lenient().when(configuration.getGattParser()).thenReturn(gattParser);
        lenient().when(configuration.getFlagUtils()).thenReturn(flagUtils);

        charParser = spy(new GenericCharacteristicParser(configuration));

        lenient().when(flagField.getName()).thenReturn("fLags");
    }


    @Test
    public void testParseNotValidForRead() throws CharacteristicFormatException {
        when(characteristic.isValidForRead()).thenReturn(Boolean.FALSE);
        Exception ex = assertThrows(CharacteristicFormatException.class, () -> {
            charParser.parse(characteristic, new byte[]{});
            verify(characteristic.isValidForRead(), times(1));
        });
    }

    @Test
    public void testParseFormats() throws CharacteristicFormatException, UnsupportedEncodingException {
        lenient().when(ieee754NumberFormatter.deserializeSFloat(ArgumentMatchers.<BitSet>any())).thenReturn(0.0F);
        lenient().when(ieee754NumberFormatter.deserializeFloat(ArgumentMatchers.<BitSet>any())).thenReturn(0.0F);
        lenient().when(ieee754NumberFormatter.deserializeDouble(ArgumentMatchers.<BitSet>any())).thenReturn(0.0D);

        lenient().when(ieee11073NumberFormatter.deserializeSFloat(ArgumentMatchers.<BitSet>any())).thenReturn(0.0F);
        lenient().when(ieee11073NumberFormatter.deserializeFloat(ArgumentMatchers.<BitSet>any())).thenReturn(0.0F);

        lenient().when(twosComplementNumberFormatter.deserializeInteger(ArgumentMatchers.<BitSet>any(), anyInt(), anyBoolean())).thenReturn(0);
        lenient().when(twosComplementNumberFormatter.deserializeLong(ArgumentMatchers.<BitSet>any(), anyInt(), anyBoolean())).thenReturn(0L);
        lenient().when(twosComplementNumberFormatter.deserializeBigInteger(ArgumentMatchers.<BitSet>any(), anyInt(), anyBoolean())).thenReturn(BigInteger.ZERO);


        byte[] data = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
        BitSet bits = BitSet.valueOf(data);
        assertParseFormat(false, "boolean", new byte[]{(byte) 0b010});
        assertParseFormat(true, "boolean", new byte[]{(byte) 0b101});

        assertParseFormat(0, "2bit", data);
        verify(twosComplementNumberFormatter, times(1)).deserializeInteger(bits, 2, false);
        assertParseFormat(0, "8bit", data);
        verify(twosComplementNumberFormatter, times(1)).deserializeInteger(bits, 8, false);
        assertParseFormat(0, "uint8", data);
        verify(twosComplementNumberFormatter, times(2)).deserializeInteger(bits, 8, false);
        assertParseFormat(0, "sint8", data);
        verify(twosComplementNumberFormatter, times(1)).deserializeInteger(bits, 8, true);

        assertParseFormat(0, "sint31", data);
        verify(twosComplementNumberFormatter, times(1)).deserializeInteger(bits, 31, true);
        assertParseFormat(0, "uint31", data);
        verify(twosComplementNumberFormatter, times(1)).deserializeInteger(bits, 31, false);
        assertParseFormat(0, "sint32", data);
        verify(twosComplementNumberFormatter, times(1)).deserializeInteger(bits, 32, true);
        assertParseFormat(0L, "uint32", data);
        verify(twosComplementNumberFormatter, times(1)).deserializeLong(bits, 32, false);
        assertParseFormat(0L, "sint33", data);
        verify(twosComplementNumberFormatter, times(1)).deserializeLong(bits, 33, true);

        assertParseFormat(0L, "sint63", data);
        verify(twosComplementNumberFormatter, times(1)).deserializeLong(bits, 63, true);
        assertParseFormat(0L, "uint63", data);
        verify(twosComplementNumberFormatter, times(1)).deserializeLong(bits, 63, false);
        assertParseFormat(0L, "sint64", data);
        verify(twosComplementNumberFormatter, times(1)).deserializeLong(bits, 64, true);
        assertParseFormat(BigInteger.ZERO, "uint64", data);
        verify(twosComplementNumberFormatter, times(1)).deserializeBigInteger(bits, 64, false);
        assertParseFormat(BigInteger.ZERO, "sint65", data);
        verify(twosComplementNumberFormatter, times(1)).deserializeBigInteger(bits, 65, true);

        assertParseFormat(0.0F, "float32", data);
        verify(ieee754NumberFormatter, times(1)).deserializeFloat(bits);
        assertParseFormat(0.0D, "float64", data);
        verify(ieee754NumberFormatter, times(1)).deserializeDouble(bits);
        assertParseFormat(0.0F, "sfloat", data);
        verify(ieee11073NumberFormatter, times(1)).deserializeSFloat(bits);
        assertParseFormat(0.0F, "float", data);
        verify(ieee11073NumberFormatter, times(1)).deserializeFloat(bits);

        assertParseFormat("test8", "utf8s", "test8".getBytes());
        assertParseFormat("test16", "utf16s", "test16".getBytes("UTF-16"));
    }

    @Test
    public void testParse() throws CharacteristicFormatException, UnsupportedEncodingException {
        Object value = new Object();
        doReturn(value).when(charParser).parse(ArgumentMatchers.any(Field.class), ArgumentMatchers.any(byte[].class), anyInt());

        List<Field> fields = new ArrayList<>();
        Field flagsField = MockUtils.mockFieldFormat("flags", "uint8");
        fields.add(flagsField); // should be ignored
        fields.add(MockUtils.mockFieldFormat("Field1", "uint8", "C1"));
        fields.add(MockUtils.mockFieldFormat("Field2", "uint8", "C1", "C2"));
        fields.add(MockUtils.mockFieldFormat("Field3", "uint8", "C2"));
        fields.add(MockUtils.mockFieldFormat("Field4", "uint8", new String[]{}));
        fields.add(MockUtils.mockFieldFormat("Field5", "uint8", "C5"));
        fields.add(MockUtils.mockFieldFormat("Field6", "uint8", "Mandatory"));
        when(reader.getFields(characteristic)).thenReturn(fields);
        when(characteristic.getValue().getFields()).thenReturn(fields);
        when(characteristic.isValidForRead()).thenReturn(true);

        when(flagsField.isFlagField()).thenReturn(true);
        when(flagUtils.getReadFlags(anyList(), any(byte[].class), any(RealNumberFormatter.class))).thenReturn(
                new HashSet<>(Arrays.asList("C1", "C3", "C4")));
        assertFieldsExist(value, "Field1", "Field4", "Field6");

        when(flagUtils.getReadFlags(anyList(), any(byte[].class), any(RealNumberFormatter.class))).thenReturn(
                new HashSet<>(Arrays.asList("C2")));
        assertFieldsExist(value, "Field3", "Field4", "Field6");

        when(flagUtils.getReadFlags(anyList(), any(byte[].class), any(RealNumberFormatter.class))).thenReturn(
                new HashSet<>(Arrays.asList("C1", "C2")));
        assertFieldsExist(value, "Field1", "Field2", "Field3", "Field4", "Field6");
    }

    @Test
    public void testParseNotEnoughData() throws CharacteristicFormatException, UnsupportedEncodingException {
        byte[] data = new byte[]{0};
        assertThrows(CharacteristicFormatException.class, () -> {
            assertParseFormat(0, "8bit", data);
            assertParseFormat(0, "9bit", data);

        });
    }

    @Test
    public void testParseComplex() {
        byte[] data = new byte[]{85, -86, 85, -86, 85, -86, 85, -86};

        List<Field> fields = new ArrayList<>();
        fields.add(MockUtils.mockFieldFormat("Field1", "uint40", new String[]{}));
        fields.add(MockUtils.mockFieldFormat("Field2", "uint24", new String[]{}));
        when(reader.getFields(characteristic)).thenReturn(fields);
        when(characteristic.getValue().getFields()).thenReturn(fields);
        when(characteristic.isValidForRead()).thenReturn(true);

        charParser.parse(characteristic, data);

        verify(twosComplementNumberFormatter, times(1)).deserializeLong(BitSet.valueOf(data).get(0, 40), 40, false);
        verify(twosComplementNumberFormatter, times(1)).deserializeInteger(BitSet.valueOf(data).get(40, 64), 24, false);

    }

    @Test
    public void testParseComplexWithReferences() {
        when(twosComplementNumberFormatter.deserializeInteger(ArgumentMatchers.<BitSet>any(), eq(8), eq(false))).thenReturn(10);
        // Flags for inner fields: C1, C2
        Set<String> flags = new HashSet<String>() {{
            add("C1");
            add("C2");
        }};
        when(twosComplementNumberFormatter.deserializeInteger(ArgumentMatchers.<BitSet>any(), eq(8), eq(true))).thenReturn(-12);
        when(twosComplementNumberFormatter.deserializeInteger(ArgumentMatchers.<BitSet>any(), eq(16), eq(false))).thenReturn(13);
        when(twosComplementNumberFormatter.deserializeInteger(ArgumentMatchers.<BitSet>any(), eq(16), eq(true))).thenReturn(14);
        byte[] data = new byte[]{10, 0b11, -12, 13, 0, 14, 0};

        List<Field> fields = new ArrayList<>();
        fields.add(MockUtils.mockFieldFormat("Field1", "uint8", new String[]{}));
        Field field = mock(Field.class);
        when(field.getReference()).thenReturn("org.bluetooth.characteristic_id");
        fields.add(field);
        fields.add(MockUtils.mockFieldFormat("Field2", "sint16", new String[]{}));

        when(characteristic.getValue().getFields()).thenReturn(fields);
        when(characteristic.isValidForRead()).thenReturn(true);

        List<Field> innerFields = new ArrayList<>();
        Field innerFlags = MockUtils.mockFieldFormat("flags", "8bit", new String[]{});
        BitField bitField = mock(BitField.class);
        lenient().when(innerFlags.getBitField()).thenReturn(bitField);
        List<Bit> bits = new ArrayList<Bit>() {{
            add(MockUtils.mockBit(0, "C1"));
            add(MockUtils.mockBit(1, "C2"));
            add(MockUtils.mockBit(2, "C3"));
        }};
        lenient().when(bitField.getBits()).thenReturn(bits);
        innerFields.add(innerFlags);
        innerFields.add(MockUtils.mockFieldFormat("InnerField1", "sint8", "C1"));
        innerFields.add(MockUtils.mockFieldFormat("InnerField0", "uint16", "C3"));
        innerFields.add(MockUtils.mockFieldFormat("InnerField2", "uint16", "C2"));
        Characteristic referenced = mock(Characteristic.class, RETURNS_DEEP_STUBS);
        when(referenced.getValue().getFields()).thenReturn(innerFields);
        when(referenced.isValidForRead()).thenReturn(true);
        when(flagUtils.getReadFlags(anyList(), any(byte[].class), any(RealNumberFormatter.class))).thenReturn(flags);
        when(innerFlags.isFlagField()).thenReturn(true);
        when(reader.getCharacteristicByType("org.bluetooth.characteristic_id")).thenReturn(referenced);

        LinkedHashMap<String, FieldHolder> result = charParser.parse(characteristic, data);
        // assertEquals(4, result.size());
        assertThat(result, aMapWithSize(4));

        // assertEquals(10, (int) result.get("Field1").getInteger(null));
        assertThat(result.get("Field1").getInteger(null), is(10));

        // assertEquals(-12, (int) result.get("InnerField1").getInteger(null));
        assertThat(result.get("InnerField1").getInteger(null), is(-12));

        // assertEquals(13, (int) result.get("InnerField2").getInteger(null));
        assertThat(result.get("InnerField2").getInteger(null), is(13));

        // assertEquals(14, (int) result.get("Field2").getInteger(null));
        assertThat(result.get("Field2").getInteger(null), is(14));
    }

    @Test
    public void serialize() {

        int sint4 = 13;
        String str = "awesome value";
        int strLength = BitSet.valueOf(str.getBytes()).length();

        when(twosComplementNumberFormatter.serialize(eq(sint4), eq(4), eq(true))).thenReturn(BitSet.valueOf(new long[]{sint4}));

        List<Field> fields = new ArrayList<>();
        fields.add(MockUtils.mockFieldFormat("Field1", "uint8"));
        fields.add(MockUtils.mockFieldFormat("Field2", "sint4"));
        fields.add(MockUtils.mockFieldFormat("Field3", "utf8s"));
        fields.add(MockUtils.mockFieldFormat("Field4", "sint4"));
        GattRequest request = new GattRequest(CHARACTERISTIC_UUID, fields);

        request.setField("Field2", sint4);
        request.setField("Field3", str);
        request.setField("Field4", sint4);

        byte[] data = charParser.serialize(request.getAllFieldHolders());
        // assertNotNull(data);
        assertThat(data, notNullValue());

        // assertEquals(sint4, data[0] & 0x0000000F);
        assertThat(sint4, is(data[0] & 0x0000000F));

        BitSet textBytes = BitSet.valueOf(data).get(4, 4 + strLength);
        // assertEquals(str, new String(textBytes.toByteArray()));
        assertThat(new String(textBytes.toByteArray()), is(str));

        BitSet field4 = BitSet.valueOf(data).get(4 + strLength, 4 + strLength + 4);
        // assertEquals(sint4, field4.toByteArray()[0]);
        assertThat(field4.toByteArray()[0], is((byte) sint4));
    }

    @Test
    public void testSerializeBoolean() {
        List<Field> fields = new ArrayList<>();
        fields.add(MockUtils.mockFieldFormat("Field1", "boolean"));
        fields.add(MockUtils.mockFieldFormat("Field2", "boolean"));
        GattRequest request = new GattRequest(CHARACTERISTIC_UUID, fields);
        request.setField("Field1", true);
        request.setField("Field2", false);

        byte[] data = charParser.serialize(request.getAllFieldHolders());
        // assertNotNull(data);
        assertThat(data, notNullValue());

        verify(charParser, times(1)).serialize(true);
        // assertTrue((data[0] & 1) == 1);
        assertThat(data[0] & 1, is(1));
        // assertTrue(((data[0] >>> 1) & 1) == 0);
        assertThat((data[0] >>> 1) & 1, is(0));
    }

    @Test
    public void testSerializeReal() throws UnsupportedEncodingException {
        int sint32 = -123;
        int uint32 = 123;
        int sint64 = -124;
        int uint64 = 124;
        BigInteger sint128 = new BigInteger("-125");

        when(twosComplementNumberFormatter.serialize(anyInt(), anyInt(), eq(true))).thenReturn(BitSet.valueOf(new long[]{sint32}));
        when(twosComplementNumberFormatter.serialize(anyLong(), anyInt(), eq(false))).thenReturn(BitSet.valueOf(new long[]{uint32}));
        when(twosComplementNumberFormatter.serialize(anyLong(), anyInt(), eq(true))).thenReturn(BitSet.valueOf(new long[]{sint64}));
        when(twosComplementNumberFormatter.serialize(any(BigInteger.class), anyInt(), eq(false))).thenReturn(BitSet.valueOf(new long[]{uint64}));
        when(twosComplementNumberFormatter.serialize(any(BigInteger.class), anyInt(), eq(true))).thenReturn(BitSet.valueOf(sint128.toByteArray()));

        List<Field> fields = new ArrayList<>();
        fields.add(MockUtils.mockFieldFormat("Field1", "sint32"));
        fields.add(MockUtils.mockFieldFormat("Field2", "uint32"));
        fields.add(MockUtils.mockFieldFormat("Field3", "sint64"));
        fields.add(MockUtils.mockFieldFormat("Field4", "uint64"));
        fields.add(MockUtils.mockFieldFormat("Field5", "sint128"));

        GattRequest request = new GattRequest(CHARACTERISTIC_UUID, fields);
        request.setField("Field1", sint32);
        request.setField("Field2", uint32);
        request.setField("Field3", sint64);
        request.setField("Field4", uint64);
        request.setField("Field5", sint128);

        byte[] data = charParser.serialize(request.getAllFieldHolders());
        // assertNotNull(data);
        assertThat(data, notNullValue());

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        verify(twosComplementNumberFormatter, times(1)).serialize(sint32, 32, true);
        // assertEquals(sint32, data[0]);
        assertThat(data[0], is((byte) sint32));

        verify(twosComplementNumberFormatter, times(1)).serialize((long) uint32, 32, false);
        // assertEquals(uint32, data[4]);
        assertThat(data[4], is((byte) uint32));

        verify(twosComplementNumberFormatter, times(1)).serialize((long) sint64, 64, true);
        // assertEquals(sint64, data[8]);
        assertThat(data[8], is((byte) sint64));

        verify(twosComplementNumberFormatter, times(1)).serialize(new BigInteger(String.valueOf(uint64)), 64, false);
        // assertEquals(uint64, data[16]);
        assertThat(data[16], is((byte) uint64));

        verify(twosComplementNumberFormatter, times(1)).serialize(sint128, 128, true);
        // resulting array gets truncated to 20 bytes (length of GATT payload)
        // assertEquals(20, data.length);
        assertThat(data.length, is(20));

        //assertEquals(sint128.intValue(), data[24]);
    }

    @Test
    public void testSerializeZero() throws UnsupportedEncodingException {
        int sint32 = 0;

        when(twosComplementNumberFormatter.serialize(anyInt(), anyInt(), eq(true))).thenReturn(BitSet.valueOf(new long[]{0}));

        List<Field> fields = new ArrayList<>();
        fields.add(MockUtils.mockFieldFormat("Field1", "sint32"));

        GattRequest request = new GattRequest(CHARACTERISTIC_UUID, fields);
        request.setField("Field1", sint32);

        byte[] data = charParser.serialize(request.getAllFieldHolders());
        // assertArrayEquals(new byte[] {0}, data);
        assertThat(data, is(new byte[]{0}));
    }

    @Test
    public void testSerializeString() throws UnsupportedEncodingException {
        String utf8 = "test8";
        String utf16 = new String("test16".getBytes("UTF-8"), "UTF-16");

        List<Field> fields = new ArrayList<>();
        fields.add(MockUtils.mockFieldFormat("Field1", "utf8s"));
        fields.add(MockUtils.mockFieldFormat("Field2", "utf16s"));

        GattRequest request = new GattRequest(CHARACTERISTIC_UUID, fields);
        request.setField("Field1", utf8);
        request.setField("Field2", utf16);

        byte[] data = charParser.serialize(request.getAllFieldHolders());
        BitSet bitSet = BitSet.valueOf(data);
        int utf8Length = BitSet.valueOf(utf8.getBytes()).length();
        // assertEquals(utf8, new String(bitSet.get(0, utf8Length).toByteArray()));
        assertThat(utf8, is(new String(bitSet.get(0, utf8Length).toByteArray())));

        // assertEquals(utf16, new String(bitSet.get(utf8Length, 8 * 20).toByteArray(), "UTF-16"));
        assertThat(utf16, is(new String(bitSet.get(utf8Length, 8 * 20).toByteArray(), "UTF-16")));
    }

    @Test
    public void testSerializeDecimal() throws UnsupportedEncodingException {

        float float32 = 123.4F;
        double float64 = 456.7D;
        float sfloat = 89.1F;
        float _float = 234.5F;

        when(ieee754NumberFormatter.serializeFloat(anyFloat())).thenReturn(BitSet.valueOf(new long[]{
                Float.floatToRawIntBits(float32)}));
        when(ieee754NumberFormatter.serializeDouble(anyDouble())).thenReturn(BitSet.valueOf(new long[]{
                Double.doubleToLongBits(float64)}));

        when(ieee11073NumberFormatter.serializeSFloat(anyFloat())).thenReturn(BitSet.valueOf(new long[]{
                Float.floatToRawIntBits(sfloat)}));
        when(ieee11073NumberFormatter.serializeFloat(anyFloat())).thenReturn(BitSet.valueOf(new long[]{
                Float.floatToRawIntBits(_float)}));

        List<Field> fields = new ArrayList<>();
        fields.add(MockUtils.mockFieldFormat("Field1", "float32"));
        fields.add(MockUtils.mockFieldFormat("Field2", "float64"));
        fields.add(MockUtils.mockFieldFormat("Field3", "SFLOAT"));
        fields.add(MockUtils.mockFieldFormat("Field4", "FLOAT"));

        GattRequest request = new GattRequest(CHARACTERISTIC_UUID, fields);
        request.setField("Field1", float32);
        request.setField("Field2", float64);
        request.setField("Field3", sfloat);
        request.setField("Field4", _float);

        byte[] data = charParser.serialize(request.getAllFieldHolders());
        // assertNotNull(data);
        assertThat(data, notNullValue());

        verify(ieee754NumberFormatter, times(1)).serializeFloat(float32);
        verify(ieee754NumberFormatter, times(1)).serializeDouble(float64);

        verify(ieee11073NumberFormatter, times(1)).serializeSFloat(sfloat);
        verify(ieee11073NumberFormatter, times(1)).serializeFloat(_float);
    }

    @Test
    public void testParseAndSerializeStringFields() throws Exception {
        assertParseAndSerializeStringFields("utf8s", "UTF-8");
        assertParseAndSerializeStringFields("utf16s", "UTF-16");
    }

    @Test
    public void testParseAndSerializeStructFields() throws Exception {
        // Testing that structure fields that go after another field can be parsed correctly

        // mocking number formatter to parse/serialize integers returning 1 set bit
        when(twosComplementNumberFormatter.deserializeInteger(ArgumentMatchers.<BitSet>any(), anyInt(), anyBoolean())).thenReturn(1);
        BitSet oneBit = new BitSet();
        oneBit.set(0);
        when(twosComplementNumberFormatter.serialize(ArgumentMatchers.any(Integer.class), anyInt(), anyBoolean())).thenReturn(oneBit);

        // mocking fields, first field is a dummy field with a length of 1 bit, next one is our target field
        List<Field> fields = new ArrayList<>();
        Field field1 = MockUtils.mockFieldFormat("Field1", "uint1");
        fields.add(field1);
        Field field2 = MockUtils.mockFieldFormat("Field2", "struct");
        fields.add(field2);
        when(reader.getFields(characteristic)).thenReturn(fields);
        when(characteristic.getValue().getFields()).thenReturn(fields);
        when(characteristic.isValidForRead()).thenReturn(true);

        // data that we are testing
        byte[] field2Data = {12, 24, 56};
        BitSet data = new BitSet();
        data.set(0);
        charParser.concat(data, BitSet.valueOf(field2Data), 1, field2Data.length * 8);

        // performing the test to check if we can parse data
        LinkedHashMap<String, FieldHolder> response = charParser.parse(characteristic, data.toByteArray());
        // assertEquals(2, response.size());
        assertThat(response, aMapWithSize(2));

        // assertEquals(1, (int) response.get("Field1").getInteger());
        assertThat(response.get("Field1").getInteger(), is(1));

        // assertArrayEquals(field2Data, response.get("Field2").getBytes());
        assertThat(response.get("Field2").getBytes(), is(field2Data));

        // Now, do it in reverse

        // mocking field holders
        FieldHolder holder1 = new FieldHolder(field1);
        holder1.setInteger(1);
        FieldHolder holder2 = new FieldHolder(field2);
        holder2.setStruct(field2Data);

        // performing the test to check if we can serialize holders, the initial data for the previous test
        // should match to the result
        byte[] serialized = charParser.serialize(Arrays.asList(holder1, holder2));
        // assertArrayEquals(data.toByteArray(), serialized);
        assertThat(data.toByteArray(), is(serialized));
    }

    private void assertFieldsExist(Object value, String... fieldNames) {
        Map<String, FieldHolder> values = charParser.parse(characteristic, new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0});
        // assertEquals(fieldNames.length, values.size());
        assertThat(values, aMapWithSize(fieldNames.length));

        // assertTrue(values.keySet().containsAll(Arrays.asList(fieldNames)));
        assertThat(values.keySet(), containsInAnyOrder(fieldNames));

        for (FieldHolder fieldHolder : values.values()) {
            assertEquals(value, fieldHolder.getRawValue());
        }
    }

    private void assertParseFormat(Object expected, String format, byte[] bytes) throws CharacteristicFormatException {
        assertParseFormat(expected, format, bytes, null);
    }

    private void assertParseFormat(Object expected, String format, byte[] bytes, Integer exponent)
            throws CharacteristicFormatException {
        List<Field> fields = new ArrayList<>();
        String name = format + "Field";
        Field field = MockUtils.mockFieldFormat(name, format);
        lenient().when(field.getDecimalExponent()).thenReturn(exponent);
        fields.add(field);
        lenient().when(reader.getFields(characteristic)).thenReturn(fields);
        //when(characteristic.getValue().getFlags()).thenReturn(null);
        lenient().when(characteristic.getValue().getFields()).thenReturn(fields);
        lenient().when(characteristic.isValidForRead()).thenReturn(true);
        Map<String, FieldHolder> values = charParser.parse(characteristic, bytes);
        // assertEquals(1, values.size());
        assertThat(values, aMapWithSize(1));

        // assertTrue(values.containsKey(name));
        assertThat(values, hasKey(name));

        // assertEquals(expected, values.get(name).getRawValue());
        assertThat(values.get(name).getRawValue(), is(expected));
    }

    private void assertParseAndSerializeStringFields(String format, String encoding) throws Exception {
        // Testing that string fields that go after another field can be parsed correctly

        // mocking test data
        // mocking number formatter to parse/serialize integers returning 5 bits set to 1
        int dummyNumber = 0b11111;
        when(twosComplementNumberFormatter.deserializeInteger(ArgumentMatchers.<BitSet>any(), anyInt(), anyBoolean()))
                .thenReturn(dummyNumber);
        BitSet oneBit = new BitSet();
        oneBit.set(0, 5);
        when(twosComplementNumberFormatter.serialize(any(Integer.class), anyInt(), anyBoolean())).thenReturn(oneBit);

        // mocking fields, first field is a dummy field with a length of 1 bit, next one is our target field
        List<Field> fields = new ArrayList<>();
        Field field1 = MockUtils.mockFieldFormat("Field1", "uint5");
        fields.add(field1);
        Field field2 = MockUtils.mockFieldFormat("Field2", format);
        fields.add(field2);
        lenient().when(reader.getFields(characteristic)).thenReturn(fields);
        lenient().when(characteristic.getValue().getFields()).thenReturn(fields);
        lenient().when(characteristic.isValidForRead()).thenReturn(true);

        // data that we are testing
        String field2Text = "Test!";
        byte[] field2Data = field2Text.getBytes(encoding);
        BitSet data = new BitSet();
        data.set(0, 5);
        charParser.concat(data, BitSet.valueOf(field2Data), 5, field2Data.length * 8);

        // performing the test to check if we can parse data
        LinkedHashMap<String, FieldHolder> response = charParser.parse(characteristic, data.toByteArray());
        // assertEquals(2, response.size());
        assertThat(response, aMapWithSize(2));

        // assertEquals(0b11111, (int) response.get("Field1").getInteger());
        assertThat(response.get("Field1").getInteger(), is(0b11111));

        // assertEquals(field2Text, response.get("Field2").getString());
        assertThat(response.get("Field2").getString(), is(field2Text));

        // Now, do it in reverse

        // mocking field holders
        FieldHolder holder1 = new FieldHolder(field1);
        holder1.setInteger(0b11111);
        FieldHolder holder2 = new FieldHolder(field2);
        holder2.setString(field2Text);

        // performing the test to check if we can serialize holders, the initial data for the previous test
        // should match to the result
        byte[] serialized = charParser.serialize(Arrays.asList(holder1, holder2));
        // assertArrayEquals(data.toByteArray(), serialized);
        assertThat(data.toByteArray(), is(serialized));
    }

}